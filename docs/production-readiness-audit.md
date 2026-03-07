# Production Readiness Audit

Date: 2026-03-07
Scope: Kotlin/Compose single-module OMS app, with emphasis on licensing/auth, correctness, security, UX/accessibility, release readiness, and maintainability.

## Executive Summary
- The app is materially safer and more stable than at the start of this pass, and the core production changes are now unit-tested, lint-clean, and buildable in both debug and release variants.
- High-value fixes landed in licensing/auth, M-PESA parsing, contacts sync reliability, query efficiency, manifest hardening, calendar/customer accessibility, and CI connected-test coverage.
- The app is still not fully release-ready because two blockers remain:
  - device-limit enforcement is improved but still not server-trusted end-to-end
  - local Compose connected verification on this host/device is not reproducible due Gradle/UTP/instrumentation instability, and late-session reruns failed even for targeted classes

## Category Scorecard
- Correctness and crash resilience: 8.1 / 10
- Security and data safety: 8.0 / 10
- UX clarity and accessibility baseline: 7.8 / 10
- Performance efficiency: 7.4 / 10
- Testing confidence: 6.8 / 10
- Build and release readiness: 6.9 / 10
- Maintainability: 7.1 / 10
- Observability and operability: 6.0 / 10

Overall: 7.2 / 10

## Fixed In This Pass
- `core/licensing/AuthGate.kt`
  - Removed undocumented email/password sign-in and sign-up behavior.
  - Preserved the required entry flow: signed out -> login, signed in -> entitlement/device validation, allowed -> app, blocked -> clear reason screen.
  - Added lifecycle revalidation on app start.
- `core/licensing/LicensingLocalStore.kt`
  - Moved cached licensing state to encrypted preferences with migration fallback.
- `core/licensing/LicensingRepository.kt`, `firestore.rules`
  - Split remote/cache seams for testability.
  - Moved device registration to a Firestore transaction-backed claim path using `registeredDeviceIds`.
  - Tightened rules so self-service updates are constrained to append-only device registration state rather than entitlement fields.
- `AndroidManifest.xml`
  - Explicitly disabled cleartext traffic.
  - Added `android.permission.BIND_APPWIDGET` protection to the widget receiver.
- `accounting/mpesa/MpesaParser.kt`, `accounting/ui/PaymentIntakeViewModel.kt`
  - Malformed date/time fragments no longer crash the parse pipeline.
- `accounting/ui/PaymentIntakeHistoryViewModel.kt`, `order/data/OrderDao.kt`
  - Move-target order loading now filters/sorts open orders in SQL.
- `ContactsLoader.kt`, `core/contacts/ContactsSyncWorker.kt`
  - Added typed load outcomes and more correct `success` / `failure` / `retry` mapping.
- `order/ui/CalendarDayCellModern.kt`, `order/ui/SummaryScreen.kt`, `customer/ui/CustomerDetailSections.kt`, `core/ui/VoiceInputRouter.kt`
  - Restored calendar payment markers.
  - Fixed touch-target semantics for the summary copy action.
  - Hardened isolated Compose screen rendering with a safe voice-router fallback.
  - Improved customer-detail accessibility with 48dp actions and large-font stacking.
- `.github/workflows/android-ci.yml`
  - Added emulator-backed connected-test coverage in CI config, but that lane was not executed from this session.

