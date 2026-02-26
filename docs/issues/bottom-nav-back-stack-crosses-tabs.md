# Title: Bottom nav back stack traverses across tabs (confusing Back behavior)

Status: Fixed (2026-02-26)

## Context
Screen: Bottom navigation (AppScaffold)
Files:
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt
  - navigateTopLevel(...)
  - onDestinationSelected in AppScaffold
  - direct navController.navigate(...) calls to top-level routes
Related:
- docs/requirements.md (Navigation and global UX)

## Current behavior (facts)
- Repeatedly switching tabs (Calendar → Money → Customers → Orders) builds a back stack.
- Pressing Back from a top-level screen can return to a different tab or a deep screen from another tab.
- This creates “confused” navigation because Back does not consistently exit or stay within the current tab.

## Expected behavior (rules)
- Bottom-nav tab switches should not add cross-tab entries to the back stack.
- Back should:
  - Pop within the current tab’s stack if there is in-tab history, otherwise
  - Exit the app from the tab root.
- Switching tabs should reset to each tab’s root unless explicitly requested.

## Steps to reproduce
1. Open Calendar, then open a day detail screen.
2. Switch to Money tab.
3. Switch to Customers tab.
4. Press Back multiple times.

## Acceptance criteria (DoD for this issue)
- Back does not traverse across tab roots once a new tab is selected.
- Back from a tab root exits the app.
- ./gradlew :app:assembleDebug passes.

## Implementation notes (2026-02-26)
- Top-level Money navigation now consistently uses `navigateTopLevel(..., resetToRoot = true)` from:
  - share-intent handoff
  - in-app "record payment" jumps from other modules
- This keeps tab switches rooted and prevents cross-tab history from leaking into Back.

## Notes / screenshots
- (paste screenshots or sample navigation history here)
