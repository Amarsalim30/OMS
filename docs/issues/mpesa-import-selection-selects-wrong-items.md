# Title: Mpesa import selection selects wrong items + modernize intake UX

## Context
Screen: MpesaImportScreen
Files (expected):
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt
  - Composable: MpesaImportScreen(...)
  - Current row UI: MpesaTransactionCard(...) (to be replaced)
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeViewModel.kt
  - Logic: buildUi(...), selectReadyOnly(), setAllSelected(...), setSelected(...),
    selectOrder(...), selectAllocationMode(...), applySelected()
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt (new)
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaAllocationSheet.kt (new)
Related:
- docs/ui-behavior.md (Mpesa import section)
- docs/requirements.md

## Current behavior (facts)
- Tapping "Select ready" also selects duplicate items.
- Items without customer/order match get selected.
- UI is noisy (chips/expands) and slower to scan.
- TopAppBar uses too much vertical space due to insets/padding.

## Target UX (high-level)
- Modern Google-level: fast, minimal, predictable.
- List should scan like M-PESA / KCB style: one-row hierarchy per payment.
- Tap a payment row opens a sleek bottom sheet for details/options (no in-card expand UI).
- Bottom bar is a single-row: total KES + Apply.

## Expected behavior (rules)
### 1) Auto allocation + preselect (default behavior after paste)
For each imported transaction with duplicateState == NONE:
- If customer is confidently matched and customer has unpaid orders:
  - Auto-allocate to OLDEST unpaid order(s) (no allocation details shown by default)
  - Preselect (checked)
- If customer matched but customer has NO unpaid orders:
  - Default to Customer Credit (carry-forward)
  - Preselect (checked)
- If customer is NOT matched:
  - Not selected
  - Status shows "Needs match"

Implementation note (current data model):
- MpesaTransactionUi has duplicateState, selectedCustomerId, selectedOrderId,
  allocationMode, selected.
- canApply() currently returns selectedCustomerId != null || selectedOrderId != null.
- Auto-allocation must set these fields so canApply() is true only when the rules above are met.

### 2) Bulk selection rules
- "Select ready" selects ONLY items where:
  - duplicateState == NONE
  - canApply() == true
- Duplicates must never be selected by bulk actions.
- Duplicates must not be selectable by checkbox.

Implementation note:
- This is PaymentIntakeViewModel.selectReadyOnly() and setAllSelected(true).

### 3) Bottom sheet behavior
- Tapping a payment row opens a bottom sheet:
  - Shows amount, code, time, selected customer (if any).
  - Customer quick edit:
    - Tapping the selected customer opens an inline input to replace the match.
    - Input supports search by name or phone.
    - If no customer match exists, show search input by default with placeholder.
  - Shows current allocation mode (oldest / credit / specific order).
  - Allows changing allocation mode or picking an order.
  - Allows viewing existing receipt if duplicate (EXISTING).
  - Allows moving existing receipt only if user assigns a target.
- Closing sheet returns to list without losing scroll/selection.

### 4) UI layout rules
- TopAppBar should not waste space above content:
  - Use proper WindowInsets handling; avoid double padding.
- Message input area:
  - Paste / Clear / Select All (ready) available, minimal footprint.
- List rows:
  - One primary line: "KES amount • code".
  - Secondary: "From sender • time".
  - Right side: status chip or small label + checkbox (disabled if duplicate).
- Avoid chips as primary navigation; filtering can be tucked into a compact control or bottom sheet.

## Steps to reproduce
1. Paste messages containing:
   - 1 duplicate transaction
   - 1 unmatched transaction
   - 1 matched transaction with unpaid orders
   - 1 matched transaction with no unpaid orders
2. Observe default selection + allocation mode.
3. Tap "Select ready" and ensure only eligible items become selected.
4. Tap any row to open bottom sheet and change allocation.

## Acceptance criteria (DoD for this issue)
- Default selection and auto allocation follow rules above.
- "Select ready" selects only eligible items; duplicates never selected.
- UI uses row + bottom sheet pattern; no expand details inside row.
- Bottom bar is a single row (Total KES + Apply).
- ./gradlew :app:assembleDebug passes.
- No new lint warnings.
- Add unit tests for selection filtering + default allocation decisions.

## Notes / screenshots
- (paste screenshots or sample messages here)
