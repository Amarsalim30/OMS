# Production Readiness Audit

Date: 2026-03-06
Scope: Full repository review across architecture, UX, reliability, security, performance, testing, and delivery.

## Executive summary

Current state: **materially improved and close to production-ready, but not fully release-complete**.

This pass restored a green local verification baseline, fixed a real order-entry correctness defect, removed stale code and repo artifacts, and added sanitized operational logging around the most failure-sensitive licensing and backup or restore paths. The earlier local `25.0.1` toolchain issue is no longer an active blocker in this environment.

### Completed in this pass
- Fixed the order-editor customer suffix parser so product-only or amount-only tails no longer trigger customer typeahead.
- Expanded parser regression coverage in `OrderEditorInputTest.kt`.
- Resolved the two hard Compose lint violations by replacing configuration-unsafe resource access in composables.
- Removed dead legacy order UI files and a stray checked-in log artifact.
- Added sanitized logs around licensing validation and backup or restore execution.
- Verified debug unit tests, debug lint, debug assembly, release lint, and release assembly locally.

### Top remaining blockers
1. Device-backed acceptance validation was not executed in this pass, including `:app:connectedDebugAndroidTest`, the licensing P0 checklist, and release-signed Google Sign-In.
2. `core/backup/BackupManager.kt` and `core/helper/HelperOverlayService.kt` remain large, high-risk maintenance hotspots.
3. Firebase operational controls such as API key restrictions, signing fingerprint governance, and deployed rules status remain external to the repository and must be checked outside code review.

## Category scoring (0-10)

- Correctness and crash resilience: **7.5/10**
- Security and data safety: **7.0/10**
- UX polish and accessibility: **7.0/10**
- Performance: **6.5/10**
- Testing confidence: **7.5/10**
- Build and release readiness: **8.0/10**
- Maintainability: **6.5/10**
- Observability and operations: **6.5/10**

Overall score: **7.1/10**

## Verification completed in this pass
- `./gradlew.bat :app:testDebugUnitTest --console=plain --stacktrace` -> passed
- `./gradlew.bat :app:lintDebug --console=plain --stacktrace` -> passed
- `./gradlew.bat :app:assembleDebug --console=plain --stacktrace` -> passed
- `./gradlew.bat :app:lintRelease --console=plain --stacktrace` -> passed
- `./gradlew.bat :app:assembleRelease --console=plain --stacktrace` -> passed
- `:app:connectedDebugAndroidTest` -> not run in this pass; device or emulator unavailable
- `docs/requirements/licensing-verification-checklist.md` -> not run in this pass; requires Firebase-backed manual validation and release signing context

## Detailed findings

### 1) Order-entry customer suffix parsing was too permissive
- Severity: **high**
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorInput.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/order/ui/OrderEditorInputTest.kt`
- Why it matters in production: customer typeahead could be triggered by trailing product or amount text, which risks wrong customer suggestions and incorrect note cleanup.
- Recommended fix: only extract a customer query from the actual trailing suffix after the final numeric token on the last line.
- Implement now or later: **implemented now**
- Risk of change: **low**
- Status: **implemented and verified**

### 2) Compose resource access violated lint-safe configuration rules
- Severity: **high**
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CustomersGraph.kt`
- Why it matters in production: lint blockers reduce release confidence and configuration-unsafe resource access can age badly around locale or configuration changes.
- Recommended fix: use `stringResource(...)` or `LocalResources.current` and remember derived values where needed.
- Implement now or later: **implemented now**
- Risk of change: **low**
- Status: **implemented and verified**

### 3) Dead legacy UI code and checked-in log output created hygiene risk
- Severity: **medium**
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditor.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCard.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/logs`
- Why it matters in production: stale files increase review noise, confuse future maintainers, and can hide accidental reintroduction of obsolete flows.
- Recommended fix: remove dead files and non-source artifacts from the source tree.
- Implement now or later: **implemented now**
- Risk of change: **low**
- Status: **implemented and verified**

### 4) Observability around licensing and backup or restore was too thin
- Severity: **high**
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Why it matters in production: without stable logs for start, success, blocked reason, and failure mode, incident triage is slower and field support becomes guesswork.
- Recommended fix: add sanitized reason-code logging around validation and backup or restore flows, then follow with central crash and telemetry integration later.
- Implement now or later: **implemented now, expand later**
- Risk of change: **low**
- Status: **partially addressed**

### 5) Large hotspot files still carry maintainability and regression risk
- Severity: **high**
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
- Why it matters in production: the largest files still mix multiple responsibilities, making defects harder to isolate and risky refactors harder to verify.
- Recommended fix: continue staged extraction with tests, prioritizing backup and helper service collaborators first. `MainAppContent.kt` is still large, but some shell and navigation responsibilities have already been split into `MainAppHostScaffold.kt`, `AppFeatureNavHost.kt`, and `core/navigation/graphs/*`.
- Implement now or later: **later**
- Risk of change: **medium-high**
- Status: **open**

### 6) Device-backed release acceptance coverage remains incomplete
- Severity: **critical**
- Affected areas:
  - licensing and auth flows
  - backup and restore flows
  - overlay and microphone permission flows
  - release-signed Google Sign-In
- Why it matters in production: the highest-risk acceptance cases still require a real device or emulator plus Firebase-backed configuration to prove release readiness.
- Recommended fix: run `:app:connectedDebugAndroidTest`, execute every item in `docs/requirements/licensing-verification-checklist.md`, and validate release-signed Google Sign-In with the production signing certificate fingerprints configured.
- Implement now or later: **blocked in this environment**
- Risk of change: **low to code, high to release if skipped**
- Status: **open**

### 7) Firebase operational governance still depends on external controls
- Severity: **high**
- Affected files:
  - `app/google-services.json`
  - `firestore.rules`
  - Firebase project configuration outside the repo
- Why it matters in production: Firebase config files in source control are normal, but safe operation still depends on Android app restrictions, SHA certificate fingerprints, enabled API scope, and correct rules deployment.
- Recommended fix: verify API key restrictions, signing fingerprints, and deployed rules in the Firebase console or release runbook.
- Implement now or later: **later, outside the code-only scope of this pass**
- Risk of change: **low to code, medium operationally**
- Status: **open**

### 8) Accessibility and UX consistency still need a device-backed pass
- Severity: **medium**
- Affected areas: auth, order entry, payment intake, backup or restore, and overlay permission surfaces
- Why it matters in production: keyboard flow, focus order, permission prompts, and empty or error states can still regress even when unit tests and lint are green.
- Recommended fix: run a focused accessibility and UX acceptance pass on the critical user journeys before GA.
- Implement now or later: **later**
- Risk of change: **low-medium**
- Status: **open**

## Release recommendation
- This codebase is in a stronger state than the initial March audit suggested.
- It is suitable for continued QA and release-candidate hardening.
- Do not treat this pass as final GA sign-off until the remaining blockers below are cleared.

## remaining blockers
- No device or emulator was available in this pass, so `:app:connectedDebugAndroidTest` was not run.
- The manual licensing P0 checklist in `docs/requirements/licensing-verification-checklist.md` is still open.
- Release-signed Google Sign-In could not be verified from this local environment because it depends on production signing fingerprints and Firebase configuration.
- `BackupManager.kt` and `HelperOverlayService.kt` remain large, high-risk files that still need staged decomposition and targeted tests.
- Firebase API key restrictions, deployed Firestore rules status, and signing key governance are external controls and cannot be proven from repository contents alone.
