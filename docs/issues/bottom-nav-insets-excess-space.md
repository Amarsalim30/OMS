# Title: Bottom inset stacking pushes bottom bars up (varies by screen)

## Context
Screens with visible bottom spacing issues:
- Calendar (Home): BottomActionBar (“Selected day” row) sits too high above the system nav area.
- Money > M‑PESA: ApplyReadyBar (“Total KES + Apply”) sits too high.
- Money > Manual payment: Save payment bar sits too high.

Files (current implementation):
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt
  - bottomBar = BottomActionBar(..., modifier = Modifier.navigationBarsPadding())
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt
  - ApplyReadyBar() uses Row(...).navigationBarsPadding()
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt
  - bottomBar Row(...).navigationBarsPadding()
- app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt
  - BottomAppBar + NavigationBar (app‑wide bottom nav)

Related config:
- Many screens now use Scaffold(contentWindowInsets = WindowInsets(0)) to avoid top double‑insets.

## Current behavior (facts)
- Bottom action bars are pushed up with too much empty space below them.
- The amount of extra space differs per screen (Calendar vs M‑PESA vs Manual payment).
- On gesture navigation devices, the bottom nav + bottom bars feel “double padded.”

## Likely cause (based on code)
- Bottom nav (AppScaffold) already reserves space for system nav + gestures.
- Per‑screen bottom bars ALSO add navigationBarsPadding(), stacking the inset.
- Some screens may also apply extra padding from Scaffold content or from bottomBar modifiers, causing inconsistent heights.

## Expected behavior (rules)
- Bottom action bars should sit close to the bottom nav/system bar with minimal gap.
- Spacing should be consistent across screens (Calendar, M‑PESA, Manual payment).
- Insets should be applied exactly once.

## Steps to reproduce
1. Open Calendar (Home) → observe the “Selected day” bar sits too high.
2. Open Money > M‑PESA → observe “Total KES + Apply” bar sits too high.
3. Open Money > Manual payment → observe “Save payment” bar sits too high.
4. Compare spacing between screens — the gap differs.

## Acceptance criteria (DoD for this issue)
- Calendar BottomActionBar sits just above the system nav area (no tall gap).
- M‑PESA ApplyReadyBar sits just above the system nav area.
- Manual payment bottom bar sits just above the system nav area.
- Spacing is consistent across the three screens.
- Verified on gesture navigation + 3‑button navigation.

## Notes / implementation guidance
- Decide a single inset owner:
  - Option A: AppScaffold handles bottom insets. Remove navigationBarsPadding() from screen bars.
  - Option B: Screen bars handle bottom insets. Disable nav‑bar insets in AppScaffold bottom nav.
- Avoid mixing both or padding will stack and vary per screen.
