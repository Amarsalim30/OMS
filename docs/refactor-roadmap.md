# Refactor Roadmap

Date: 2026-03-07

## Principles
- Keep milestones production-safe and reviewable.
- Do not mix licensing-critical changes with unrelated broad rewrites.
- Add tests before or alongside every extraction that changes risky behavior.

## Ordered Milestones
| Milestone | Focus | Why | Acceptance criteria | Risk |
|---|---|---|---|---|
| 1 | Trusted licensing device claims | Current `maxDevices` enforcement is improved but still not server-trusted. | Device registration no longer depends on the current client-driven claim path; trusted rules/design documented; licensing checklist updated. | Medium-High |
| 2 | App shell decomposition | `MainAppContent.kt` is too broad for safe iteration. | Route-specific state holders extracted with no nav behavior change; smoke tests and build remain green. | Medium-High |
| 3 | Backup subsystem decomposition | `BackupManager.kt` is too large and multi-purpose. | Archive build, target write, restore validation, and restore execution have separate collaborators and tests. | Medium-High |
| 4 | Worker hardening | Background reliability and battery efficiency still need cleanup. | Contacts/notification/backup workers classify retryable vs permanent failures and log consistently. | Medium |
| 5 | Test and CI restoration | Device-backed confidence is incomplete and the local connected suite is unstable. | Connected tests reproducible in CI/local, migration replay fixed, high-risk flows gated before release. | Medium |

## Deferred But Valuable
- Customer search scalability work (prefix or FTS).
- Centralized telemetry with redaction policy.
- Longer-term modularization if app-shell and backup decomposition are not enough.
