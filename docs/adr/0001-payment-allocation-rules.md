# ADR 0001: Payment Allocation Rules

## Status
Accepted

## Date
2026-01-31

## Context
OMS records payments as receipts and allocates them to orders or customer credit. This logic powers:
- M-PESA import allocation and duplicate handling.
- Manual payment recording.
- Payment history move/void actions.
- Order deletion with payment handling.

Allocation rules must be stable to keep balances and ledgers correct.

Primary sources in code:
- app/src/main/java/com/zeynbakers/order_management_system/accounting/domain/PaymentReceiptProcessor.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeViewModel.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryViewModel.kt
- app/src/main/java/com/zeynbakers/order_management_system/accounting/data/AccountingDao.kt
- app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt

## Decision
1) All payments are recorded as payment receipts, then allocated.
2) Allocation targets are limited to:
   - A specific order.
   - Oldest open orders for a customer.
   - Customer credit (no order).
3) Allocation outcomes are recorded as payment allocations with status APPLIED or VOIDED.
4) Ledger impact:
   - Each applied allocation creates a CREDIT ledger entry tied to the order or customer.
   - Voiding or reallocating creates REVERSAL ledger entries for any prior applied allocations.
5) Receipt status is derived from applied amount:
   - UNAPPLIED: 0 applied
   - PARTIAL: applied < receipt amount
   - APPLIED: applied == receipt amount
   - VOIDED: explicitly voided
6) If an allocation to an order exceeds the outstanding balance:
   - The remainder is applied to customer credit (if customer is known).
7) If an order is deleted:
   - User chooses to MOVE allocations or VOID them.
   - MOVE reassigns allocations to another target (full receipt or selected allocations).
   - VOID reverses allocations and marks them voided.
   - Order is then marked CANCELLED and its debit/write-off entries are removed.
8) M-PESA intake selection rules:
   - Items are selectable only if duplicateState == NONE and canApply() == true.
   - Duplicates can be viewed or reallocated but must not be applied as new receipts.

## Consequences
- Any UI flow that changes allocations must use the same ReceiptAllocation rules.
- Ledger totals and customer balances depend on the allocation and reversal rules above.
- Future features must not alter allocation semantics without a new ADR.

## References
- ReceiptAllocation and allocation logic: PaymentReceiptProcessor
- Intake selection rules: PaymentIntakeViewModel + MpesaImportScreen
- Order save reconciliation: OrderViewModel + AccountingDao
