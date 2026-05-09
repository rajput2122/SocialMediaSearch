# ADR 0002 — Agents open PRs, never self-merge

**Status**: Accepted

## Context

The sandcastle loop produces commits on feature branches. It could either merge those branches directly to `master` or open a pull request for human review.

## Decision

Agents open PRs. They never merge to `master` themselves. Every agent branch requires at least one human approval before it can land.

## Consequences

- Humans stay in the loop on every change — agents assist, they do not replace review.
- The merger prompt must call `gh pr create` instead of `git merge`.
- CI runs on the PR branch before any human spends time reviewing.
- Slightly slower than self-merge, but safe enough for a team codebase.
- Agents may comment on their own PR with a summary of what was done and why.
