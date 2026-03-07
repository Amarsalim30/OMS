# Security Review

Date: 2026-03-07

## Security Posture Summary
- Firebase Auth + Firestore entitlement validation remains the correct platform direction for this repo.
- This pass materially improved the client-side security posture. Device registration is now transaction-backed and rules-constrained, but the core trust problem remains: `maxDevices` is still enforced through client-driven Firebase calls and has not been proven tamper-resistant against a modified client.

## Findings
| ID | Severity | Affected files | Production impact | Recommendation | Implement now vs later | Change risk | Status |
|---|---|---|---|---|---|---|---|
| SEC-1 | High | `firestore.rules`, `core/licensing/LicensingRepository.kt` | Device-limit enforcement is stronger than before, but it is still client-driven and bypassable by a sufficiently modified client. | Keep the transaction/rules hardening, then move to a trusted claim design and validate it against live Firebase before release. | Later, but release-blocking for strong anti-redistribution guarantees. | Medium-High | Open |
| SEC-2 | Medium | `core/licensing/AuthGate.kt`, `strings.xml` | Undocumented email/password auth expanded the attack surface and allowed auth behavior outside the required Google Credential Manager flow. | Keep Google-only sign-in unless a documented fallback is explicitly approved. | Implemented now. | Low-Medium | Fixed |
| SEC-3 | Medium | `core/licensing/LicensingLocalStore.kt` | Licensing cache state was stored in plaintext preferences. | Keep encrypted preference storage and treat client state as advisory only. | Implemented now. | Low | Mitigated |
| SEC-4 | Low | `AndroidManifest.xml` | Cleartext denial was implicit, not explicit. | Keep `android:usesCleartextTraffic="false"` as a manifest guardrail. | Implemented now. | Low | Fixed |
| SEC-5 | Low-Medium | `AndroidManifest.xml`, `core/widget/TodayUnpaidWidgetProvider.kt` | Widget lifecycle broadcasts were less constrained than necessary. | Keep the widget receiver protected with `android.permission.BIND_APPWIDGET` and defensive action filtering. | Implemented now. | Low | Fixed |
| SEC-6 | Medium | licensing, backup, notifications, sync work | Production incident analysis still relies on local logs. | Add centralized crash/event telemetry with PII redaction rules. | Later. | Low-Medium | Open |

## Compliant With Licensing Requirements
- Auth gate remains at app entry.
- Signed-out -> login, signed-in -> validate entitlement/device, allowed -> app, blocked -> reason screen is preserved.
- Firebase Auth and Firestore remain the only auth/entitlement backend path in the codebase.
- Device binding still uses generated install UUID, not IMEI.
- Offline grace window behavior remains in place.

## Verification Notes
- Code, lint, and release-build verification passed in this pass.
- A targeted device-backed accessibility/instrumentation run passed.
- Full connected-suite verification is still blocked by local Gradle/UTP/instrumentation instability.
- Firestore-rules behavior was reviewed statically only in this shell session; trustworthy max-device enforcement still requires broader design work plus live Firebase validation.
