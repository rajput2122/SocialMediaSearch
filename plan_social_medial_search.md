# Implementation Plan: Social Media Search Feature

## Context
Building a paginated search API for a social media app that allows searching across 5 entity types: User, Post, Page, Tag, Location. Core requirements: <200ms latency, high availability, 5M searches/day. Solution uses a dual-write pattern — H2/JPA as source of truth, Elasticsearch as the search index. All search queries go to Elasticsearch only.

---

## Implementation Order

### Phase 0 — Infrastructure
1. `pom.xml` — add all missing dependencies
2. `docker-compose.yml` — Elasticsearch 8.13.0 single-node
3. `application.properties` — H2, JPA, ES, Security config

### Phase 1 — Foundation (no deps, build first)
4. `common/ApiResponse.java`
5. `exception/ResourceNotFoundException.java`
6. `exception/ElasticsearchIndexingException.java`
7. `exception/GlobalExceptionHandler.java`

### Phase 2 — JPA Entities (FK dependency order)
8. `model/Location.java` — no FK deps
9. `model/Tag.java` — no FK deps
10. `model/User.java` — FK → Location
11. `model/UserCredential.java` — shared PK with User via `@MapsId`
12. `model/Post.java` — FK → User, Location; `@ManyToMany` → Tag
13. `model/Page.java` — FK → Location

### Phase 3 — Elasticsearch Documents
14. `document/LocationDocument.java`
15. `document/TagDocument.java`
16. `document/UserDocument.java`
17. `document/PostDocument.java`
18. `document/PageDocument.java`

### Phase 4 — Repositories
19. `repository/jpa/` — one per entity (LocationRepository, TagRepository, UserRepository, UserCredentialRepository, PostRepository, PageRepository)
20. `repository/es/` — one per document (intentionally empty — queries go via `ElasticsearchOperations`)

### Phase 5 — Configuration
21. `config/ElasticsearchConfig.java`
22. `config/SecurityConfig.java`

### Phase 6 — DTOs
23. `dto/SearchType.java` — enum: USER, POST, PAGE, TAG, LOCATION
24. `dto/SearchResult.java` — unified projection for all search hits
25. `dto/response/` — UserResponse, PostResponse, PageResponse, TagResponse, LocationResponse

### Phase 7 — Services
26. `service/CustomUserDetailsService.java`
27. `service/IndexingService.java`
28. `service/SearchService.java`

### Phase 8 — Controller
29. `controller/SearchController.java`

---

## Key Design Decisions

### 1. pom.xml — Dependencies to add
```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
h2 (runtime)
spring-boot-starter-data-elasticsearch
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-testcontainers (test)
org.testcontainers:elasticsearch (test)
org.testcontainers:junit-jupiter (test)
```
**Note:** pom.xml declares Spring Boot `4.0.5` — change to `3.3.5`. SB 4.x does not exist; all deps in this plan are SB 3.x.

### 2. docker-compose.yml
```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false   # critical — disables TLS/auth for local dev
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9200:9200"
```

### 3. JPA Entity notes

**User:** Use `@Table(name = "app_users")` — `"users"` is reserved in H2.

**UserCredential — `@MapsId` pattern:**
```java
@Id
private Long userId;          // NOT @GeneratedValue — value copied from User

@OneToOne(fetch = LAZY)
@MapsId
@JoinColumn(name = "user_id")
private User user;
```
`userId` is both PK and FK. Never set `@GeneratedValue` here.

**Post tags:**
```java
@ManyToMany
@JoinTable(name = "post_tags",
  joinColumns = @JoinColumn(name = "post_id"),
  inverseJoinColumns = @JoinColumn(name = "tag_id"))
private List<Tag> tags;
```

All entities: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`. No `@Version`, no audit fields (out of scope).

### 4. Elasticsearch Document field mapping
| Document | Fields | Type |
|---|---|---|
| UserDocument | name, username | `@MultiField` (text + keyword) |
| UserDocument | bio | text |
| UserDocument | locationName | keyword |
| PostDocument | caption | text |
| PostDocument | tags | keyword (List) |
| PostDocument | locationName, userId | keyword |
| PageDocument | pageName | `@MultiField` (text + keyword) |
| PageDocument | bio | text |
| TagDocument | tagName | `@MultiField` (text + keyword) |
| LocationDocument | displayName | `@MultiField` (text + keyword) |

All document `id` fields are `String` (ES IDs are strings; convert from `Long` when indexing).

### 5. ES Repositories — intentionally empty
All search queries use `ElasticsearchOperations` directly in `SearchService` for full DSL control. Repository interfaces extend `ElasticsearchRepository<Document, String>` but define no methods.

### 6. Repository package split — required to avoid scan conflicts
```
@EnableJpaRepositories(basePackages = "...repository.jpa")
@EnableElasticsearchRepositories(basePackages = "...repository.es")
```
Without this, Spring tries to create JPA repos from ES interfaces (startup failure).

### 7. Dual-write pattern — transactional boundary
JPA `@Transactional` and ES writes CANNOT share a transaction (ES doesn't participate in JPA transactions).

Correct pattern:
```
@Transactional
createUser():
  1. userRepository.save(user)           ← inside JPA transaction
  2. userCredentialRepository.save(cred) ← inside JPA transaction
  [transaction commits here]
  3. indexingService.indexUser(saved)    ← ES write, outside transaction
