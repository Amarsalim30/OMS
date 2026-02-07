# OMS App Requirements (Current Behavior)

This document captures the current logic, UI, and UX of the Order Management System (OMS) Android app as implemented in the codebase. It is a descriptive baseline of what exists today.

## 1. Product scope and goals
- Manage bakery orders by day and month.
- Track customers, balances, and payment history.
- Record payments via M-PESA import or manual entry.
- Maintain a Customer ledger of charges, payments, reversals, and write-offs.
- Provide reminders, backups, and quick access (shortcuts and widget).

## 2. Tech stack and architecture
- Android app, Kotlin, Jetpack Compose (Material 3).
- Single-activity architecture with Compose Navigation.
- Room database (offline-first). 
- WorkManager for background jobs (backup, notifications, widget updates).

## 3. Navigation and global UX
### 3.1 Top-level navigation
- Bottom navigation on compact width, navigation rail on medium and above.
- Top-level tabs:
  - Calendar
  - Orders (unpaid view)
  - Customers
  - Money
- "More" bottom sheet actions:
  - Order oNotes Summary
  - customer Ledgers
  - Payment history (all)
  - Backup and restore
  - Notifications
  - Import contacts

### 3.2 Routes and deep links
- Calendar: `calendar`
- Day detail: `day/{date}`
- Orders (unpaid list): `orders`
- Customers list: `customers`
- Customer detail: `customer/{customerId}`
- Money: `money`
- Payment history: `payment_history/all`, `payment_history/customer/{customerId}`, `payment_history/order/{orderId}`
- Summary: `summary`
- Backup: `backup`
- Notifications: `notifications`
- Import contacts: `import_contacts`

### 3.3 Intents and shortcuts
- Share intent (text/plain): when receiving shared text, append to M-PESA intake if already on Money/M-PESA; otherwise switch to Money/M-PESA and prefill with shared text.
- App shortcuts:
  - New order
  - Today
  - Unpaid
- Widget intents:
  - Show today
  - Show unpaid
  - New order

### 3.4 Global overlays and dialogs
- Voice calculator overlay (floating mic) available on most screens.
- Update dialog ("What's new") shown on Calendar when app version changes.
- Credit prompt dialog after creating a new order when the customer has available credit.

## 4. Data model (Room database)
### 4.1 Tables (entities)
- `orders`:
  - id, orderDate, createdAt, updatedAt
  - notes, pickupTime
  - status (PENDING/CONFIRMED/COMPLETED/CANCELLED)
  - statusOverride (OPEN/CLOSED/NULL)
  - totalAmount, amountPaid, customerId
- `order_items`:
  - orderId, name, category, quantity, unitPrice
- `customers`:
  - id, name, phone
- `account_entries`:
  - type: DEBIT, CREDIT, WRITE_OFF, REVERSAL
  - amount, date, description
  - optional orderId, customerId
- `payments` (legacy):
  - orderId, amount, method, paidAt
- `payment_receipts`:
  - amount, receivedAt, method (MPESA or CASH)
  - transactionCode, hash, senderName, senderPhone, rawText
  - customerId, note
  - status: UNAPPLIED, PARTIAL, APPLIED, VOIDED
- `payment_allocations`:
  - receiptId, orderId, customerId, amount
  - type: ORDER or CUSTOMER_CREDIT
  - status: APPLIED or VOIDED
  - optional accountEntryId and reversalEntryId

### 4.2 Core accounting rules
- Order creation or update inserts a DEBIT entry for the order total.
- Credits (payments) and write-offs reduce the outstanding balance.
- Reversals are created when payments are voided or reallocated.
- If order total is reduced below payments, excess CREDIT is moved to customer-level credit.

## 5. Modules and screens

## 5.1 Orders module
### 5.1.1 Calendar (top-level)
**UI layout**
- Top app bar: month label, Summary icon (left), Today button, Customers button.
- Month summary card: month total and unpaid count badge.
- Payment status legend (unpaid, partial, paid, overpaid).
- Weekday row.
- Monthly grid with swipeable months (horizontal pager).
- Floating action button to add an order.
- Bottom action bar shows selected day and a "View day" button.

**UX behaviors**
- Tap a day to select it.
- Tap selected day with orders to open day detail.
- Long-press a day to open Quick Add for that date.
- Swipe horizontally to change months; month totals use cached snapshots.

**Logic**
- Calendar cell shows up to three payment-state dots. If more, shows "+N".
- Month totals and badge counts are calculated from orders and paid amounts.
- Quick add uses customer search suggestions by name or phone.

### 5.1.2 Quick Add Order (Calendar bottom sheet)
**UI**
- Modal bottom sheet covering ~90% height.
- Fields: notes (required), total (required), pickup time, customer name, customer phone.
- Customer suggestions list.
- Actions: Cancel, Clear, Save.

