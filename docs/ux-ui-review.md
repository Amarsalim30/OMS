# UX/UI Review

Date: 2026-03-07

## Summary
- The app already contains detailed screen-level UX reviews under `docs/issues/`, so this document focuses on production-facing cross-cutting UX findings from the current pass.
- The most important UX correction made here was aligning login behavior with the documented entitlement model: approved Google account in, blocked reason out, no extra credential path.

## Findings
| ID | Severity | Affected files | Production impact | Recommendation | Implement now vs later | Change risk | Status |
|---|---|---|---|---|---|---|---|
| UX-1 | Medium | `core/licensing/AuthGate.kt`, `strings.xml` | Email/password login and self-signup created a confusing auth story and did not match the product requirement. | Keep the login surface single-purpose: approved Google account only. | Implemented now. | Low | Fixed |
| UX-2 | Medium | `core/licensing/AuthGate.kt` | Blocked users get a reason and retry, but no stronger support or remediation guidance. | Add concise admin/help copy once support workflow is documented. | Later. | Low | Open |
| UX-3 | Medium | order/customer/accounting screens | Loading, empty, and error state consistency still varies by screen, increasing cognitive load in long workflows. | Converge on shared async-state patterns and explicit retry affordances. | Later. | Medium | Open |
| UX-4 | Low-Medium | app-wide accessibility and instrumentation coverage | Accessibility confidence still depends on limited smoke checks and issue-review docs rather than a green device-backed suite. | Restore connected UI test coverage and manual accessibility checklist execution before release. | Later. | Medium | Open |

## Implemented In This Pass
- Simplified the auth gate UI to one documented sign-in path.
- Kept the existing blocked-reason screen intact so operator messaging still stays clear when entitlement/device validation fails.

## Open UX Risks
- Existing issue docs still point to complexity in order creation, day detail, unpaid follow-up, and customer-heavy screens.
- A targeted device-backed accessibility pass succeeded, but full tactile validation for auth, notifications, widget behavior, and the entire Compose suite is still incomplete because the whole connected suite is unstable on this host.
