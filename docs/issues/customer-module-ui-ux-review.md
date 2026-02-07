# Title: Customer module UI/UX review (single-file issue list)

## Scope
Customer module screens and flows:
- Customer list (filters, search, sort, actions)
- Customer detail (orders, ledger, filters)
- Import contacts

Primary files:
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt

## High-impact issues (keep these)

### 1) Replace hard delete with archive
- Hard delete breaks historical records and audit trails.
- Expected behavior:
  - If customer has transactions ? Archive (hide from list, reversible) with confirm.
  - If customer has zero transactions ? Delete allowed with confirm.
  - Provide Undo for archive where possible.

### 2) Default view should not hide customers unintentionally
- Small businesses often use Customers to quickly find anyone for a new order.
- Best default: **All** with **Hide zero balances = ON** (toggle).
- Add a toggle “Show inactive (no orders)” for visibility when needed.

### 3) Rename chips for non-technical clarity
- Due ? **Owes**
- Extra ? **Credit**
- Clear ? **Settled**
- These labels map directly to real-world language.

### 4) Normalize phone numbers + WhatsApp links
- Normalize phone formats (`07…`, `+2547…`, `2547…`, spaces, dashes) into one canonical form.
- WhatsApp links must always use E.164 (default +254 unless configured).
- Deduplicate customers on import/creation by normalized phone.

### 5) Make actions discoverable without long-press
- Relying on long-press hides critical actions.
- Expected: row tap ? details, plus a visible trailing menu (or swipe actions) for “Add payment”, “New order”, “Message”, “Archive/Delete”.

## Medium-impact issues

### 6) Remove sort duplication
- Top-bar “More” sort duplicates the inline sort icon.
- Expected: single, obvious sort control (inline is fine).

### 7) Import contacts: select-all respects filters
- Select-all should apply to visible/filtered contacts.
- Label should clarify scope (“Select all (visible)” when filtered).

### 8) Import action visibility
- “Import (n)” should be a clear primary action (bottom bar or primary button).
- Disabled until n>0.

## Performance / polish

### 9) Ledger search performance
- Add debounce and move heavy filtering off the main thread for large ledgers.

### 10) Action sheet hierarchy
- Separate destructive actions visually (red text + divider).
- Primary actions appear first (Add payment, New order, Message).

## Acceptance criteria
- Delete only allowed for customers with zero transactions; otherwise archive.
- Default list = All with Hide zero balances ON (toggle available).
- Chips read Owes / Credit / Settled.
- Phone normalization is consistent; WhatsApp uses E.164.
- Dedupe by normalized phone on import and creation.
- All critical actions discoverable without long-press.
- Select-all respects filters and clarifies scope.
- Import action is prominent and disabled until items selected.
- Ledger search remains smooth under load.
