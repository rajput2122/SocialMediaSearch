# ADR 0004 — Agent PRs follow a structured format

**Status**: Accepted

## Context

Agents open PRs for human review. Without a standard format, reviewers don't know what the agent did, why, or what to focus on.

## Decision

Every agent-opened PR must include:
- Title: `[SMS] #<issue-number>: <issue-title>`
- Body sections: Summary, Key decisions, Files changed, Test coverage, Follow-ups not done
- Label: `sandcastle` (auto-applied)
- Linked issue (closes #N)

## Consequences

- Reviewers immediately know this is an agent PR and what to look for.
- The merge-prompt.md must produce this format via `gh pr create`.
- Humans can filter agent PRs by the `sandcastle` label.
