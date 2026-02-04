# Title: Calendar main screen - full UI/UX review and target plan

## Scope
This review covers the full Calendar main screen and its surrounding UI:
- Top app bar (month title, summary, today).
- Month summary card and status legend.
- Weekday header row.
- Month grid and day cells.
- Floating action button (Add order).
- Bottom navigation (AppScaffold).
- Quick add bottom sheet triggered from Calendar.

Primary references:
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt
- calendarscreen.jfif
- day detail problem.jfif

## Status update (Feb 2, 2026)
Implemented improvements:
- Day details open on a single tap (no second tap required).
- Visible previous/next month controls removed; month/year picker added on the title (tap month title to jump).
- Summary duplication removed from the month card; summary is a single top-bar entry.
- Bottom "Selected day" action bar removed to reduce stacked bottom UI.
- Customers icon removed from top bar (customers remain in bottom nav).
- "Due" chip is now defined and labeled as "Unpaid" (includes partial balances).
- "Unpaid" chip is actionable (opens summary).
- Unpaid days are marked directly on the grid with a badge.
- Weekday labels respect locale + week start.
- Day cells surface order count and conditional compact totals for busy days.
- Count • total is now a single line to reduce truncation on small cells.
- Selected day highlight softened (lighter fill + border) to avoid overpowering the grid.
- Today state is visually clearer (distinct background + ring).
- Quick add bottom sheet uses LazyColumn.
- Customer search is debounced to avoid typing jank.
- Inline hint moved to legend info to keep the grid dominant.
- Empty-month overlay removed to avoid covering the grid.
- Empty-month CTA now lives in the summary row ("No orders" + "Add order").
- FAB is now smaller and less dominant.
- Legend replaced with ? info icon that opens a bottom sheet.
- Today icon now shows the date inside the calendar icon.
- Crash fix: Lazy grid item key uses a Bundle-safe String.
- Responsive grid height: cell height adapts to available grid height and month row count (5 vs 6 rows).
- Removed exclamation badge from day cells.
- Status markers replaced with color-coded count • total line:
  - Unpaid: red
  - Paid: green
  - Partial: amber
  - Overpaid: indigo
- Day cell density reduced to show only day number + count • total.
- Unused vertical space in cells reduced by capping cell height to the 6-row baseline.

## Remaining UX issues (ordered by impact)

### 1) Primary action hierarchy
- Quick add is still taught via FAB/long-press; consider a contextual CTA on selected empty days that does not cover the grid.

### 2) Data states and feedback
- "Unpaid" explanation is only inside the legend sheet; consider a lightweight persistent info affordance.

### 3) Performance and polish
- Day cells still use multiple Surface layers; could be simplified for low-end devices.

## Acceptance criteria (definition of done)
- The calendar grid occupies at least ~3/4 of the vertical space on a standard phone.
- No empty-state UI overlays the grid or blocks day cells.
- Selected day highlight does not overpower the grid; day cells remain evenly readable.
- The count • total line is readable without truncation in 6-row months.
- A user can open day details in one obvious action with no ambiguity.
- Month navigation includes both swipe and direct month/year selection.
- Status meaning is understandable without color alone.
- Smooth scrolling and typing on low-end devices; no visible jank.

## Notes
- This issue now reflects the fix for unused vertical space (cell height capped to 6-row baseline).
