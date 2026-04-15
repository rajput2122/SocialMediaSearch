# Testing Guide

This document covers the test strategy, coverage breakdown, how to run tests, and the design decisions that keep search latency low.

---

## Test Strategy

The project uses a layered testing approach — each layer is tested in isolation with the right tool for the job.

| Layer | Test type | Tool |
|---|---|---|
| Controller | Slice test (`@WebMvcTest`) | MockMvc + Mockito |
| Service | Unit test | Mockito |
| Exception handler | Slice test (`@WebMvcTest`) | MockMvc |
| Common / DTO | Unit test | JUnit 5 + AssertJ |
| Full application context | Integration smoke test | `@SpringBootTest` |
| E2E (live app) | Shell script | curl + Python JSON parsing |

### Why `@WebMvcTest` over `@SpringBootTest` for controller tests

`@SpringBootTest` boots the full application context including JPA, Elasticsearch, and DataInitializer. For controller-layer tests we only need the web layer. `@WebMvcTest` starts only the MVC slice (DispatcherServlet, security filters, exception handlers) and mocks the service. This makes the test suite faster and keeps each test focused on its own boundary.

### Why Mockito for service tests

`SearchService` delegates all I/O to `ElasticsearchOperations`. Tests mock that dependency and verify the mapping logic (document → `SearchResult`) and the guard conditions (null query, blank query, null type). No network call, no container — sub-millisecond execution per test.

`IndexingService` similarly delegates to five ES repository interfaces. Tests capture the document passed to each `save()` call with `ArgumentCaptor` and assert every field mapping, including the null-safety branches (`location == null`, `tags == null`, `user == null`).

---

## Test Classes

### `SearchControllerTest`
**Type:** `@WebMvcTest` slice  
**Scope:** HTTP layer — routing, parameter binding, security, response envelope

| Test | What it verifies |
|---|---|
| `searchShouldReturnWrappedPageResponse` | 200 OK, `data.content` populated, `meta.timestamp` present, no `error` field |
| `searchShouldReturnBadRequestForBlankQuery` | `@NotBlank` constraint fires → 400, `error.code = VALIDATION_ERROR`, field name in `fieldErrors` |
| `searchShouldReturnBadRequestForInvalidType` | Unknown enum value → 400, `error.code = BAD_REQUEST` |
| `searchShouldRequireAuthentication` | Unauthenticated request → 401 |

---

### `SearchServiceTest`
**Type:** Unit  
**Scope:** Search routing, ES query construction, document-to-DTO mapping, guard conditions

| Test | What it verifies |
|---|---|
| `searchReturnsEmptyPageWhenQueryIsNull` | Null query short-circuits before ES call |
| `searchReturnsEmptyPageWhenQueryIsBlank` | Blank/whitespace query short-circuits before ES call |
| `searchReturnsEmptyPageWhenTypeIsNull` | Null type short-circuits before ES call |
| `searchUserMapsDocumentToResult` | `UserDocument` → `SearchResult` (id, type, primaryText, secondaryText, locationName) |
| `searchUserReturnsEmptyPageWhenNoHits` | Zero ES hits → empty page, totalElements = 0 |
| `searchPostMapsDocumentToResult` | `PostDocument` → `SearchResult` (caption, tags, locationName, userId) |
| `searchPageMapsDocumentToResult` | `PageDocument` → `SearchResult` (pageName, bio) |
| `searchTagMapsDocumentToResult` | `TagDocument` → `SearchResult` (tagName) |
| `searchLocationMapsDocumentToResult` | `LocationDocument` → `SearchResult` (displayName) |

---

### `IndexingServiceTest`
**Type:** Unit  
**Scope:** Entity-to-document mapping, null safety, exception wrapping

| Test | What it verifies |
|---|---|
| `indexLocationSavesCorrectDocument` | `Location` → `LocationDocument` (id as string, displayName) |
| `indexLocationThrowsIndexingExceptionOnFailure` | ES save failure wrapped in `ElasticsearchIndexingException` |
| `indexTagSavesCorrectDocument` | `Tag` → `TagDocument` |
| `indexTagThrowsIndexingExceptionOnFailure` | ES save failure wrapped |
| `indexUserSavesCorrectDocument` | `User` → `UserDocument` including locationName |
| `indexUserHandlesNullLocation` | User without location → `locationName = null` (no NPE) |
| `indexUserThrowsIndexingExceptionOnFailure` | ES save failure wrapped |
| `indexPostSavesCorrectDocument` | `Post` → `PostDocument` (caption, tag names, locationName, userId) |
| `indexPostHandlesNullTagsAndNullLocationAndNullUser` | Post with no tags/location/user → empty list, null fields (no NPE) |
| `indexPostThrowsIndexingExceptionOnFailure` | ES save failure wrapped |
| `indexPageSavesCorrectDocument` | `Page` → `PageDocument` (pageName, bio, locationName) |
| `indexPageHandlesNullLocation` | Page without location → `locationName = null` |
| `indexPageThrowsIndexingExceptionOnFailure` | ES save failure wrapped |

