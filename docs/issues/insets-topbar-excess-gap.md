# Title: Excess top inset / double padding between status bar, top bar, and first content row

## Context
Screens:
- Calendar (home)
- Money (tabs)
- Summary
- Customer list/detail
- Manual payment / M-PESA intake / Ledger
Files:
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt
  - enableEdgeToEdge() and top-level AppScaffold/Scaffold usage
- app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt
  - MoneyScreen(), TabRow wrapped in statusBarsPadding()
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt
  - CalendarScreen(), CalendarTopAppBar(), MonthSummaryCard()
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/SummaryScreen.kt
  - SummaryScreen(), CenterAlignedTopAppBar()
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/core/notifications/NotificationSettingsScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt

## Current behavior (facts)
- On devices with system bars visible, there is a large empty gap between the phone status bar (insets) and the app top bar.
- The Home screen (Calendar) shows too much vertical space before the first content card (MonthSummaryCard), making the UI feel pushed down.
- Money screen top bar (TabRow) is visibly lower than other top bars (extra top padding).

Code signals that can cause this:
- MainActivity enables edge-to-edge: enableEdgeToEdge().
- MoneyScreen wraps its TabRow with statusBarsPadding() even though it is already inside a Scaffold topBar.
- Many screens use CenterAlignedTopAppBar/TopAppBar defaults (which apply WindowInsets internally), while Scaffold’s content padding may also include system bar insets.
- Result: insets can stack (status bar padding + top bar insets + Scaffold content insets), producing a double top offset.

## Expected behavior (rules)
- Top bars should sit directly below the status bar (no extra blank gap).
- First content row should start immediately below the top bar (standard 8–12dp spacing, not “double” insets).
- Insets should be applied exactly once in a consistent way across the app.

## Steps to reproduce
1. Open the app to Home (Calendar).
2. Observe the gap above the top bar and the large space between the top bar and MonthSummaryCard.
3. Switch to Money tab.
4. Observe the TabRow sitting lower than other top bars.

## Acceptance criteria (DoD for this issue)
- Remove double inset behavior so status bar -> top bar spacing is tight and consistent.
- Home/Calendar first card starts directly under the top bar (no extra blank gap).
- Money tab top bar aligns with other screens (no extra statusBarsPadding stacking).
- Verify on gesture navigation + 3-button navigation devices/emulators.
- No content clipped under status bar; no regressions to bottom nav spacing.

## Notes
- Likely fix is to standardize insets:
  - Either set Scaffold contentWindowInsets to WindowInsets(0) and manually apply insets to top bars, or
  - Rely on TopAppBarDefaults.windowInsets and avoid manual statusBarsPadding()/extra top padding on top bars.
- Start with MoneyScreen: remove statusBarsPadding() from TabRow and verify visual alignment with other top bars.