## Prioritized Findings
| ID | Severity | Area | Affected files | Production impact | Recommendation | Implement now vs later | Change risk | Status |
|---|---|---|---|---|---|---|---|---|
| PR-1 | High | Licensing security | `firestore.rules`, `core/licensing/LicensingRepository.kt` | Device registration is better than the old count-then-write path, but `maxDevices` is still enforced through client-driven Firebase calls and is not yet proven tamper-resistant or live-validated. | Keep the transaction/rules hardening, then move to a stronger trusted-claim design and validate against live Firebase before release. | Later, but release-blocking. | Medium-High | Open |
| PR-2 | High | Connected verification | `app/src/androidTest/**`, `.github/workflows/android-ci.yml` | Local Compose connected testing is not reproducible on this host/device; late-session reruns failed even for targeted classes with `No compose hierarchies found in the app`. | Validate the configured CI emulator lane separately and re-run locally only from a clean Android toolchain/device state before release. | Later, environment-dependent. | Medium | Open |
| PR-3 | High | Maintainability | `MainAppContent.kt`, `core/backup/BackupManager.kt` | Large orchestration files still increase regression risk and review cost. | Split route state holders and backup collaborators incrementally behind stable interfaces. | Later. | Medium-High | Open |
| PR-4 | Medium | Parsing correctness | `accounting/mpesa/MpesaParser.kt`, `accounting/ui/PaymentIntakeViewModel.kt` | Malformed M-PESA metadata previously threatened parse-job stability. | Keep nullable calendar parsing and guarded async parse/build work. | Implemented now. | Medium | Fixed |
| PR-5 | Medium | Auth surface | `core/licensing/AuthGate.kt`, `docs/requirements/licensing.md` | The previous auth UI drifted from the documented entitlement model. | Keep the login surface Google-only unless a documented fallback is explicitly approved. | Implemented now. | Low-Medium | Fixed |
| PR-6 | Medium | Data safety | `core/licensing/LicensingLocalStore.kt` | Local licensing cache was previously plaintext. | Keep encrypted storage, but plan a supported replacement for deprecated Jetpack Security APIs. | Implemented now, follow-up later. | Low | Mitigated |
| PR-7 | Medium | Reliability | `ContactsLoader.kt`, `core/contacts/ContactsSyncWorker.kt` | Contacts sync previously blurred permanent vs transient failures and risked bad retry behavior. | Keep typed outcomes and preserve cancellation semantics. | Implemented now. | Low-Medium | Fixed |
| PR-8 | Medium | UX/accessibility | `order/ui/*`, `customer/ui/*`, `core/ui/VoiceInputRouter.kt` | Several touch-target and isolated-screen test paths were weak. | Keep the accessibility/touch-target fixes and continue expanding device-backed checks. | Implemented now. | Low-Medium | Fixed |
| PR-9 | Medium | Observability | licensing, backup, sync, notification workers | Local logs are still not enough for field triage. | Add centralized, redacted crash and event telemetry. | Later. | Low-Medium | Open |
| PR-10 | Low | Network and component hardening | `AndroidManifest.xml`, widget provider path | Cleartext policy and widget broadcast scope were not explicit enough. | Keep deny-by-default manifest settings and receiver protection. | Implemented now. | Low | Fixed |

## Release Blockers
1. Server-trusted device-limit enforcement is still unresolved. The current Firebase-only design is improved, but it is not yet strong enough to claim tamper-resistant `maxDevices` enforcement.
2. Local `:app:connectedDebugAndroidTest` execution is still blocked by host/device test-harness instability; by the end of this session even targeted Compose reruns were not reproducible.
3. Firestore rules and licensing flows still need live Firebase/admin validation on a signed build.

## Verification Commands And Outcomes
```text
git status --short                                                                                                  -> PASS
adb devices                                                                                                         -> PASS (192.168.0.192:34559 device)
./gradlew :app:compileDebugAndroidTestKotlin --console=plain --no-daemon                                           -> PASS
./gradlew :app:testDebugUnitTest --console=plain --no-daemon                                                       -> PASS
./gradlew :app:lintDebug --console=plain --no-daemon                                                               -> PASS
./gradlew :app:assembleRelease --console=plain --no-daemon                                                         -> PASS
./gradlew "-Pandroid.testInstrumentationRunnerArguments.class=com.zeynbakers.order_management_system.ui.AccessibilitySmokeTest" :app:connectedDebugAndroidTest --console=plain --no-daemon -> FAIL (`No compose hierarchies found in the app`)
```

## Known Limitations And Risks
- Live Firebase verification was not completed in this shell session; rules were reviewed statically and exercised only through local code paths/tests.
- Local connected-test evidence is still incomplete because Gradle/UTP/device state became unstable across both whole-suite and targeted Compose reruns.
- `EncryptedSharedPreferences` is a pragmatic near-term hardening step here, but the dependency is deprecated upstream and should be replaced by a supported secure-storage abstraction later.
