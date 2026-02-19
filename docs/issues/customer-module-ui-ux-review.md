# Title: Customer module UI/UX review (single-file issue list)

Status: fixed (2026-02-19)

## Scope
Customer module screens and flows:
- Customer list (filters, search, sort, actions)
- Customer detail (orders, ledger, filters)
- Import contacts

Primary files:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailSections.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CustomersGraph.kt`
- `app/src/main/res/values/strings.xml`

## Resolution summary
All listed items are now implemented and verified by build.

### 1) Replace hard delete with archive
Status: fixed
- Customer list destructive action is now conditional:
  - customers with transactions -> Archive
  - customers without transactions -> Delete
- Delete action wired end-to-end from UI to ViewModel.
- ViewModel enforces safety by checking ledger entries before delete.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:422`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:464`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt:116`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CustomersGraph.kt:54`
- `app/src/main/res/values/strings.xml:145`

### 2) Default view should not hide customers unintentionally
Status: fixed
- Default list state remains:
  - Filter `All`
  - `Hide zero balances = true`
  - `Show inactive = false`
- Toggle controls are present and explicit.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:133`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:135`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:189`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:196`

### 3) Rename chips for non-technical clarity
Status: fixed
- Filter chips use `Owes`, `Credit`, `Settled`.
Evidence:
- `app/src/main/res/values/strings.xml:153`
- `app/src/main/res/values/strings.xml:154`
- `app/src/main/res/values/strings.xml:155`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:272`

### 4) Normalize phone numbers + WhatsApp links
Status: fixed
- Messaging now uses normalized E.164 and launches WhatsApp first, then SMS fallback.
- Contact import + customer creation dedupe by normalized phone remains in place.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerMessaging.kt:8`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:339`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailSections.kt:124`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerAccountsViewModel.kt:74`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:575`
- `app/src/main/java/com/zeynbakers/order_management_system/ContactsLoader.kt:33`

### 5) Make actions discoverable without long-press
Status: fixed
- Row tap opens details.
- Visible trailing menu is present on every row.
- Critical actions are accessible from menu without long-press.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:315`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:393`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:367`

### 6) Remove sort duplication
Status: fixed
- Sort control is in the list control row; top bar does not expose duplicate sort.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:179`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListSections.kt:66`

### 7) Import contacts: select-all respects filters
Status: fixed
- Select-all applies to visible/filtered contacts.
- Label switches to `Select all (visible)` when filtered.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt:66`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt:174`
- `app/src/main/res/values/strings.xml:353`

### 8) Import action visibility
Status: fixed
- Import action is primary in bottom bar and disabled until selection exists.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt:87`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt:107`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/ImportContactsScreen.kt:112`

### 9) Ledger search performance
Status: fixed
- Query input is debounced.
- Ledger filtering and section building are executed on `Dispatchers.Default`.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:117`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:123`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt:128`

### 10) Action sheet hierarchy
Status: fixed
- Primary actions appear first.
- Destructive action is visually separated by divider and highlighted in error color.
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:367`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:399`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:403`

## Acceptance criteria
- [x] Delete only allowed for customers with zero transactions; otherwise archive.
- [x] Default list = All with Hide zero balances ON (toggle available).
- [x] Chips read Owes / Credit / Settled.
- [x] Phone normalization is consistent; WhatsApp uses E.164 with SMS fallback.
- [x] Dedupe by normalized phone on import and creation.
- [x] All critical actions discoverable without long-press.
- [x] Select-all respects filters and clarifies scope.
- [x] Import action is prominent and disabled until items selected.
- [x] Ledger search remains smooth under load.

## Verification
- Build:
  - `./gradlew.bat :app:compileDebugKotlin` (successful on 2026-02-19)
