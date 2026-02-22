# Production Readiness Backlog (2026-02-22)

## Scope and evidence
- Repo scan of `app/`, `docs/`, build config, manifest, and data layer.
- Build checks run:
  - `./gradlew :app:testDebugUnitTest` (pass)
  - `./gradlew :app:assembleRelease` (pass)
  - `./gradlew :app:lintRelease` (fail: 15 errors, 193 warnings, 8 hints)
- Prioritization principle: release blockers first, then data integrity/reliability, then scalability and maintainability.

## Prioritized backlog

### 1) Lint release blockers are failing the production gate
- Severity: Critical
- Impact: Release quality gate fails (`lintRelease`), and known runtime failure risks remain.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupAttentionNotifier.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperCaptureActivity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/ui/NotesHistoryScreen.kt`
- Suggested fix:
  - Resolve all 15 lint errors (permission guard, API-level guard, Compose flow usage, Compose resource access patterns).
- Acceptance criteria:
  - `./gradlew :app:lintRelease` passes with 0 errors.
  - No suppression added for these 15 errors unless justified in code review.

### 2) Helper foreground-service policy/compliance risk
- Severity: Critical
- Impact: Overlay helper may violate Android/Play foreground-service policy semantics and can regress on newer platform behavior.
- Affected files:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayController.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
- Suggested fix:
  - Align FGS type and behavior with actual workload.
  - Keep mic capture in a user-visible activity path when required by while-in-use constraints.
  - Add compatibility fallback path for OEM/platform restrictions.
- Acceptance criteria:
  - No `SecurityException`/`ForegroundServiceStartNotAllowedException` in helper flows on Android 13-16 test devices.
  - Cross-app capture works when permissions are granted; graceful fallback when denied.

### 3) No CI pipeline enforcing production gates
- Severity: High
- Impact: Regressions can merge without lint/test/release checks.
- Affected files:
  - `.github/workflows` (missing)
  - `app/build.gradle.kts`
- Suggested fix:
  - Add CI workflow for `:app:testDebugUnitTest`, `:app:lintRelease`, and `:app:assembleRelease`.
- Acceptance criteria:
  - Every PR runs all three checks and blocks merge on failure.

### 4) Release hardening is disabled (`isMinifyEnabled = false`)
- Severity: High
- Impact: Larger APK, weaker reverse-engineering resistance, and no minified-build validation before production.
- Affected files:
  - `app/build.gradle.kts`
  - `app/proguard-rules.pro`
- Suggested fix:
  - Enable R8 minification and resource shrinking for release.
  - Add required keep rules and run minified smoke tests.
- Acceptance criteria:
  - Release build runs with minification enabled and key user flows pass smoke tests.
  - No critical reflection/serialization regressions in release variant.

### 5) Room schema export is disabled
- Severity: High
- Impact: Schema drift is harder to review and migration regressions are easier to miss.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/db/DatabaseProviderMigrationTest.kt`
- Suggested fix:
  - Enable `exportSchema = true` and commit schema artifacts.
  - Add real migration replay tests using Room migration test APIs (not only SQL string assertions).
- Acceptance criteria:
  - Historical migration path tests pass against real DB files.
  - Schema JSON snapshots are versioned in repo.

### 6) Missing referential constraints for core accounting/order links
- Severity: High
- Impact: Orphaned rows and silent integrity drift are possible under edge cases and manual data operations.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderEntity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/data/AccountEntryEntity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/data/PaymentReceiptEntity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/data/PaymentAllocationEntity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/DatabaseProvider.kt`
- Suggested fix:
  - Add explicit foreign keys and corresponding migrations with clear delete/update semantics.
- Acceptance criteria:
  - Invalid references cannot be inserted.
  - Migration preserves existing records and integrity checks pass.

### 7) Large-list scalability relies on unbounded/full-load queries
- Severity: High
- Impact: Memory pressure and slower UI as customer/order/receipt volume grows.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderDao.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/data/AccountingDao.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/data/HelperNoteDao.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt`
- Suggested fix:
  - Introduce pagination strategy (Paging 3 or explicit page/limit APIs) for high-volume screens.
- Acceptance criteria:
  - App remains responsive with large seeded dataset (for example: 10k customers, 50k orders).
  - No OOM or ANR during list navigation.

