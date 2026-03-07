# Production Readiness Plan

Date: 2026-03-07

## Goals
- Audit the repo deeply enough to produce severity-ranked, evidence-backed findings.
- Ship safe, reviewable fixes in the highest-value areas touched by this pass.
- Keep documentation, plan, and implementation log aligned with actual code and verification state.

## Non-Goals
- No speculative architecture rewrite.
- No undocumented auth/back-end product changes.
- No claim that device-only verification is complete when it is not.

## Ordered Milestones
### Milestone 1 - Deep Audit And Evidence Capture
- Acceptance criteria:
  - Required audit docs refreshed with repo-specific findings.
  - Findings include severity, affected files, impact, recommendation, implement-now-vs-later, and change risk.
  - Release blockers identified explicitly.
- Verification commands:
  - `git status --short`

### Milestone 2 - Correctness And Security Hardening
- Acceptance criteria:
  - Auth gate matches documented Google-only licensing flow.
  - Licensing cache is hardened and repository logic is testable.
  - Parser crash path is guarded and covered by tests.
  - At least one verified performance hotspot is improved without changing product behavior.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --console=plain`
  - `./gradlew :app:lintDebug --console=plain`
  - `./gradlew :app:assembleRelease --console=plain`

### Milestone 3 - Production Readiness Reconciliation
- Acceptance criteria:
  - Docs show fixed vs open work and current release blockers.
  - Implementation log records actual commands, failures, and blockers from this pass.
  - Unverifiable items and tooling blockers are stated plainly.
- Verification commands:
  - `./gradlew :app:connectedDebugAndroidTest --console=plain`
  - `adb devices`
  - `git status --short`

## Rollback And Risk Notes
- This pass intentionally stayed within licensing, manifest hardening, payment-intake resilience, and a small SQL optimization.
- Remaining high-risk work is documented instead of being forced into an unsafe rewrite.
- The biggest open risk is still server-trusted device-limit enforcement; no local-only patch in this pass closes that gap safely.
- Connected-test harness instability is documented as a release blocker instead of being papered over with unverified runner changes.
- Late-session local Compose reruns degraded from targeted passes to `No compose hierarchies found in the app`, so local connected verification cannot be treated as a stable gate from this machine.
