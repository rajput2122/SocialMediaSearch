## Controllers

Annotate with `@RestController` and `@RequestMapping("/api/v1/<resource>")`. Use plural nouns: `/users`, `/posts`, not `/getUser`. Inject services via constructor injection — never `@Autowired` on a field. Return `ResponseEntity<ApiResponse<T>>` for every endpoint. No business logic in controllers; delegate entirely to the service layer.

---

## Services

Annotate with `@Service`. All business logic and validation rules live here — never in controllers or repositories. Throw typed custom exceptions; never return `null` or raw strings for errors. Use `@Transactional` on any method that writes to multiple tables.

---

## Repositories

Extend `JpaRepository<Entity, Long>`. Use Spring Data derived method names for simple queries. Use `@Query` with JPQL for complex queries — never raw SQL unless unavoidable.

---

## Entities (Models)

Annotate with `@Entity` and `@Table(name = "...")`. Always include audit fields via `@EntityListeners(AuditingEntityListener.class)` (`@CreatedDate`, `@LastModifiedDate`). Use `@Enumerated(EnumType.STRING)` — never store enums as integers. Apply `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` from Lombok. Add `@Version` for optimistic locking on any entity subject to concurrent updates.

---

## DTOs

Separate Request and Response DTOs — never expose a JPA entity directly from a controller. Apply `@Valid` plus Bean Validation annotations on request DTOs. Add a static `from(Entity e)` factory method on every response DTO.

---

## Response Envelope

Every endpoint returns `ResponseEntity<ApiResponse<T>>`. Use `ApiResponse.success(data)` for happy-path responses and `ApiResponse.error(code, message)` for error responses. Never construct raw maps or ad-hoc JSON bodies.

---

## Exception Handling

One `GlobalExceptionHandler` annotated with `@RestControllerAdvice` — never scatter try/catch across controllers. Map each custom exception type to the correct HTTP status (see table below). Let unexpected exceptions bubble to the generic `Exception` handler; never swallow them silently.

Custom exceptions to use:

- `ResourceNotFoundException` → 404
- `ConflictException` → 409
- `BadRequestException` → 400

---

## HTTP Status Codes

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

## Dual-Write Pattern

Every create/update/delete writes to **both** H2 (via JPA) and Elasticsearch (via Spring Data ES). H2 is the source of truth; ES is the search index. All search queries go directly to Elasticsearch — never to H2.

---

## Filters and Data Shape

Filters must stay in sync with the shape of the data they filter. When a new field is added to an entity that affects what something "is" (status, category, state), every filter, count, and badge surfacing that concept must be updated. Drift between filters and data shape produces silently-wrong results.

---

## Optional Parameters

Optional parameters passed to functions must be scrutinised carefully — they are a large source of bugs by omission. Prioritise correctness over backwards compatibility.

---

## Testing

### Core Principle

Tests verify behavior through public interfaces, not implementation details. Code can change entirely; tests should not break unless behavior changed.

### Good Tests

Integration-style tests that exercise real code paths through public APIs. They describe *what* the system does, not *how*.

```java
// GOOD: Tests observable behavior through the public interface
@Test
void createUser_makesUserRetrievable() {
    UserResponse created = userService.create(new CreateUserRequest("Alice"));
    UserResponse retrieved = userService.findById(created.getId());
    assertThat(retrieved.getUsername()).isEqualTo("Alice");
}
```

- Test behavior callers care about
- Use the public API only
- Survive internal refactors
- One logical assertion per test

### Bad Tests

```java
// BAD: Mocks internal collaborator, tests HOW not WHAT
@Test
void checkout_callsPaymentService() {
    verify(mockPaymentService).process(cart.getTotal());
}

// BAD: Bypasses the interface to verify via the database directly
@Test
void createUser_savesToDatabase() {
    userService.create(new CreateUserRequest("Alice"));
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
}
```

Red flags:

- Mocking internal collaborators (your own classes/services)
- Testing private methods
- Asserting on call counts or order of internal calls
- Test breaks when refactoring without behavior change
- Test name describes HOW not WHAT

### Mocking

Mock at **system boundaries** only:

- External APIs
- Time / randomness
- Databases when a real instance is not practical (use Testcontainers for ES integration tests)

**Never mock your own services or internal collaborators.** If something is hard to test without mocking internals, redesign the interface.

### TDD Workflow: Vertical Slices

Do NOT write all tests first, then all implementation. Correct approach — one test, one implementation, repeat:

```
RED→GREEN: test1→impl1
RED→GREEN: test2→impl2
RED→GREEN: test3→impl3
```

Never refactor while RED — get to GREEN first.

---

## Interface Design

Prefer deep modules: small interface, deep implementation — a few methods with simple parameters hiding complex logic. Avoid shallow modules with many pass-through methods. Ask: can I reduce the number of methods? Can I simplify the parameters? Can I hide more complexity inside?

Accept dependencies via constructor injection; never construct them internally. Return results rather than producing side effects where possible.

---

## Things to Avoid

- Never put business logic in a controller
- Never return a JPA entity directly from a controller — always map to a DTO
- Never use `@Autowired` on fields — use constructor injection
- Never swallow exceptions silently — always let them bubble to the global handler
- Never use `FLOAT` or `DOUBLE` for monetary values — use `BigDecimal`
- Never store enums as integers — use `@Enumerated(EnumType.STRING)`