---

### `CustomUserDetailsServiceTest`
**Type:** Unit  
**Scope:** Spring Security `UserDetailsService` contract

| Test | What it verifies |
|---|---|
| `loadUserByUsernameReturnsUserDetails` | Returns `UserDetails` with correct username, hashed password, and `ROLE_USER` authority |
| `loadUserByUsernameThrowsWhenUserNotFound` | Unknown username → `UsernameNotFoundException` (message includes the username) |

---

### `GlobalExceptionHandlerTest`
**Type:** `@WebMvcTest` slice  
**Scope:** Exception-to-HTTP-response mapping for every handler in `GlobalExceptionHandler`

| Test | HTTP status | `error.code` |
|---|---|---|
| `handlesMethodArgumentTypeMismatchWithBadRequest` | 400 | `BAD_REQUEST` |
| `handlesConstraintViolationForBlankQuery` | 400 | `VALIDATION_ERROR` |
| `handlesElasticsearchIndexingException` | 500 | `INDEXING_ERROR` |
| `handlesGenericException` | 500 | `INTERNAL_ERROR` |
| `returnsUnauthorizedWithJsonBodyForMissingCredentials` | 401 | `UNAUTHORIZED` |

---

### `ApiResponseTest`
**Type:** Unit  
**Scope:** `ApiResponse` factory methods

| Test | What it verifies |
|---|---|
| `successWrapsDataAndSetsTimestamp` | `data` set, `error` null, `meta.timestamp` not null |
| `errorSetsCodeAndMessageAndNullData` | `data` null, `error.code`, `error.message`, `fieldErrors` null |
| `validationErrorSetsCodeAndFieldErrors` | `error.code = VALIDATION_ERROR`, `fieldErrors` list populated |

---

### `SocialMediaSearchApplicationTests`
**Type:** `@SpringBootTest` (full context)  
**Scope:** Application context loads without errors (wires JPA, Elasticsearch config, Security, DataInitializer)

---

## Coverage Report

Tool: **JaCoCo 0.8.11**  
Metric: **Instruction coverage**  
Gate: **80% minimum** — `./mvnw verify` fails if coverage drops below this threshold.

### Overall

| Metric | Result |
|---|---|
| Instruction coverage | **82%** (1,098 / 1,328 instructions) |
| Tests | **37 tests, 0 failures** |
| Build gate | **PASS** |

### By package

| Package | Coverage | Notes |
|---|---|---|
| `service` | **100%** | SearchService, IndexingService, CustomUserDetailsService |
| `common` | **100%** | ApiResponse and inner classes |
| `dto` | **100%** | SearchResult, SearchType |
| `controller` | **100%** | SearchController |
| `config` | **99%** | SecurityConfig, ElasticsearchConfig (DataInitializer excluded from gate) |
| `exception` | **51%** | GlobalExceptionHandler fully covered; `ResourceNotFoundException` constructors not exercised (no endpoint throws it yet) |
| `dto.response` | **0%** (excluded) | Response DTOs are pure Lombok data classes — no logic to test |

### JaCoCo exclusions

The following are excluded from the coverage gate (not from the report):

| Excluded class | Reason |
|---|---|
| `SocialMediaSearchApplication` | Spring Boot entry point — just `main()` |
| `DataInitializer` | Dev seed data runner — covered by the integration smoke test indirectly |
| `ElasticsearchConfig` | Pure configuration annotations, no logic |
| `model/*` | JPA entities — Lombok-generated getters/setters/builders, no business logic |
| `document/*` | Elasticsearch documents — same as above |
| `dto/response/*` | Response DTOs — pure data containers |
| `repository/**` | Spring Data interfaces — framework-generated implementations |

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run all tests + generate coverage report + enforce gate
./mvnw verify

# View HTML coverage report (open in browser after verify)
open target/site/jacoco/index.html

# Run a single test class
./mvnw test -Dtest=SearchServiceTest

