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


## Milestone 10 - Licensing Rule Alignment Recovery
- Acceptance criteria:
  - Current Firestore rules allow first-device registration for existing entitlement docs that still rely on the documented default `maxDevices=1`.
  - The Android licensing flow remains compatible with older deployed Firestore rulesets by falling back to the legacy device-doc registration path only when the transaction write is denied for compatibility reasons.
  - Licensing verification records the exact commands run after the rules/code compatibility recovery.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --console=plain --no-daemon`
  - `./gradlew :app:testDebugUnitTest --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepository.kt app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepositoryTest.kt docs/requirements/licensing.md firestore.rules plans.md implementation-log.md`


## Milestone 11 - Live Licensing Recovery
- Acceptance criteria:
  - The currently affected signed-in device can pass licensing validation against the live Firebase project without `Validation fatal failure`.
  - The exact live-firestore evidence is documented, including whether the recovery is a general code/rules fix or an admin-side entitlement/device repair.
  - Any remaining gap in generic first-device self-registration is called out explicitly instead of being marked complete.
- Verification commands:
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `adb -s 192.168.0.192:34559 install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`
  - `cmd /c firebase deploy --only firestore:rules --project ordermanagementsystem-36b41 --non-interactive --force`
  - `adb -s 192.168.0.192:34559 logcat -d | Select-String -Pattern "LicensingRepository|Validation success|Validation fatal failure|PERMISSION_DENIED"`


## Milestone 12 - First-Device Registration Repair (Current Pass)
- Acceptance criteria:
  - First-device registration no longer rewrites entitlement controls on `users/{uid}` and instead updates only `registeredDeviceIds`.
  - Firestore rules validate only the `registeredDeviceIds` diff so older entitlement docs and extra admin metadata do not break allowed self-registration.
  - Verification includes focused JVM coverage plus live rules deployment and a real device registration retry.
  - If the attached device cannot reach Firestore cleanly, the exact transport blocker is documented instead of claiming a live green result.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRegistrationHelpersTest --console=plain --no-daemon`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `cmd /c firebase deploy --only firestore:rules --project ordermanagementsystem-36b41 --non-interactive --force`
  - `adb -s 192.168.0.192:34559 install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`
  - `adb -s 192.168.0.192:34559 logcat -c`
  - `adb -s 192.168.0.192:34559 logcat -d | Select-String -Pattern "LicensingRepository|Validation success|Validation fatal failure|PERMISSION_DENIED|UNAVAILABLE|GoogleApiManager"`


## Milestone 13 - Entitlement-First Sign-In Policy (Current Pass)
- Acceptance criteria:
  - `allowed=true` is the only runtime access gate after entitlement fetch succeeds.
  - Device lookup, revoke state, device-limit results, and registration write failures never surface `Validation fatal failure` for an entitled user.
  - Licensing tests and docs reflect the entitlement-first policy clearly.
- Verification commands:
  - `./gradlew :app:testDebugUnitTest --tests com.zeynbakers.order_management_system.core.licensing.LicensingRepositoryTest --console=plain --no-daemon`
  - `./gradlew :app:testDebugUnitTest --console=plain --no-daemon`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`


## Milestone 14 - Auth Picker UX Refinement (Current Pass)
- Acceptance criteria:
  - The signed-out gate makes the direct Google account chooser the primary CTA.
  - The alternate Credential Manager Google flow remains available as a secondary fallback action.
  - Login copy makes it explicit that the app is Google-only and does not support email/password auth.
- Verification commands:
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/AuthGate.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 15 - Dual-Mode Auth UX Refinement (Current Pass)
- Acceptance criteria:
  - The redundant secondary Google fallback action is removed from the signed-out gate.
  - Google account selection remains the primary sign-in CTA.
  - Inline email/password sign-in is available as the alternate authentication path without changing the licensing gate after sign-in.
- Verification commands:
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/licensing/AuthGate.kt app/src/main/res/values/strings.xml docs/requirements/licensing.md plans.md implementation-log.md`


