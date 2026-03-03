# OrderBook (OMS)

OrderBook is a bakery-focused Order Management System (OMS) Android app. It manages daily and monthly orders, customer balances, and payment intake with a full accounting ledger. The app is offline-first, built with Kotlin and Jetpack Compose, and includes background backups, reminders, and a home screen widget.

This README is intentionally detailed and mirrors the current codebase behavior as of this repo.

## Product scope
- Track orders by day and month with payment status (unpaid, partial, paid, overpaid).
- Manage customers, balances, and full ledger history.
- Record payments manually or import M-PESA SMS messages in bulk.
- Allocate receipts to specific orders, oldest open orders, or customer credit.
- Provide summaries for chef prep, reporting, and daily workflows.
- Include reminders, backups, shortcuts, and a home screen widget.

## Tech stack and architecture
- Android, Kotlin, Jetpack Compose (Material 3).
- Single-activity app with Compose Navigation.
- Room database (offline-first).
- WorkManager for background jobs (backups, notifications, widget updates).

## Build configuration
- ApplicationId: `com.zeynbakers.order_management_system`
- App name (launcher): `OrderBook`
- Min SDK: 24
- Target SDK: 36
- Compile SDK: 36
- Version: 3.0 (versionCode 6)
- Android Gradle Plugin: 8.12.3
- Kotlin: 2.0.21
- Compose BOM: 2024.09.00

## Key modules (package layout)
```
app/src/main/java/com/zeynbakers/order_management_system/
  accounting/    # payment intake, ledger, receipts, allocations
  core/          # backups, notifications, widgets, navigation, shared UI
  customer/      # customer list, detail, ledger
  order/         # calendar, day detail, unpaid list, summary
  MainActivity   # navigation + top-level orchestration
```

## Navigation and routes
Top-level navigation adapts by window size:
- Compact: bottom navigation
- Medium+: navigation rail

Top-level tabs:
- Calendar
- Orders (unpaid view)
- Customers
- Money

Routes (Compose Navigation):
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

"More" actions (from the top-level menu):
- Floating helper settings
- Notes history
- Backup and restore
- Notifications
- Import contacts
- Tutorial

## Features and workflows

### Orders module
**Calendar**
- Month grid with swipeable months and cached month snapshots.
- Month summary card with totals and unpaid badge.
- Tap to select day, tap selected day to open day detail.
- Long-press day for Quick Add.
- Floating action button to add an order.

**Quick Add**
- Bottom sheet (~90% height).
- Fields: notes, total, pickup time, customer name, customer phone.
- Validation: notes required, total numeric > 0, name requires phone.
- Voice overlay for notes/total.

**Day Detail**
- Summary card: totals, paid, balance, counts by payment status.
- Filter chips: All, Unpaid, Partial, Paid, Overpaid.
- Search by notes/customer.
- Order list with payment status pill and actions.
- FAB to add a new order.

**Order editor**
- Bottom sheet with the same fields as Quick Add.
- Editing preloads customer details; saving reconciles ledger entries.

**Delete with payments**
- Confirmation dialog offers Move or Void allocations.
- Move targets: specific order, oldest orders, or customer credit.
- Void creates reversals, marks allocations voided, and cancels the order.

**Unpaid Orders**
- Summary card: count and total outstanding.
- Grouped by date with a quick Record Payment action.

**Summary**
- Month totals + Chef prep card with day/week/month ranges.
- Aggregates items parsed from order notes.
- Copy functionality for chef list and individual notes.
- Actions to open ledger, notifications, and backup.

### Customers module
**Customers list**
- Search (300 ms debounce), filter chips (All/Due/Extra/Clear), sort menu.
- Rows show customer, phone, billed/paid, and balance chip.
- Action sheet for details, payment history, add order, edit, delete.
- Empty state links to Import Contacts.

**Customer detail**
- Balance card with call/message actions if phone exists.
- Orders section with filters and status override actions.
- Ledger section with search, filters, and month grouping.
- Write-off allowed only if order is OPEN, unpaid, and at least 1 month old.

**Import contacts**
- Permission-gated (READ_CONTACTS).
- Toggle per contact or select all.
- Normalizes phone numbers and skips contacts without numbers.

### Accounting module
**Money screen**
- Tabs: M-PESA, Manual, Ledger.

**M-PESA import**
- Paste M-PESA messages into a text area.
- Parses transactions, detects duplicates, and suggests matches.
- Allocation modes: specific order, oldest orders, customer credit.
- Apply-ready actions and duplicate handling.

**Manual payment**
- Select customer, amount, method, note.
- Allocation mode: oldest orders or pick an order.
- Saves receipt and allocations; validates amount > 0 and customer selection.

**Ledger**
- Summary card: charges, payments, write-offs, reversals, net.
- Filter chips and entry cards for recent activity.

