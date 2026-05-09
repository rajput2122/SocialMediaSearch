# ADR 0003 — Sandcastle loop is label-triggered via GitHub Actions

**Status**: Accepted

## Context

The loop needs to run without requiring any team member to have API keys locally or remember to trigger it manually.

## Decision

Adding the label `sandcastle-ready` to a GitHub issue triggers a GitHub Actions workflow that runs the sandcastle loop. The loop picks up all issues with that label, implements them, and opens PRs. The label is removed after the run to prevent re-triggering.

## Consequences

- No team member needs `ANTHROPIC_API_KEY` or `GH_TOKEN` locally.
- Secrets live in GitHub Actions repository secrets only.
- Humans explicitly opt issues into agent implementation — nothing runs by surprise.
- The Actions runner must have Docker available (use `ubuntu-latest` with Docker pre-installed).