# Run a single test method
./mvnw test -Dtest=SearchServiceTest#searchUserMapsDocumentToResult
```

---

## E2E Test Script

`scripts/e2e-test.sh` is a self-contained bash script that hits the running application with curl and validates every response. It requires the app and Elasticsearch to already be running.

```bash
docker compose up -d        # start Elasticsearch
./mvnw spring-boot:run &    # start the app (seeds data on startup)
./scripts/e2e-test.sh       # run 22 assertions
```

### Scenarios covered

| # | Scenario | Expected |
|---|---|---|
| 1 | Search USER by name/username | 200, `totalElements > 0` |
| 2 | Search POST by caption | 200, `totalElements > 0` |
| 3 | Search PAGE by name | 200, `totalElements > 0` |
| 4 | Search TAG by name | 200, `totalElements > 0` |
| 5 | Search LOCATION by display name | 200, `totalElements > 0` |
| 6 | No credentials | 401, `error.code = UNAUTHORIZED`, `message = "Authentication required"` |
| 7 | Wrong password | 401, `error.code = UNAUTHORIZED`, `message = "password incorrect"` |
| 8 | Blank query (`q=%20`) | 400, `error.code = VALIDATION_ERROR` |
| 9 | Invalid type (`type=INVALID`) | 400, `error.code = BAD_REQUEST` |
| 10 | Custom page size (`size=1`) | 200, `data.size = 1` |

---

## Low Search Latency

Meeting the <200ms p99 latency target is achieved by a combination of architectural choices and Elasticsearch query design.

### Architecture: Elasticsearch as the dedicated search store

All search queries go **directly to Elasticsearch** — the relational database (H2 / production SQL) is never queried on the search path. Elasticsearch is optimised for full-text search: inverted indexes, BKD trees for numerics, and segment-level caching are built in. A SQL `LIKE '%spring%'` scan on a 5M-row table would take seconds; an ES `multi_match` on an indexed field takes single-digit milliseconds.

### Dual-write keeps the index fresh

Writes go to JPA first (source of truth) and immediately to Elasticsearch via `IndexingService`. There is no async replication lag — the ES index is updated in the same request. Search latency is not affected by stale index data.

### Query design

| Entity | Query type | Why |
|---|---|---|
| User | `multi_match` on `name`, `username`, `bio` with `fuzziness("AUTO")` | Tolerates typos in name searches |
| Post | `bool` → `multi_match` on `caption` (text) + `term` on `tags` (keyword) | `fuzziness` only on text fields; keyword fields require exact `term` match — mixing them in a single `multi_match` would silently ignore fuzziness on keywords |
| Page | `multi_match` on `pageName`, `bio` with `fuzziness("AUTO")` | Same pattern as User |
| Tag | `multi_match` on `tagName` with `fuzziness("AUTO")` | Catches partial tag matches |
| Location | `multi_match` on `displayName` with `fuzziness("AUTO")` | Catches spelling variants |

**`fuzziness("AUTO")`** uses Levenshtein distance scaled to term length: edit distance 0 for 1–2 chars, 1 for 3–5 chars, 2 for 6+ chars. This keeps recall high without the cost of a full fuzzy scan on very short tokens.

**`@MultiField`** on display fields (`name`, `tagName`, `pageName`, `displayName`) indexes each value as both `text` (analysed, for full-text) and `keyword` (not analysed, for exact/sort). The `multi_match` query targets the `text` sub-field so analysis (tokenisation, lowercasing, stemming) applies.

### Pagination

Every search response is paginated using Elasticsearch's `from`/`size` mechanism via Spring Data's `Pageable`. Only the requested page of documents is transferred over the wire — not the entire result set. Default page size is 10.

At 5M searches/day (~58 RPS average, with realistic spikes to 200–300 RPS) a single Elasticsearch node handles this comfortably given that each query touches only the search index, not JPA. For production, horizontal scaling (replica shards) distributes query load linearly.

### What to measure in production

To verify the <200ms SLA in a real deployment, instrument these two points:

1. **ES query time** — available in the ES slow query log (`index.search.slowlog.threshold.query.warn: 200ms`) and in Spring's `SearchHits` response metadata.
2. **Total request time** — measure `Content-Download` in the browser / APM tool from the moment the HTTP request hits the load balancer to the moment the full response is received.

The gap between (1) and (2) is Spring serialisation overhead, which for a typical 10-result page of `SearchResult` objects is under 5ms.
