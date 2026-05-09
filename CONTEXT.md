# CONTEXT.md

Domain language for the Social Media Search project. Implementation details belong in code; decisions belong in `docs/adr/`.

---

## Core Concepts

**Entity**
A domain object persisted in H2 via JPA. The five entities are: `User`, `Post`, `Page`, `Tag`, `Location`. H2 is the source of truth for all entity state.

**Document**
The Elasticsearch representation of an Entity, used exclusively for search queries. Each Entity has a corresponding Document class (e.g., `UserDocument`, `PostDocument`). Documents are derived from Entities — never the other way around.

**Dual-Write**
Every create, update, or delete operation writes to both H2 (via JPA) and Elasticsearch (via `IndexingService`). The JPA transaction commits first; the ES write happens outside the transaction boundary. If the ES write fails, H2 remains committed and `ElasticsearchIndexingException` (HTTP 500) is thrown. ES can be re-indexed from H2 at any time.

**Soft Delete**
Entities are never removed from the database. Deletion sets a `deletedAt` timestamp. `@SQLRestriction("deleted_at IS NULL")` on every entity class ensures deleted records are invisible to all JPA queries. Corresponding Documents are removed from Elasticsearch immediately on soft-delete.

**Immutable Name**
A field that cannot be changed after creation. `Tag.tagName` and `User.username` are immutable. Rationale: both are embedded in Elasticsearch Documents across multiple indices — renaming would require fan-out re-indexing across potentially many Documents. See `docs/adr/0001-immutable-names.md`.

**Block Delete**
A delete operation that is rejected with HTTP 409 if the target Entity is referenced by other active Entities. `Location` uses block delete: deletion is rejected if any active `User`, `Page`, or `Post` references it. `Tag` uses block delete: deletion is rejected if any active `Post` references it.

**Cascade Soft-Delete**
When a `User` is soft-deleted, all of their `Post` records are soft-deleted in the same service call. Post Documents are bulk-removed from Elasticsearch.

**SearchResult**
A unified read projection returned by the search API, independent of entity type. Fields: `id`, `type`, `primaryText`, `secondaryText`, `locationName`, `tags`. Never expose JPA entities or Elasticsearch Documents directly to API callers.

---

## Entity Relationships

```
Location ←── User ──── Post ──── Tag
         ←── Page       └───── Location
```

- `User` → `Location` (optional FK)
- `Post` → `User` (required FK), `Location` (optional FK), `Tag` (many-to-many via `post_tags`)
- `Page` → `Location` (optional FK)
- `Tag`, `Location`: no FK dependencies

---

## Write Rules (by entity)

| Entity   | Create | Update (PATCH)                        | Delete                                   |
|----------|--------|---------------------------------------|------------------------------------------|
| Tag      | ✓      | ✗ (immutable name)                    | Block if referenced by any Post          |
| Location | ✓      | displayName                           | Block if referenced by User, Page, Post  |
| User     | ✓      | email, phone, name, bio, locationId   | Cascade soft-delete all Posts            |
| Page     | ✓      | pageName, bio, locationId             | Soft delete                              |
| Post     | ✓      | caption, locationId, tagIds (replace) | Soft delete                              |

---

## Sandcastle Loop

**Sandcastle Loop**
The agentic development loop defined in `.sandcastle/`. Triggered by applying the `sandcastle-ready` label to a GitHub issue. Runs four agents in sequence: Planner → Implementer → Reviewer → Merger.

**Planner**
Reads all `sandcastle-ready` issues, builds a dependency graph, and selects unblocked issues to work in parallel. Outputs a `<plan>` JSON. Runs on Haiku (read-only reasoning task).

**Implementer**
Implements a single issue on an isolated branch using TDD (Red→Green→Refactor). Runs on Sonnet (code quality matters). Capped at 20 iterations.

**Reviewer**
Reads the diff on the implemented branch, stress-tests edge cases, and applies CODING_STANDARDS. Runs on Haiku. Capped at 1 iteration.

**Merger**
Opens a pull request per completed branch following ADR 0004 format. Never merges directly — humans approve. Removes the `sandcastle-ready` label after opening the PR.

**Sandcastle-Ready**
The GitHub label that opts an issue into the loop. Engineers apply this label when an issue is scoped, has acceptance criteria, and is safe to hand to an agent. Removing or not applying the label keeps an issue out of the loop.

---

## Audit Fields

All entities carry: `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`), `deletedAt` (nullable, set on soft-delete), `version` (`@Version` for optimistic locking). `UserCredential` is excluded — it shares the `User` lifecycle.
