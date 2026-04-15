# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.
It is an interview preparation project — the goal is to practice building
well-structured Spring Boot APIs quickly and cleanly.

---

## Commands

```bash
# Build
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClaudeDevSetupJavaSpringApplicationTests

# Run a single test method
./mvnw test -Dtest=ClaudeDevSetupJavaSpringApplicationTests#contextLoads

# Package without running tests
./mvnw package -DskipTests
```

---

## Stack

- **Java 17**, **Spring Boot 3.x**
- **Spring Data JPA** + **H2** (in-memory) — primary data store for dev
- **Elasticsearch 8.13.0** via Docker Compose — all search queries go here
- **Spring Data Elasticsearch** — ES client library
- **Spring Security** — HTTP Basic Auth (dev scope)
- **Lombok** for boilerplate reduction (annotation processor configured)
- **Spring Validation** (`spring-boot-starter-validation`) for request validation
- **Testcontainers** — integration tests spin up a real ES container
- Base package: `com.practice.socialmediasearch`

---

## Architecture

### Layers
```
src/main/java/com/practice/socialmediasearch/
├── controller/       # REST controllers — HTTP in, HTTP out, nothing else
├── service/          # Business logic — all decisions live here
├── repository/       # Spring Data JPA interfaces (H2) + ES repositories
├── model/            # JPA entities
├── document/         # Elasticsearch document classes (@Document)
├── dto/              # Request and response objects (never expose entities directly)
├── exception/        # Custom exceptions + global error handler
└── config/           # Spring Security, ES client config
```

### Write Path (Dual-Write)
Every create/update/delete goes to **both** H2 (via JPA) and Elasticsearch (via Spring Data ES).
H2 is the source of truth; ES is the search index.

### Search Path
All search queries go **directly to Elasticsearch** — never to H2.
Search is paginated using ES `Pageable` support.

### Entity → Document mapping
Each searchable entity (User, Post, Page, Tag, Location) has:
- A JPA `@Entity` in `model/` — owns the DB row
- An ES `@Document` in `document/` — owns the search index

### Local Dev
```bash
docker compose up -d   # starts Elasticsearch on port 9200
./mvnw spring-boot:run # app connects to ES + H2 auto-configured
```

---

## Coding Conventions

### Controllers
- Annotate with `@RestController` and `@RequestMapping("/api/v1/<resource>")`
- Use plural nouns for resource paths: `/users`, `/subscriptions`, not `/getUser`
- Inject service via constructor injection (never `@Autowired` on field)
- Return `ResponseEntity<ApiResponse<T>>` for all endpoints
- No business logic in controllers — delegate everything to the service layer

```java
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse data = subscriptionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getById(@PathVariable Long id) {
        SubscriptionResponse data = subscriptionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
```

### Services
- Annotate with `@Service`
- Contain all business logic and validation rules
- Throw typed custom exceptions — never return null or raw strings for errors
- Use `@Transactional` on methods that write to multiple tables

```java
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    @Transactional
    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.getPlanId()));

        boolean alreadyActive = subscriptionRepository
                .existsByUserIdAndStatus(request.getUserId(), SubscriptionStatus.ACTIVE);
        if (alreadyActive) {
            throw new ConflictException("User already has an active subscription");
        }

        Subscription subscription = Subscription.builder()
                .userId(request.getUserId())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }
}
```

### Repositories
- Extend `JpaRepository<Entity, Long>`
- Use Spring Data method names for simple queries
- Use `@Query` with JPQL for complex queries

```java
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd < :now AND s.status = 'ACTIVE'")
    List<Subscription> findExpiredActive(@Param("now") LocalDateTime now);
}
```

### Entities (Models)
- Annotate with `@Entity`, `@Table(name = "...")`
- Always include audit fields via `@EntityListeners(AuditingEntityListener.class)`
- Use `@Enumerated(EnumType.STRING)` — never store enums as integers
- Use `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` from Lombok

```java
@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime cancelledAt;

    @Version
    private Integer version; // optimistic locking

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### DTOs
- Separate Request and Response DTOs — never expose the entity directly
- Use `@Valid` + Bean Validation annotations on request DTOs
- Add a static `from(Entity e)` factory method on response DTOs

```java
// Request
@Data
public class CreateSubscriptionRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "planId is required")
    private Long planId;
}

// Response
@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private String planName;
    private String status;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime createdAt;

    public static SubscriptionResponse from(Subscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .planName(s.getPlan().getName())
                .status(s.getStatus().name())
                .currentPeriodEnd(s.getCurrentPeriodEnd())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
```

---

## Standard Response Envelope

All endpoints return this wrapper. Create this once and reuse everywhere.

```java
@Data
@Builder
public class ApiResponse<T> {
    private T data;
    private ErrorBody error;
    private MetaBody meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(MetaBody.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .error(ErrorBody.of(code, message))
                .meta(MetaBody.now())
                .build();
    }

    @Data @Builder
    public static class ErrorBody {
        private String code;
        private String message;
        private List<FieldError> details;

        public static ErrorBody of(String code, String message) {
            return ErrorBody.builder().code(code).message(message).build();
        }
    }

    @Data @Builder
    public static class MetaBody {
        private Instant timestamp;
        public static MetaBody now() {
            return MetaBody.builder().timestamp(Instant.now()).build();
        }
    }
}
```

---

## Exception Handling

Create a single `GlobalExceptionHandler` — never scatter try/catch across controllers.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.ErrorBody.FieldError> details = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(e -> new ApiResponse.ErrorBody.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .error(ApiResponse.ErrorBody.builder()
                        .code("INVALID_INPUT")
                        .message("One or more fields are invalid")
                        .details(details)
                        .build())
                .meta(ApiResponse.MetaBody.now())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

Custom exception classes to create:

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
```

---

## HTTP Status Code Reference

| Scenario                        | Status |
|---------------------------------|--------|
| Successful GET / PATCH / PUT    | 200    |
| Successful POST (created)       | 201    |
| Successful DELETE (no body)     | 204    |
| Validation failed               | 400    |
| Missing / invalid auth token    | 401    |
| Authenticated but not permitted | 403    |
| Resource not found              | 404    |
| Duplicate / state conflict      | 409    |
| Business rule violation         | 422    |
| Unexpected server error         | 500    |

---

## Interview-Specific Notes

### Do these immediately when given a new problem
1. Identify entities and relationships before writing any code
2. Define the DB schema (entity classes) first — everything else follows
3. Build one layer at a time: entity → repository → service → controller → DTO
4. Add the `GlobalExceptionHandler` early — it saves debugging time

### Patterns to use proactively (show these without being asked)
- `@Version` on entities for optimistic locking on concurrent updates
- `@Transactional` on service methods that touch multiple tables
- Soft deletes: `deletedAt TIMESTAMP NULL` instead of hard deletes
- Idempotency: check for existing records before creating to prevent duplicates
- Pagination: use `Pageable` parameter + `Page<T>` return type on list endpoints

### Things to avoid
- Never put business logic in a controller
- Never return a JPA entity directly from a controller — always map to a DTO
- Never use `@Autowired` on fields — use constructor injection
- Never swallow exceptions silently — always let them bubble to the global handler
- Never use `FLOAT` or `DOUBLE` for monetary values — use `BigDecimal`