**Validation**
- Notes required.
- Total must be numeric and > 0.
- Customer name requires phone if name is entered.
- Pickup time must parse to HH:MM.

**Voice**
- Voice overlay enabled for notes and total input.

### 5.1.3 Day Detail
**UI layout**
- Top app bar: weekday, date; back.
- Summary card: total, paid, balance, counts by payment status.
- Filter chips: All, Unpaid, Partial, Paid, Overpaid.
- Search field (notes or customer).
- List of orders with notes, total, customer label, payment status pill, balance detail.
- Per-order actions: History, Record payment; Delete icon.
- FAB to add a new order.

**UX behaviors**
- Tap order to edit in the bottom sheet.
- Search and filter update list in place.
- Empty state adapts to filter/search.

**Logic**
- Payment state derived from paid totals vs order total.
- Drafts per day are stored in-memory so partially filled forms persist when navigating.

### 5.1.4 Order editor (Day Detail bottom sheet)
**UI**
- Modal sheet with fields like Quick Add:
  - Notes, Total, Pickup time
  - Customer name and phone
- Customer suggestions.
- Save, Clear, Cancel.

**Logic**
- Editing an order preloads customer data when possible.
- Saving updates order, ledger entries, and reconciles credits.

### 5.1.5 Delete order with payments
**UI**
- Confirmation dialog when deleting orders with allocations.
- Allows moving allocations or voiding them.
- Move targets:
  - Specific order
  - Oldest orders
  - Customer credit (only if order has customer)
- Option to move full receipts.

**Logic**
- Move: reallocate receipt allocations and adjust ledger.
- Void: create reversals and mark allocations voided.
- Order is then marked CANCELLED and order-level debit/write-off entries removed.

### 5.1.6 Orders (Unpaid list)
**UI**
- Summary card with count and total outstanding.
- Grouped list by date.
- Each order row shows notes, total, customer, paid and due amounts.
- "Record payment" button on each row.

**UX**
- Tap row opens day detail for that date.

### 5.1.7 Summary
**UI**
- Month total card.
- "Chef prep" card with:
  - Day/Week/Month tabs
  - Date picker, prev/next, Today
  - Order count and total for range
- Product aggregation list (from notes parsing).
- Unparsed notes block.
- Daily view section for week/month mode.
- Orders list grouped by date (week/month) or flat (day).

**UX**
- Copy chef list to clipboard.
- Copy individual order notes.
- Open ledger, notifications, and backup from top actions.

**Logic**
- Parses order notes into line items with quantities.
- Aggregates quantities for the selected range.
- Unparsed lines are displayed separately.

## 5.2 Customers module
### 5.2.1 Customers list
**UI**
- Top app bar with title and More icon.
- Search field with clear icon.
- Filter chips: All, Due, Extra, Clear.
- Sort menu: Balance high-low, low-high, Name A-Z.
- List rows show:
  - Customer initials, name, phone
  - Billed and paid summary
  - Balance chip (Due/Extra/Clear)

**UX**
- Search is debounced (300ms).
- Long-press or menu opens action sheet:
  - View details
  - Payment history
  - Add order (supported by screen but not wired in main navigation)
  - Edit customer (supported by screen but not wired)
  - Delete customer (supported by screen but not wired)
- Empty state offers "Add customer" (wired to Import Contacts).

### 5.2.2 Customer detail
**UI**
- Balance card:
  - Balance due or extra credit
  - Order totals, payments applied, extra credit
  - Call and Message buttons if phone is present
  - "Record payment" button
- Orders section:
  - Filters: All, Open, Closed, Unpaid, Partial, Paid, Overpaid
  - Each order row with status chip and actions menu
- Ledger section:
  - Search input
  - Filters: All, Orders, Payments, Adjustments
  - Ledger grouped by month (expand/collapse)

**UX**
- Order menu supports:
  - Payment history
  - Close order override
  - Reopen order override
  - Clear override
  - Write off balance (only when eligible)

**Logic**
- Effective order status is based on paid state, unless overridden.
- Write-off only allowed if:
  - Order is OPEN
  - Amount unpaid
  - Order date is at least 1 month old
- Ledger search matches description, amount, or formatted date.

### 5.2.3 Import contacts
**UI**
- Search field.
- Select all row.
- List of contacts with toggle.
- Import button with selected count.

**UX**
- Requires READ_CONTACTS permission.
- Skips contacts without numbers.
- Normalizes phone numbers.

## 5.3 Accounting module
### 5.3.1 Money screen (tabs)
- TabRow with three tabs: M-PESA, Manual, Ledger.
- Each tab shows its own screen with shared top tabs.

