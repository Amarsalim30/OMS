# Title: Bottom nav Calendar does not return to Calendar root from Money

## Context
Screen: Bottom navigation (AppScaffold)
Files:
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt
  - onDestinationSelected in AppScaffold
  - navigateTopLevel(...)
  - topLevelRouteFor(...)
- app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt
Related:
- docs/requirements.md (Navigation and global UX)

## Current behavior (facts)
- If the last Calendar state is a nested screen (DayDetail or Summary), tapping Calendar from Money restores that nested screen.
- This is perceived as the Calendar tab “not working” because the month grid does not appear.

## Expected behavior (rules)
- Tapping Calendar in bottom nav always navigates to the Calendar root screen (month grid).
- Deep Calendar routes (day/{date}, summary) should only appear when explicitly opened, not when switching tabs.
- If currently on a Calendar sub-route, tapping Calendar should pop to Calendar root.

## Steps to reproduce
1. Open Calendar.
2. Open a day detail (day/{date}) or Summary.
3. Switch to Money tab.
4. Tap Calendar tab.

## Acceptance criteria (DoD for this issue)
- Calendar tab always shows CalendarScreen root after a tab switch.
- Re-tapping Calendar does not add new back stack entries.
- ./gradlew :app:assembleDebug passes.

## Notes / screenshots
- (paste screenshots or sample navigation history here)
