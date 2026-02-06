# App Review Status Refresh (2026-02-06)

Scope reviewed:
- `app/src/main/java/com/zeynbakers/order_management_system`
- Focus on orders, accounting, navigation, backup, and parsing flows.

Review basis:
- Current workspace diffs across app modules.
- Targeted static scans (`rg`) for high-risk paths.
- Unit tests: `./gradlew.bat :app:testDebugUnitTest --no-daemon` (pass, rerun after latest fixes).

## Resolved Since Previous Review

### 1) Resolved - Crash risk while editing order total
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCard.kt:41`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCard.kt:46`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCard.kt:54`
- What changed:
  - Invalid values now use `toBigDecimalOrNull()` guards.
  - Field exposes `isError`.
  - Save occurs on IME done / focus loss only when parse succeeds.

### 2) Resolved - Multi-step order/accounting mutations now transactional
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:108`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:312`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:385`
- What changed:
  - Save/cancel/delete-payment workflows are wrapped in `database.withTransaction`.
  - This reduces partial-write risks between orders and accounting entries.

### 3) Resolved - External intent deep routes deduped
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:315`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:324`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:942`
- What changed:
  - New `navigateCalendarExternal(...)` centralizes deep-link navigation.
  - Uses top-level reset + `launchSingleTop`/`popUpTo` to avoid duplicate stacking.

### 4) Resolved - Async race in M-PESA customer selection update
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeViewModel.kt:272`
- What changed:
  - Switched from stale snapshot remap to `_transactions.update { latest -> ... }`.
  - Prevents lost updates when parse-refresh and user selection overlap.

### 5) Resolved - Payment history loading no longer gets stuck on exceptions
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt:95`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt:101`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt:102`
- What changed:
  - `load()` now has `try/catch/finally`.
  - `CancellationException` is rethrown.
  - `_isLoading` is always reset and operational failures set `error`.

### 6) Resolved - Customer destructive action moved to archive-only behavior
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt:118`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:340`
- What changed:
  - Delete path now archives (`archiveById`) instead of hard-delete.
  - Customer list action text is archive-focused.

### 7) Resolved/Improved - Unicode parser behavior covered by tests
- Evidence:
  - `app/src/test/java/com/zeynbakers/order_management_system/order/domain/OrderNotesParserTest.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/util/VoiceMathParserTest.kt`
- What changed:
  - Added tests for real Unicode symbols (`U+2022`, `U+00D7`, `U+00F7`).
  - Prior parser reliability concern is now regression-tested.

### 8) Resolved - Backup parsing hardened and legacy `amountPaid` no longer exported
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:269`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:478`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:689`
- What changed:
  - New exports omit `amountPaid`.
  - Restore parsing now uses tolerant decimal parsing helpers and explicit field/index errors.
  - Legacy imports still read optional `amountPaid` for backward compatibility.

### 9) Resolved - Day-boundary refresh added for key long-lived screens
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/CurrentDateState.kt:20`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt:206`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/SummaryScreen.kt:107`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:92`
  - `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt:123`
- What changed:
  - Introduced `rememberCurrentDate()` with midnight invalidation.
  - Applied to reviewed screens to prevent stale "today" behavior overnight.

### 10) Resolved - Backup retry/cancellation handling and reliability tests added
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:108`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:150`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/backup/BackupManagerTest.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/ui/CurrentDateStateTest.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModelTest.kt`
- What changed:
  - Backup now rethrows cancellation and classifies retryable failures explicitly.
  - Added focused tests for backup decimal/retry logic, date rollover timing, and history error semantics.

### 11) Resolved - Date helper adoption completed for remaining reviewed composable
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:390`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:769`
- What changed:
  - `CustomerDetailScreen` write-off eligibility now uses `rememberCurrentDate()` instead of ad hoc `Clock` reads.

### 12) Resolved - `amountPaid` schema retirement completed
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderEntity.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderDao.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/DatabaseProvider.kt`
- What changed:
  - Removed `amountPaid` from `OrderEntity` and removed legacy DAO aggregate queries.
  - Bumped Room schema to version `10`.
  - Added migration `9 -> 10` that rebuilds `orders` without `amountPaid` while preserving data and indices.
  - Backup import still accepts legacy payloads and ignores `amountPaid` safely.

## Open Issues (Current)

### 1) Low - Worker/viewmodel "today" usage remains decentralized (acceptable but inconsistent)
- Evidence:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/widget/WidgetUpdater.kt:46`
  - `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt:317`
- Impact:
  - Business logic is correct (action-time read) but "today-source" policy is not unified across layers.
- Fix:
  - Document and enforce one rule: composables use `rememberCurrentDate`, non-UI layers read action-time `Clock`.

### 2) Low - Backup integration coverage is still partial
- Evidence:
  - `app/src/test/java/com/zeynbakers/order_management_system/core/backup/BackupManagerTest.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/db/DatabaseProviderMigrationTest.kt`
- Impact:
  - Reliability checks are strong at helper/migration-contract level, but full end-to-end restore behavior is not yet instrumented.
- Fix:
  - Add instrumentation-level restore test with a temporary DB and fixture zip.

## Improvement Options

### Option A (Recommended) - Reliability and test hardening sprint
- Scope:
  - Keep completed reliability/migration work as baseline.
  - Add instrumentation-level restore tests using sample backup payloads.
- Benefit:
  - Preserves the reliability gains and protects against regressions.
- Cost:
  - Low to medium.

### Option B - Consistency and platform behavior polish
- Scope:
  - Apply and document consistent "today source" policy across non-UI layers.
  - Standardize formatting/labels and error presentation across accounting flows.
- Benefit:
  - Better UX consistency and fewer edge-case support issues.
- Cost:
  - Low to medium.

## Next Fix Batch (Recommended Order)
1. Add integration-level backup restore tests with malformed and locale-formatted payload fixtures.
2. Add instrumentation test proving migration `9 -> 10` preserves `orders` rows after schema rebuild.
3. Document and enforce cross-layer "today source" policy.
4. Continue UI/accounting consistency polish.

## Test Scenarios To Add Next
- Backup restore integration with malformed numeric strings (`""`, `"NaN"`, `"1,200"`, `"1.200,50"`).
- Legacy backup import without `amountPaid` and with old `amountPaid` both succeed.
- Migration `9 -> 10` preserves all existing order rows and values.
- Worker/date logic smoke checks around day transitions.