**Payment history**
- Filters: All, Customer, Order.
- Row shows method, code, status, and target.
- Move or void receipts with reversal entries.

### Core module
**Floating helper + voice capture**
- Foreground helper bubble can run across apps when overlay permission is granted.
- Capture actions open a dedicated voice capture activity for microphone-safe behavior.
- Voice capture supports calculator parsing and quick note persistence to Notes history.

**Backup and restore**
- Manual and daily backups via WorkManager.
- Supports app-private storage, single SAF file, or SAF folder targets.
- Backups export ZIP with JSON + manifest metadata.
- Restore integrity policy defaults to Strict (manifest/checksum verification), with optional legacy mode.
- Android system Auto Backup is disabled; the in-app backup module is the authoritative restore path.

**Notifications**
- Due reminders (1 hour or 1 day lead time).
- Daily summary after 7:00 once per day.
- Uses pickup time when present, otherwise 09:00.

**Widget and shortcuts**
- Widget shows today and unpaid snapshot (up to 3 orders).
- Shortcuts: New order, Today, Unpaid.

**Update dialog**
- "What's new" dialog appears on Calendar when version changes.

## Data model (Room)
Entities:
- `orders` (orderDate, status, totalAmount, customerId, etc.)
- `order_items` (orderId, name, category, quantity, unitPrice)
- `customers` (name, phone)
- `account_entries` (DEBIT, CREDIT, WRITE_OFF, REVERSAL)
- `payments` (legacy, not used in current flows)
- `payment_receipts` (amount, receivedAt, method, status, rawText, sender info)
- `payment_allocations` (receiptId, orderId/customerId, amount, status)

Accounting rules (source of truth):
- Ledger is `account_entries`.
- Net balance = Debits - Credits - WriteOffs + Reversals.
- Payments are receipts, then allocations.
- Applied allocations create CREDIT entries.
- Voids and reallocations create REVERSAL entries.

See ADRs in `docs/adr/` for the detailed rules:
- `docs/adr/0001-payment-allocation-rules.md`
- `docs/adr/0002-ledger-definition.md`

## Intents, shortcuts, and widget
- Share intent (text/plain): appends to M-PESA intake if Money/M-PESA is open, otherwise navigates to Money/M-PESA and pre-fills.
- App shortcuts: New order, Voice note, Notes history, Unpaid.
- Widget intents: show today, show unpaid, new order.

## Permissions
- READ_CONTACTS: importing customer contacts.
- RECORD_AUDIO: voice capture.
- POST_NOTIFICATIONS: order reminders and daily summary (Android 13+).
- SYSTEM_ALERT_WINDOW: optional cross-app floating helper.
- FOREGROUND_SERVICE / FOREGROUND_SERVICE_DATA_SYNC: helper service lifecycle.

## Local development
Prerequisites:
- Android Studio (Hedgehog or newer recommended).
- JDK 11.
- Android SDK with API 36 installed.

Common commands (Windows PowerShell or bash):
```
# Debug APK
./gradlew assembleDebug

# Custom task defined in root build.gradle.kts
./gradlew debug

# Unit tests
./gradlew testDebugUnitTest

# Instrumentation tests (requires device or emulator)
./gradlew connectedDebugAndroidTest
```

## Tests
Unit tests:
- Voice math parser and notes parser
- Order notes parser

Android instrumentation tests:
- Accounting logic
- Calendar month screen UI

## Project structure (top-level)
```
app/                 # Android app module
docs/                # Requirements, ADRs, and UX/issue notes
gradle/              # Version catalogs and wrapper config
.idea/ .vscode/      # IDE settings (local)
```

## Documentation
- `docs/requirements.md` is the definitive "current behavior" spec.
- `docs/accounting_fixes.md` documents a completed accounting audit.
- `docs/adr/` contains architecture decisions for payments and ledger.
- `docs/issues/` contains UX review notes and open issues.

## Design and prototype assets
The repo includes UI prototypes and assets used during design exploration:
- `mpesa-payment-enhanced.html`, `styles.css`, `script.js`
- Images and screenshots in the repo root (e.g., `home1.jpeg`, `money1.jpeg`)
- Archived app snapshots: `app.zip`, `app (2).zip`, `app (3).zip`, etc.

Note: `index.html` in the repo root is very large and is treated as a design artifact.

## Known notes and open UX reviews
Refer to `docs/issues/` for detailed UX review notes, including:
- Bottom navigation back stack and insets behavior.
- Calendar day opening discoverability and weekday alignment.
- Customer module UI/UX review.
- Payment intake selection edge cases.

## Contribution and changes
- Keep `docs/requirements.md` in sync with code changes that affect behavior.
- Update ADRs for any changes to payment allocation or ledger semantics.
- Use `docs/issues/` for UX review notes and open problems.