## Milestone 16 - Intro Overview UX Refinement (Current Pass)
- Acceptance criteria:
  - The intro route reads as an overview screen instead of implying a hidden pager model.
  - The duplicate skip action is removed and the primary CTA clearly signals that quick setup comes next.
  - The intro includes a concrete mini workspace preview instead of relying only on generic value bullets.
- Verification commands:
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 17 - Intro Screen Weight Reduction (Current Pass)
- Acceptance criteria:
  - The intro keeps the clearer overview structure but removes the heavy workspace preview block.
  - The booking/dashboard value returns as a lightweight value row so the first-run message stays complete.
  - Unused preview-only strings and composables are removed.
- Verification commands:
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 18 - Setup Flow UX Refinement (Current Pass)
- Acceptance criteria:
  - The quick-setup checklist no longer auto-advances when a step becomes complete.
  - Users can leave optional setup immediately after the required business profile is finished.
  - Setup copy makes it explicit that the final action continues into the walkthrough, not directly into the app.
- Verification commands:
  - `rg -n --hidden -S "setup_subtitle_ready|setup_action_finish_later|setup_action_continue_walkthrough" app/src/main/java app/src/main/res`
  - `rg -n --hidden -S "LaunchedEffect\(steps\.map \{ it\.done \}" app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt`
  - `rg -n --hidden -F ' | ' app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 19 - Returning-User Backup UX Refinement (Current Pass)
- Acceptance criteria:
  - Quick setup treats backup as complete only when the remembered single-file target is still healthy.
  - Returning users with a stale remembered backup target see reconnect-oriented copy and action instead of being treated as fully configured.
  - The backup step copy makes it clear that one backup file is remembered and reused across launches.
- Verification commands:
  - `rg -n --hidden -S "backupTargetHealth|onReconnectBackupFile|OpenWritableBackupDocumentContract|setup_backup_relink_needed|setup_backup_unavailable|setup_backup_reconnect_action" app/src/main/java app/src/main/res`
  - `rg -n --hidden -S "backupConfigured = hasBackupFile && backupTargetHealth == BackupTargetHealth\.Healthy|updateBackupTarget\(|relinkBackupFileLauncher|setup_item_backup_body" app/src/main/java app/src/main/res`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 20 - Reinstall Backup Single-Source UX (Current Pass)
- Acceptance criteria:
  - Fresh-install quick setup prefers reopening an existing canonical backup file before creating a new one.
  - Creating a new onboarding backup file uses the same canonical `backup_latest.oms` suggestion as the backup settings screen.
  - The backup step explicitly explains the reinstall path so users do not create duplicate backup files by default.
- Verification commands:
  - `rg -n --hidden -S "onOpenExistingBackupFile|setup_backup_open_existing_action|setup_backup_create_action|setup_backup_single_source_hint|backup_latest\.oms" app/src/main/java app/src/main/res`
  - `rg -n --hidden -S "onReconnectBackupFile|setup_backup_choose_folder_action|intialsetupbackupsave\.oms" app/src/main/java app/src/main/res`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`


## Milestone 21 - Backup Copy Simplification (Current Pass)
- Acceptance criteria:
  - The onboarding backup card removes the extra explanatory hint text and keeps only essential state and actions.
  - Backup labels and status copy are shorter and easier to scan.
  - Removed backup-only strings are not referenced anywhere in the app.
- Verification commands:
  - `rg -n --hidden -S "setup_backup_auto_enable_hint|setup_backup_saved_hint|setup_backup_single_source_hint|setup_backup_relink_hint|setup_backup_unavailable_hint" app/src/main/java app/src/main/res`
  - `rg -n --hidden -S "setup_item_backup_body|setup_backup_open_existing_action|setup_backup_create_action|setup_backup_relink_needed|setup_backup_unavailable|setup_backup_reconnect_action" app/src/main/java app/src/main/res`
  - `./gradlew :app:assembleDebug --console=plain --no-daemon`
  - `git diff -- app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt app/src/main/res/values/strings.xml plans.md implementation-log.md`
