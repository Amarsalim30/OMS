# Production Readiness Plan

Date: 2026-03-06 (current pass)

## Goals
- Complete a repository-wide production-readiness audit with actionable severity-ranked findings.
- Remove immediately risky artifacts/issues that can be fixed safely now.
- Keep an implementation trail and verification evidence.

## Non-goals
- No broad framework migration.
- No large architecture rewrite in a single pass.
- No unverified claims for device-only or signed-release checks.

## Ordered milestones

### Milestone 1 — Deep audit and evidence capture
- Acceptance criteria:
  - All required docs refreshed with repo-specific findings.
  - Findings include severity, impacted files, production impact, recommendation, now/later decision, and risk.
- Validation commands:
  - `git status --short`

### Milestone 2 — Immediate high-value fixes
- Acceptance criteria:
  - Remove unsafe repository artifacts with no runtime impact.
  - Record actions in `implementation-log.md`.
- Validation commands:
  - `git status --short`
  - `./gradlew :app:testDebugUnitTest --console=plain`
  - `./gradlew :app:lintDebug --console=plain`

### Milestone 3 — Production-readiness reconciliation
- Acceptance criteria:
  - Docs reflect implemented changes and remaining blockers.
  - Commit produced with reviewable scope.
- Validation commands:
  - `git status --short`

## Rollback/risk notes
- This pass targets low-risk hygiene/documentation improvements.
- Any non-documentation change should remain isolated and reversible.
