# Title: Replace overall Ledger with Customer‑specific Ledgers/Statements

## Context
Current Ledger screen is a global, mixed‑customer ledger. This does not match the core workflow of the app (orders + payments tied to customers) and makes reconciliation harder.

Files / screens involved:
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerScreen.kt
  - Current overall ledger list
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerViewModel.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt
  - Already has customer balance + history context
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt
  - Money tab includes Ledger

## Problem
- Overall ledger mixes all customers, which is less actionable than a customer statement.
- Users need customer‑specific statements more often than a global ledger.
- Current ledger creates extra steps: you must scan/filters to find customer entries.

## Expected behavior (rules)
- Replace “overall ledger” with customer‑specific ledgers/statements as the primary ledger view.
- From Customers list or Customer detail, user should access a full statement easily.
- Statement should show:
  - Customer name + phone
  - Opening balance, charges, payments, reversals, write‑offs
  - Running balance per entry
  - Date range filter (e.g. month, custom range)

## Proposed workflow
- Money tab → Ledger becomes “Customer Statements”:
  - Step 1: pick a customer (search by name/phone)
  - Step 2: show customer ledger/statement
- Customer detail screen gets a “Statement” action (top bar or button) that opens the same statement view.

## Steps to reproduce (current issue)
1. Open Money > Ledger.
2. Observe entries for all customers mixed together.
3. Try to isolate one customer’s statement – requires manual scanning.

## Acceptance criteria (DoD)
- Ledger entry point now leads to a Customer Statement flow.
- Statement view is customer‑specific and shows running balance.
- Date range filter exists (month / custom range).
- Navigation from Customer detail to statement is ≤1 tap.
- Global mixed‑customer ledger removed or tucked behind an “All customers” advanced option.

## Notes
- This aligns better with how the business operates (payments + orders tied to customers).
- If a global ledger is still needed, it should be secondary, not the default.
