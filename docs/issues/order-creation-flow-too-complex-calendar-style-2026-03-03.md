# Title: Creating order is too complex; simplify to Samsung Calendar style

## Date
2026-03-03

## Reported problem
Client feedback: creating an order in the current app feels heavy and complex compared to the old workflow in Samsung Calendar.

Previous mental model:
- Title/notes first
- Optional customer bookmark/tag
- Save quickly

Current app pain points:
- Add order uses a dense bottom sheet with many visible inputs/options at once.
- Too much setup before save for common cases.
- When customer is empty, UI falls back to labels like `Walk-in` / `No customer`, which is not desired for this workflow.

Reference UX direction: Samsung Calendar "Add Event" full-screen editor (simple list-like form, minimal visible fields, top-level `Cancel` / `Save` actions).

## Goal
Make order creation feel lightweight and familiar:
- Use a full screen editor (not bottom sheet).
- Start with notes as the primary first field.
- Keep inputs minimal by default.
- Do not auto-label blank customer orders as `Walk-in`; keep customer display blank and let notes lead.

## Scope (implementation targets)
- Replace bottom-sheet entry UI with a dedicated Add/Edit Order screen flow:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailDialogs.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`
- Update blank-customer display behavior:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/SummaryScreen.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersSections.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`
  - `app/src/main/res/values/strings.xml` (`summary_walk_in`, `unpaid_customer_walk_in`, `day_no_customer` usage in order rows)

## Proposed UX changes
1. `Add order` opens a full screen editor with top app bar actions: `Cancel` (left) and `Save` (right).
2. Primary field order:
   - `Notes` first (focus on open).
   - `Customer` optional.
   - `Total amount` required.
   - `Pickup time` optional, visually secondary.
3. Reduce visible complexity:
   - Remove heavy section card framing.
   - Keep only core fields visible by default.
   - Keep advanced helpers (quick amounts/time suggestions) subtle or collapsible.
4. Blank customer behavior:
   - If no customer selected/typed, do not render `Walk-in`/`No customer` in order rows.
   - Order row should lead with notes, with customer line omitted when empty.
5. Keep draft safety:
   - Back/cancel on dirty form should still protect against accidental loss.

## Additional improvements to include
- Keep validation concise and inline (avoid multiple stacked warnings).
- Ensure keyboard flow matches fast entry (`Notes -> Total -> Save` as the common path).
- Preserve current accounting/data behavior; this is a UX simplification, not a ledger logic change.

## Acceptance criteria
- From Calendar and Day Detail, tapping add/edit order opens a dedicated screen, not a modal bottom sheet.
- Common case can be completed with only notes + total, then save.
- Empty customer does not display fallback identity labels in day list, summary, or unpaid list.
- On saved orders without customer, notes remain the primary visible identity.
- Save/cancel behavior remains safe with draft/unsaved-change handling.
- Existing save logic, customer resolution, and accounting reconciliation still behave exactly as before.

## Verification checklist (manual)
1. Open Calendar, tap `+`, confirm full-screen Add Order appears.
2. Enter only notes + total, save, confirm order is created correctly.
3. Create order with blank customer, verify no `Walk-in`/`No customer` label appears in:
   - Day Detail order list
   - Summary orders list
   - Unpaid list
4. Edit existing order from Day Detail, confirm same full-screen editor is used.
5. Start typing then back/cancel, confirm unsaved-change protection still works.
6. Create order with customer selected, verify customer still displays normally and accounting links remain intact.

## Priority
High (daily workflow friction during core task: order creation).