### 8) Heavy transformation work can still happen on UI path for dense customer records
- Severity: High
- Impact: Jank when loading customer detail/statement on large ledgers.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt`
- Suggested fix:
  - Move expensive grouping/sorting/statement model building to `Dispatchers.Default` and profile with realistic data.
- Acceptance criteria:
  - Customer detail open remains smooth on large ledgers.
  - No long main-thread stalls in trace/profile run.

### 9) Backup onboarding SAF permission persistence differs from settings flow
- Severity: High
- Impact: Backup setup can look successful but fail to persist provider access on some OEM/providers.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- Suggested fix:
  - Reuse one shared, provider-safe URI permission persistence helper in both onboarding and settings.
- Acceptance criteria:
  - URI access remains valid after app restart/device reboot for both onboarding and settings flows.

### 10) Backup target model and settings UI are inconsistent
- Severity: High
- Impact: Runtime supports `SafDirectory` but settings exposes only file flow, causing dead/ambiguous mode behavior.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Suggested fix:
  - Either remove directory mode from runtime or expose full directory mode UX and controls.
- Acceptance criteria:
  - Every persisted target type is user-selectable and testable from UI.
  - No unreachable/dead target mode remains.

### 11) Restore integrity policy default is `LegacyCompatible`
- Severity: High
- Impact: Weaker restore validation by default for new installs.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- Suggested fix:
  - Default new installs to strict manifest verification and expose explicit policy control.
- Acceptance criteria:
  - Fresh install defaults to strict policy.
  - Legacy restore remains available only via explicit user opt-in.

### 12) SAF health probe can produce false positives and write mode fallback is brittle
- Severity: High
- Impact: UI can show healthy target while writes fail; provider compatibility suffers.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Suggested fix:
  - Require concrete I/O probe for health.
  - Make output-mode fallback exception-safe per attempt (`wt` -> `w` -> default).
- Acceptance criteria:
  - Stale/invalid SAF file URIs report unhealthy.
  - Backup succeeds on providers that only support fallback output modes.

### 13) Dual backup systems can conflict (system auto-backup + custom backup)
- Severity: Medium
- Impact: Ambiguous restore source and support complexity in recovery scenarios.
- Affected files:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/xml/backup_rules.xml`
  - `app/src/main/res/xml/data_extraction_rules.xml`
- Suggested fix:
  - Define one authoritative restore strategy, then align Android auto-backup includes/excludes accordingly.
- Acceptance criteria:
  - Restore behavior is deterministic and documented.
  - Recovery drill validates the documented source-of-truth flow.

### 14) Backup archives are plain data at rest (no optional encryption)
- Severity: Medium
- Impact: External backup files can expose customer/financial data if copied/shared.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Suggested fix:
  - Add optional passphrase-based encryption (for example AES-GCM) and clear UX around key management.
- Acceptance criteria:
  - Encrypted backup cannot be opened without passphrase.
  - Restore path validates incorrect/passphrase-missing cases safely.

### 15) Launch UX still uses layered splash approach (theme + composable route)
- Severity: Medium
- Impact: Higher chance of visible flash/glitch on cold start across OEMs.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt`
  - `app/src/main/res/values/themes.xml`
- Suggested fix:
  - Consolidate to one startup mechanism (AndroidX SplashScreen API + clean handoff).
- Acceptance criteria:
  - Cold start shows one coherent splash surface with no black/white flash.

### 16) User-facing text is still hardcoded in several non-UI-resource paths
- Severity: Medium
- Impact: Localization and tone consistency regressions; harder copy maintenance.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/notifications/ReminderWorker.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt`
- Suggested fix:
  - Move user-visible strings to `strings.xml` and use formatted resources.
- Acceptance criteria:
  - No new hardcoded user-facing strings in touched modules.
  - Copy updates are possible from resources only.

### 17) Accessibility bug in helper bubble touch handling
- Severity: Medium
- Impact: TalkBack/assistive interaction is impaired for floating helper.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
- Suggested fix:
  - Ensure touch listener triggers `performClick()` on click path and improve content descriptions.
- Acceptance criteria:
  - Accessibility lint issue is resolved.
  - TalkBack can activate helper controls reliably.

### 18) Compose warning debt remains high (performance and API usage hygiene)
- Severity: Medium
- Impact: Increased maintenance cost and latent runtime/UI edge bugs.
- Affected files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarDayCellModern.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreenSections.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- Suggested fix:
  - Clean warning classes incrementally (modifier conventions, primitive state APIs, compose best practices).
- Acceptance criteria:
  - Warning count reduced with documented target per release cycle.

### 19) Dependency governance drift and direct version usage outside catalog
- Severity: Medium
- Impact: Upgrade risk and inconsistent dependency management.
- Affected files:
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
- Suggested fix:
  - Move hardcoded deps into version catalog.
  - Plan controlled dependency upgrades with smoke/regression matrix.
- Acceptance criteria:
  - No `UseTomlInstead` lint warning.
  - Upgrade plan and rollback notes documented.

### 20) Documentation drift against actual app version and schema
- Severity: Medium
- Impact: Onboarding/support confusion and incorrect operational assumptions.
- Affected files:
  - `README.md`
  - `app/build.gradle.kts`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
- Suggested fix:
  - Sync README with current version/build and real data model (remove stale references).
- Acceptance criteria:
  - README version/build/schema sections match current code exactly.

### 21) Large unused resource inventory indicates UI/content drift
- Severity: Low
- Impact: APK bloat and confusing maintenance surface.
- Affected files:
  - `app/src/main/res/values/strings.xml`
- Suggested fix:
  - Remove truly unused strings or annotate intentionally reserved resources.
- Acceptance criteria:
  - UnusedResources warning count is significantly reduced with no feature regression.

### 22) Repository hygiene: non-source binary artifacts in root
- Severity: Low
- Impact: Slower clones, noisy diffs, and accidental shipping risk.
- Affected files:
  - Repository root (for example `.zip`, `.jpeg`, `.jfif`, patch/log artifacts)
  - `.gitignore`
- Suggested fix:
  - Remove accidental binary artifacts from source control and extend ignore rules.
- Acceptance criteria:
  - Root contains only source/release-essential files.
  - New transient artifacts are ignored by default.

## Recommended execution order (short)
1. Items 1-4 (release gate + compliance + CI + hardening).
2. Items 5-12 (data integrity and backup reliability).
3. Items 13-18 (runtime UX/accessibility/performance).
4. Items 19-22 (maintenance and documentation hygiene).
