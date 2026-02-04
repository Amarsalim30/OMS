# Title: Calendar weekday alignment mismatch (date does not match correct weekday)

## Context
The calendar grid should align each date under the correct weekday (e.g., Monday dates should appear under Mon).

Related files:
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt

## Observed behavior
- Dates appear under the wrong weekday header (example: a Monday date shows under a different day like Tuesday/Wednesday).
- This makes the calendar unreliable for planning and scanning.

## Expected behavior
- Each date should appear under the correct weekday according to the locale’s first day of week.

## Suspected causes
- Weekday header order uses locale-specific first day of week.
- Month grid generation uses a fixed offset based on `start.dayOfWeek.ordinal` (Kotlinx datetime), which may not align with the locale’s first day of week.
- Result: headers and grid use different “week start” assumptions.

## Steps to reproduce
1. Open Calendar screen.
2. Compare day-of-week for a known date (e.g., check a phone calendar or Google Calendar).
3. Observe that the date is placed under the wrong weekday header.

## Acceptance criteria
- Calendar grid dates align correctly with weekday headers for all locales.
- Changing locale/first-day-of-week does not misalign the grid.

## Notes
- Align the grid’s leadingDays calculation with the same week-start logic used in `WeekdayHeaderRow()`.
- Consider using a shared helper for week-start indexing to avoid divergence.
