# ADR 0001 — Immutable names for Tag and User

**Status**: Accepted

## Context

`Tag.tagName` is embedded in `PostDocument.tags` (a `List<String>` of tag names) in Elasticsearch. `User.username` is the auth lookup key in `UserCredentialRepository.findByUser_Username`. Both fields are referenced across multiple Elasticsearch indices and/or the security layer.

## Decision

`Tag.tagName` and `User.username` are immutable after creation. No `PATCH` endpoint exposes these fields.

## Consequences

- Renaming either field would require finding and re-indexing every Document that embeds it — an O(n) fan-out write that breaks the simple dual-write pattern.
- `username` doubles as the Spring Security principal name; renaming it mid-session would invalidate active HTTP Basic credentials.
- Callers who need a different name must delete and recreate the entity.
- All other fields on these entities remain mutable via `PATCH`.