### 5.3.2 M-PESA import
**UI**
- Text area for pasted M-PESA messages.
- Paste and Clear actions, auto-collapse for large input.
- Filter chips: All, Needs, Duplicates, Selected.
- Summary counts and selection controls.
- Transaction cards with:
  - Amount, code, time
  - Sender info
  - Status pill (Ready, Selected, Needs match, Duplicate)
  - Suggestions and allocation controls
  - Existing duplicate actions (Move, Receipt details)

**UX**
- Select ready-only, select all, clear selection.
- Apply-ready bar at bottom when matches exist.

**Logic**
- Parses text into transactions (amount, code, sender, time).
- Detects duplicates by transaction code or hash.
- Auto-suggests customer and up to 3 orders by confidence.
- Allocation modes:
  - Specific order
  - Oldest orders
  - Customer credit
- Apply creates receipts and allocations; duplicate state prevents re-apply.

### 5.3.3 Manual payment
**UI**
- Customer search or selected customer card.
- Amount field (KES).
- Method chips: Cash, M-PESA.
- Note field.
- Allocation: Oldest orders or Pick order (opens sheet).
- Save payment button in bottom bar.

**UX**
- Suggests customers while typing.
- When picking an order, shows outstanding amounts.

**Logic**
- Creates a payment receipt and allocates to chosen order or oldest orders.
- Validation: amount > 0 and customer selected.

### 5.3.4 Ledger (Money tab)
**UI**
- Summary card: charges, payments, write-offs, reversals, net balance.
- Filter chips: All, Charges, Payments, Adjustments.
- Entry cards with description, order/customer labels, signed amounts, date.

**Logic**
- Loads recent entries (default 250).
- Net balance = debits - credits - write-offs + reversals.

### 5.3.5 Payment history
**UI**
- Top app bar with context label (All/Customer/Order).
- Optional header with customer or order label.
- List rows show:
  - Date/time, display amount
  - Method and transaction code
  - Target (order or customer)
  - Status pill (Not used, Part used, Used, Voided)
  - Buttons: Move, Void

**UX**
- Move dialog:
  - Allocation modes: Order, Oldest orders, Customer credit
  - Order selector if applicable
- Void dialog accepts optional reason.
- Focus receipt id scrolls to the relevant row.

**Logic**
- Void creates reversal entries and marks receipt voided.
- Move reallocates receipt allocations and updates receipt status.

## 5.4 Core module
### 5.4.1 Voice input overlay
**UI**
- Floating mic button.
- Expanded idle panel with target and notes mode selection.
- Listening, result, and error panels.

**UX**
- Can target Total or Notes; follows focused field unless set manually.
- Notes mode: Append or Replace.
- Drag to reposition; auto-hide when idle.

**Logic**
- Uses Android SpeechRecognizer.
- Parses totals via voice math; notes via voice notes parser.
- Applies values to the last focused total field or notes field.

### 5.4.2 Backup and restore
**UI**
- Toggle for automatic daily backup.
- Backup location selection: app storage or folder (SAF).
- Manual backup button.
- Restore: latest app backup or pick file.
- Progress indicators for running backup/restore.

**Logic**
- Backup exports database to a ZIP with JSON files and metadata.
- App storage keeps last 7 backups.
- Restore clears database and imports all entities.
- WorkManager is used for daily and manual jobs.

### 5.4.3 Notifications
**UI**
- Toggle for order reminders with permission handling.
- Lead time chips: 1 hour, 1 day.
- Toggle for daily summary.

**Logic**
- Worker runs hourly:
  - Due reminders for orders within lead-time window.
  - Daily summary after 7:00 once per day.
- Orders excluded if cancelled, closed, or fully paid.
- Uses pickup time if present, otherwise default 09:00.
- Tracks reminder history to avoid duplicates.

### 5.4.4 Widgets and shortcuts
**Widget**
- Displays today order count and unpaid order count.
- Shows up to 3 order lines.
- Tap actions: open today, open unpaid, add order.

**Shortcuts**
- Dynamic shortcuts for new order, today, unpaid.

### 5.4.5 Update dialog
- Shows a "What's new" list when the version changes and the Calendar tab is active.

## 6. Permissions
- READ_CONTACTS: Import contacts.
- RECORD_AUDIO: Voice input overlay.
- POST_NOTIFICATIONS: Required on Android 13+ for reminders.

## 7. Formatting and utilities
- Currency formatting: KES with rounding (no decimals in display).
- Phone numbers: normalized to digits and plus sign; phone matching via candidate expansion.
- Pickup time parsing: supports formats like 9, 930, 9:30, 9.30.
- Order note parser extracts product quantities for chef prep.

## 8. Known screen behaviors and edge cases
- If customer name is entered without phone, order cannot be saved.
- Deleting orders with payments requires move or void decisions.
- If order total is reduced below payments, extra credit is moved to customer-level credit.
- Receipt reallocation or voiding creates reversal ledger entries.
- In customer list, action sheet includes edit/delete hooks that are not wired in main navigation.

---
End of current behavior documentation.
