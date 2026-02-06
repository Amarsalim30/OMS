# App Review Refresh (2026-02-06, Open Issues Implemented)

Scope reviewed:
- `app/src/main/java/com/zeynbakers/order_management_system`
- `app/src/main/res`
- Build quality gates (`testDebugUnitTest`, `lintDebug`)

Validation (latest):
- `./gradlew.bat :app:testDebugUnitTest --no-daemon` (pass)
- `./gradlew.bat :app:lintDebug --no-daemon` (pass)
- Lint summary: `0 errors, 18 warnings, 9 hints`
  - Evidence: `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`

## Implemented In This Pass

### 1) Implemented - Feature state/callback coupling reduced
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:64`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:86`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:92`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:106`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:113`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:120`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt:127`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:381`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:507`
- What changed:
  - Added grouped feature state bundles (`AppCalendarState`, `AppOrdersState`, `AppCustomersState`, `AppAccountsState`).
  - Added grouped callback bundles (`AppCalendarCallbacks`, `AppCustomersCallbacks`, `AppAccountsCallbacks`).
  - `MainActivity` now prepares feature bundles once and passes compact objects into `AppFeatureNavHost`.

### 2) Implemented - Customer filter UX no longer depends on horizontal chip scrolling
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:231`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:435`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:444`
- What changed:
  - Replaced horizontally scrolling chip rows with `FlowRow` for filter groups.
  - Sort action remains visible in a stable trailing row.

### 3) Implemented - Build hygiene warnings addressed (high-value subset)
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/PendingIntentFactory.kt:9`
  - `gradle/libs.versions.toml:50`
  - `gradle/libs.versions.toml:53`
  - `app/build.gradle.kts:86`
  - `app/build.gradle.kts:96`
- What changed:
  - Removed obsolete SDK check in `PendingIntentFactory` (`FLAG_IMMUTABLE` now unconditional for minSdk 24).
  - Migrated previously hardcoded Gradle dependencies to version catalog aliases (`UseTomlInstead` cleanup).

### 4) Implemented - KTX migration cleanup for URI and SharedPreferences operations
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:192`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:4`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/calendar/CalendarPreferences.kt:4`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:656`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:732`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/notifications/NotificationPreferences.kt:4`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/updates/UpdatePreferences.kt:4`
- What changed:
  - Switched URI parsing callsites to `toUri()`.
  - Switched SharedPreferences writes to `androidx.core.content.edit`.

### 5) Implemented - Launcher asset warning set reduced
- Evidence:
  - `app/src/main/AndroidManifest.xml:11`
  - Removed old launcher resources (`ic_launcher.png`, `ic_launcher_round.png`, legacy adaptive xml + unused launcher xml drawables).
- What changed:
  - Consolidated launcher icon usage to `@mipmap/ic_launcher_foreground`.
  - Removed duplicate/unused launcher resources and resized icon content to reduce shape issues.

### 6) Implemented - Top-level deprecated icon usage removed
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:361`
- What changed:
  - Replaced deprecated `Icons.Filled.ListAlt` with `Icons.AutoMirrored.Filled.ListAlt`.

## Current Remaining Items

### 1) Dependency currency warnings (non-blocking)
- Evidence:
  - `gradle/libs.versions.toml:2`
  - `gradle/libs.versions.toml:5`
  - `gradle/libs.versions.toml:19`
- Notes:
  - Remaining warnings are primarily upgrade suggestions (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`).
  - These are not regressions or release blockers; they require a coordinated dependency-upgrade sprint and compatibility validation.

## Status Summary
- Open implementation issues from the previous review are completed.
- Quality gate status is healthy (`0` lint errors, tests passing).
- Remaining work is version-upgrade maintenance, not app-flow defects.
