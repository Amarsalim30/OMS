# Calendar Screen UI/UX Issues

Status: Implemented (Option A + layout polish from 001cal.jfif)

## Scope
- Route: `calendar`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt`

## Current review focus
- Calendar grid is acceptable and should stay unchanged.
- Primary problem was two stacked summary cards above the grid creating visual heaviness and pushing core content down.
- Screenshot-based review (`001cal.jfif`) confirmed the top summary block still felt too heavy for compact devices.

## Implemented fix (2026-02-17)

### P1
- Implemented Option A: merged owner context + month summary into a single top summary card.
- Removed separate owner header card from `CalendarScreen`.
- Extended `MonthSummaryCard` to include owner title/subtitle inline.
- Preserved existing month total, unpaid count, add-order action, and legend action.

### P1 (layout polish from screenshot review)
- Reduced top summary card vertical density (smaller outer spacing and tighter internal spacing).
- Made owner subtitle compact (single-line with ellipsis) to prevent the header from consuming excess height.
- Added subtle divider between owner context and financial metrics for clearer hierarchy.
- Increased month total visual priority and reduced weekday-row top padding to surface more calendar grid content.

Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreenSections.kt`

## Acceptance criteria
- [x] Only one dominant summary card appears above the calendar grid.
- [x] Top area height is reduced so more grid rows are visible on first load.
- [x] No loss of existing month total/unpaid visibility or quick add workflow.
- [x] Summary card hierarchy is clearer and less visually heavy on compact screens.

## Shared Order Editor UX Pass (2026-02-18)

Because Calendar Quick Add uses the shared `OrderEditorSheet`, this screen inherits the same editor layout updates made for Day Detail:

- duplicate close actions removed (single top `X`),
- compact footer actions (`Clear` + `Save`),
- removal of `Save + Next` button and workflow path from the shared editor,
- flow order tuned for faster entry (`Customer -> Pickup -> Notes -> Total`),
- quick pickup chips and additive amount chips added for fewer taps,
- mode-aware initial focus (new starts at customer, edit starts at notes),
- sticky footer total summary added for clearer save context.
