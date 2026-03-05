# Title: Order Create/Edit should use one Notes field with customer typeahead chip

## Date
2026-03-03

## Problem
Customer input in Order Create/Edit is underused and adds extra form weight.

Current friction:
- User types order details in `Title/Notes`.
- Customer is a separate field/row, which feels disconnected and slows entry.
- Fast entry flow is not sleek enough for daily usage.

## Goal
Use a single, fast input pattern:
- User types in `Title/Notes`.
- While typing, customer suggestions appear as typeahead popup above keyboard/field.
- On suggestion tap, selected customer appears as a **blue chip** with remove `X`.
- User continues writing notes seamlessly after chip selection.
- No `@` mention syntax is required.

## UX spec (target behavior)

### 1) Single-field primary flow
- Keep one primary `Title/Notes` editor as the first interaction.
- Remove dedicated visible `Customer` row/input from the default form layout.
- Customer selection is attached to notes flow, not a separate section.

### 2) Customer typeahead trigger
- Typeahead runs from the same notes input text.
- Matching uses customer name and phone.
- Suggestions render in a compact popup anchored near the input (or just above IME), max 5 items.
- Popup hides when:
  - user dismisses it,
  - a customer chip is selected,
  - no matches exist.

### 3) Selected customer chip
- Selected customer shows as a blue chip directly under/inside notes context area.
- Chip content: customer name (and optional compact phone).
- Chip has trailing `X` to clear selection.
- Clearing chip returns form to anonymous order mode.

### 4) Notes continuity
- After selecting customer chip, keyboard stays active and cursor remains in notes editor.
- User can continue writing notes without switching fields.
- Editing existing order preloads chip when order has customer.

### 5) Save semantics
- Save writes:
  - `notes` from the notes field text,
  - `customerName/customerPhone` from selected chip (if any),
  - no chip means `customerId = null`.
- Existing ledger/order save behavior must remain unchanged.

## Non-goals
- No `@mention` parser/token model.
- No multi-customer chips.
- No change to payment allocation/accounting logic.

## Implementation targets
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailDialogs.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt`
- `app/src/main/res/values/strings.xml`

## Acceptance criteria
1. Create/Edit order screen has no separate customer input row in default UI.
2. Typing in notes shows customer suggestions (name/phone match) in popup.
3. Tapping a suggestion creates a blue removable chip.
4. After chip select, user continues typing notes immediately.
5. Removing chip clears customer association cleanly.
6. Editing existing order with customer pre-renders chip and allows remove/replace.
7. Save still works for:
   - notes + customer chip
   - notes without chip (anonymous order)

## Manual verification checklist
1. Open Create Order, type partial customer name in notes, verify popup appears.
2. Tap suggestion, confirm blue chip appears with `X`, keyboard stays open.
3. Continue typing notes after chip selection; save; verify customer linked.
4. Remove chip with `X`; save; verify order has no customer.
5. Edit order with existing customer; chip is preloaded; replace/remove works.
6. Confirm no regressions in quick save, validation, and draft behavior.

## Priority
High (core data-entry speed and daily usability).
