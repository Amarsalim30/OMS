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


## Milestone 4 - QA Defect Sweep And Priority Fixes (Current Pass)
- Acceptance criteria:
  - Produce an evidence-backed bug/UX defect list grouped by confidence level.
  - Fix at least the highest-priority confirmed integrity/state issues discovered in this sweep.
  - Record environment blockers that prevent full Gradle verification.
- Verification commands:
  - `./gradlew assembleDebug`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintDebug`
  - `git status --short`


## Milestone 5 - Auth Fallback + Onboarding Resume
- Acceptance criteria:
  - Google sign-in provides a clear fallback interaction path when the default credential prompt fails.
  - App relaunch resumes onboarding at setup checklist when intro is already completed but onboarding is not finished.
  - Changes are documented with verification attempts and environment blockers.
- Verification commands:
  - `./gradlew assembleDebug`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintDebug`
  - `git status --short`


## Milestone 6 - AuthGate Compile Stabilization
- Acceptance criteria:
  - `AuthGate.kt` compiles with the Google sign-in fallback path intact.
  - Tracking docs are free of merge conflict markers.
- Verification commands:
  - `./gradlew :app:compileDebugKotlin --console=plain --no-daemon`
  - `./gradlew :app:testDebugUnitTest --console=plain --no-daemon`
  - `git grep -n "^<<<<<<< \\|^=======\\|^>>>>>>> " -- plans.md implementation-log.md`


## Milestone 7 - Authentication Compatibility Recovery
- Acceptance criteria:
  - Existing authorized devices are not blocked just because the validation heartbeat write fails after server validation already succeeded.
  - First-device registration remains compatible with both the current transaction-backed Firestore rules and older deployed rulesets that still deny `users/{uid}` updates.
  - Regression coverage exists for compatibility fallback and heartbeat-write failures.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --console=plain --no-daemon`
  - `./gradlew :app:testDebugUnitTest --console=plain --no-daemon`


## Milestone 7 - Authentication Compatibility Recovery
- Acceptance criteria:
  - Allowed accounts continue to authenticate on backends that still use the older device-registration Firestore rules.
  - Newer `registeredDeviceIds` transaction registration remains the primary path.
  - Unit tests cover both compatibility fallback and non-compatibility failures.
- Verification commands:
  - `./gradlew :app:compileDebugKotlin --console=plain --no-daemon`
  - `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepositoryTest.kt`


## Milestone 8 - Auth Compatibility Fallback Hardening
- Acceptance criteria:
  - Allowed accounts are not blocked if the legacy-registration compatibility error reaches the repository as a raw or nested Firestore exception instead of the custom wrapper type.
  - Licensing unit coverage pins the nested Firestore permission-denied fallback path.
  - The exact verification blocker is documented if local Gradle execution cannot reach plugin resolution offline in this environment.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepositoryTest.kt plans.md implementation-log.md`


## Milestone 9 - Active Device Slot Recovery
- Acceptance criteria:
  - Allowed accounts are not blocked just because `registeredDeviceIds` still contains stale or revoked device IDs from post-`835f6c` licensing changes.
  - Transaction registration reconciles claimed device IDs against active non-revoked device docs before applying `maxDevices`.
  - Firestore rules allow the repaired client flow to prune stale device claims while still preventing entitlement edits.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRegistrationHelpersTest --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingRegistrationHelpersTest.kt firestore.rules plans.md implementation-log.md`


## Milestone 9 - Install ID Persistence Recovery
- Acceptance criteria:
  - A previously allowed device does not get a new install UUID just because encrypted licensing storage is unavailable on a later launch.
  - Licensing unit coverage documents the secure-first, legacy-fallback install ID selection logic.
  - Verification attempts and blockers are recorded for this pass.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingLocalStoreTest --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingLocalStore.kt app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingLocalStoreTest.kt plans.md implementation-log.md`
