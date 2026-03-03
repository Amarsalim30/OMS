# Tutorial, Launcher Quick Actions, and Widget Revamp (2026-02-22)

## Objective
Improve non-technical user efficiency by:
1. Expanding tutorial coverage to include helper and notes workflows.
2. Reworking launcher quick actions for practical daily use.
3. Refining the home widget for faster action and cleaner presentation.

## Issue 1: Tutorial Coverage Was Too Narrow
### Problem
- The tutorial entry from More was opening calendar-only interactive walkthrough.
- It did not guide users through `Notes history` and `Floating helper`, which are now core daily tools.

### Implementation
- More menu tutorial action now opens the broader beginner tutorial screen (`AppRoutes.Tutorial`).
- Beginner tutorial expanded from 5 steps to 7 steps.
- Added dedicated tutorial actions for:
  - Notes history
  - Floating helper settings
- Navigation chrome is hidden on tutorial route for focused onboarding flow.

### Acceptance Criteria
- From More, tapping tutorial opens the multi-screen tutorial (not calendar-only tutorial).
- Tutorial includes direct navigation actions for Calendar, Orders, Customers, Money, Notes history, Floating helper, Backup, and Notifications.

## Issue 2: Launcher Quick Actions Were Outdated
### Problem
- Launcher shortcuts used `Today`, which is less useful than fast capture and retrieval workflows.

### Implementation
- Replaced `Today` shortcut with `Voice note` and added `Notes history` shortcut.
- Current dynamic launcher quick actions:
  - New order
  - Voice note
  - Notes history
  - Unpaid
- Added new app intent actions:
  - `ACTION_SHOW_NOTES_HISTORY`
  - `ACTION_CAPTURE_VOICE_NOTE`
  - `ACTION_CAPTURE_VOICE_CALCULATOR`
- Added routing in `MainAppContent` for these intents.
- Voice-note quick action opens `HelperCaptureActivity` directly for reliable capture flow.

### Acceptance Criteria
- Long-press app icon shows updated quick actions.
- Tapping `Voice note` opens voice capture.
- Tapping `Notes history` opens notes history screen.
- Existing `New order` and `Unpaid` actions continue to work.

## Issue 3: Widget Needed Better Utility and Sleeker UI
### Problem
- Widget showed basic counts only, with limited action affordance.
- Visual hierarchy and action density were not optimized for quick daily use.

### Implementation
- Widget UI refinements:
  - Improved card/chip/button styling (clean neutral palette, better contrast).
  - Added `Today total` line.
  - Kept concise order preview lines.
- Widget action row now includes:
  - Voice note
  - Unpaid
  - New
- Widget data improvements:
  - Shows Today count.
  - Shows Unpaid count with unpaid amount.
  - Shows Today total amount.
- Added widget intent wiring for voice-note action (`ACTION_CAPTURE_VOICE_NOTE`).

### Acceptance Criteria
- Widget renders updated style and stat lines.
- `Voice note`, `Unpaid`, and `New` widget actions navigate/launch correctly.
- Widget updates continue to run through `WidgetUpdater` without crashes.

## Issue 4: Interactive Tutorial Hidden Behind Modal Sheet
### Problem
- During calendar interactive tutorial, once quick-add `ModalBottomSheet` opened, the spotlight/tutorial could be visually hidden by the sheet.
- The quick-add sheet height also felt too constrained on smaller screens during guided steps.
- Tutorial copy needed clearer practical guidance for:
  - where `Notes history` lives
  - how to share M-PESA SMS directly into `Money > Collect`

### Implementation
- Kept spotlight overlay only for pre-sheet steps (calendar date and `+` action).
- Added in-sheet tutorial panel for sheet-focused steps (`Customer`, `Notes`, `Total`, `Save`) so guidance remains visible while editing.
- Increased quick-add sheet usable height (`0.90f -> 0.94f`, and `0.96f` while tutorial panel is present).
- Refined beginner tutorial copy:
  - Added a "Quick map" card covering tabs + More destinations.
  - Explicitly documented `More -> Notes history`.
  - Explicitly documented M-PESA share path from Messages to OrderBook.

### Acceptance Criteria
- Interactive tutorial guidance remains visible during quick-add modal steps.
- Calendar tutorial does not lose progression when opening/closing quick-add.
- Beginner tutorial clearly explains where Notes history is and how to share M-PESA messages into Money collect.

## Issue 5: Tutorial Needed Cross-Screen Coach Flow (Not Calendar-Only)
### Problem
- First-run flow stopped after the 6-step calendar walkthrough.
- The broader tutorial route existed, but it was static cards and not an interactive coaching flow.
- Users needed guided progression across major screens, similar to practical app onboarding coaches.

### Implementation
- Added a post-calendar tutorial route: `AppRoutes.TutorialAfterCalendar`.
- Calendar interactive tutorial now routes into the cross-screen coach after completion.
- Rebuilt `BeginnerTutorialScreen` into an interactive coach:
  - one focused step at a time
  - step progress indicator
  - primary action per step to open the relevant feature screen
  - previous / next / skip / finish controls
  - initial step support (`initialStep`) so first-run can start from step 2 after calendar training
- Added robust tutorial exit behavior:
  - back/finish pops stack when possible
  - falls back to Calendar route when tutorial is root.
- Navigation chrome remains hidden on tutorial routes for focus.

### Acceptance Criteria
- First-run users complete calendar step coaching, then continue into cross-screen tutorial coach.
- Tutorial coach covers Calendar, Orders, Customers, Money, Notes history, Floating helper, and Backup/Notifications.
- Tutorial opened from More starts from step 1.
- Tutorial opened after calendar starts from next step (not repeating calendar step as default).
- Back/finish from coach always exits safely without dead-end routes.

## Permissions and Safety
- No new dangerous permissions were introduced.
- Existing microphone flow remains runtime-permission safe through `HelperCaptureActivity`.

## Files Updated
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppIntents.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppShortcuts.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/tutorial/BeginnerTutorialScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppRoutes.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CalendarGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/widget/WidgetUpdater.kt`
- `app/src/main/res/layout/widget_today_unpaid.xml`
- `app/src/main/res/drawable/widget_background.xml`
- `app/src/main/res/drawable/widget_button_background.xml`
- `app/src/main/res/drawable/widget_chip_background.xml`
- `app/src/main/res/values/strings.xml`

## Validation Run
- `./gradlew :app:assembleDebug` -> passed
- `./gradlew :app:testDebugUnitTest` -> passed
