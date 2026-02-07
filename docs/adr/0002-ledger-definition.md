# ADR 0002: Ledger Definition

## Status
Accepted

## Date
2026-01-31

## Context
OMS uses a unified ledger to compute balances for orders and customers, and to present financial history in the app. The ledger is the source of truth for totals and summaries.

Primary sources in code:
- app/src/main/java/com/zeynbakers/order_management_system/accounting/data/AccountingDao.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerViewModel.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerScreen.kt
- app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt

## Decision
1) The ledger is the account_entries table.
2) Entry types and meaning:
   - DEBIT: order charge (increases balance owed).
   - CREDIT: payment or customer credit (reduces balance owed).
   - WRITE_OFF: adjustment for bad debt (reduces balance owed).
   - REVERSAL: reversal of a prior credit (increases balance owed).
3) Net balance formula (as used in summaries):
   - Net = Debits - Credits - WriteOffs + Reversals.
4) Order-level vs customer-level:
   - orderId != null: entry is tied to a specific order.
   - orderId == null and customerId != null: entry is customer-level credit or reversal.
5) Ledger queries and summaries must use the same type math as above to avoid drift.
6) Customer balances and finance summary are derived from account_entries, not from orders or receipts directly.

## Consequences
- Any new payment or adjustment flow must write account_entries consistent with the definitions above.
- UI summaries (Ledger screen, Customer detail) must not reinterpret entry types.
- Changes to ledger semantics require a new ADR and migration plan.

## References
- Ledger totals and summaries: AccountingDao
- Ledger UI rendering: LedgerViewModel, LedgerScreen
- Customer ledger filters: CustomerDetailScreen