```
If ES write fails → H2 already committed. Throw `ElasticsearchIndexingException` (HTTP 500). H2 remains source of truth; ES can be re-indexed.

### 8. SearchService — query strategy
Use `NativeQuery` (not deprecated `NativeSearchQuery`) with `ElasticsearchOperations`:
```java
NativeQuery.builder()
  .withQuery(q -> q.multiMatch(mm -> mm
      .query(queryString)
      .fields("name", "username", "bio")
      .type(MultiMatchQuery.Type.BestFields)
      .fuzziness("AUTO")
  ))
  .withPageable(pageable)
  .build();
```
- `BestFields`: scores by the best-matching single field, not a sum (avoids weak multi-field inflation)
- `fuzziness("AUTO")`: handles typos (0 edits ≤2 chars, 1 edit 3-5 chars, 2 edits 6+ chars)
- `fuzziness` only applies to `text` fields — not `keyword`

### 9. Search API
```
GET /api/v1/search?q={query}&type={USER|POST|PAGE|TAG|LOCATION}&page=0&size=10
→ ResponseEntity<ApiResponse<Page<SearchResult>>>
```
`SearchResult` is a unified projection: `id, type, primaryText, secondaryText, locationName, tags`.

Spring auto-converts `type` param string → `SearchType` enum. Add `MethodArgumentTypeMismatchException` handler in `GlobalExceptionHandler` → HTTP 400.

### 10. Security config
```java
http
  .csrf(AbstractHttpConfigurer::disable)
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/h2-console/**").permitAll()
      .anyRequest().authenticated())
  .httpBasic(Customizer.withDefaults())
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .headers(h -> h.frameOptions(f -> f.disable()));  // for H2 console iframes
```
`CustomUserDetailsService.loadUserByUsername(username)` looks up `UserCredential` via `findByUser_Username(username)` — Spring Data traverses the FK join automatically.

Passwords stored as BCrypt hash. Never store plaintext. Encode at user creation time via `passwordEncoder.encode(raw)`.

---

## Files — Complete List

```
pom.xml
docker-compose.yml
src/main/resources/application.properties

src/main/java/com/practice/socialmediasearch/
  common/ApiResponse.java
  exception/ResourceNotFoundException.java
  exception/ElasticsearchIndexingException.java
  exception/GlobalExceptionHandler.java
  model/Location.java
  model/Tag.java
  model/User.java
  model/UserCredential.java
  model/Post.java
  model/Page.java
  document/LocationDocument.java
  document/TagDocument.java
  document/UserDocument.java
  document/PostDocument.java
  document/PageDocument.java
  repository/jpa/LocationRepository.java
  repository/jpa/TagRepository.java
  repository/jpa/UserRepository.java
  repository/jpa/UserCredentialRepository.java
  repository/jpa/PostRepository.java
  repository/jpa/PageRepository.java
  repository/es/LocationSearchRepository.java
  repository/es/TagSearchRepository.java
  repository/es/UserSearchRepository.java
  repository/es/PostSearchRepository.java
  repository/es/PageSearchRepository.java
  config/ElasticsearchConfig.java
  config/SecurityConfig.java
  dto/SearchType.java
  dto/SearchResult.java
  dto/response/UserResponse.java
  dto/response/PostResponse.java
  dto/response/PageResponse.java
  dto/response/TagResponse.java
  dto/response/LocationResponse.java
  service/CustomUserDetailsService.java
  service/IndexingService.java
  service/SearchService.java
  controller/SearchController.java

src/test/java/com/practice/socialmediasearch/
  SearchServiceIntegrationTest.java
```

---

## Verification

1. `docker compose up -d` → confirm ES healthy: `curl http://localhost:9200/_cluster/health`
2. `./mvnw spring-boot:run` → app starts without errors, H2 creates schema
3. Seed a user + credential via H2 console (`/h2-console`)
4. `curl -u username:password "http://localhost:8080/api/v1/search?q=test&type=USER&page=0&size=10"` → returns paginated results
5. `./mvnw test` → `SearchServiceIntegrationTest` spins up Testcontainers ES, indexes a document, asserts search returns it
