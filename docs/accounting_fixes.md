Accounting fixes audit (initial findings)

- DEBIT creation paths:
  - OrderViewModel.upsertAccountingEntry creates DEBIT for every saved order.
  - OrderProcessor.completeOrder also created a DEBIT directly.
  - Risk: duplicate billing for the same order.
- CREDIT creation paths:
  - CustomerAccountsViewModel.recordPayment inserts CREDIT ledger entries.
  - PaymentProcessor.recordPayment also inserts CREDIT (not used in UI flows).
- Payments table:
  - payments table exists; insertPayment is not used in current flows.
- Totals inconsistency:
  - OrderEntity.amountPaid and OrderDao.totalPaid were used by CustomerViewModel,
    while UI/ledger relies on account_entries credits for paid amounts.
- Date handling:
  - Order debits use orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).
  - Payments use Clock.System.now() epoch millis.

Fix strategy applied:
- Model A (ledger DEBIT exists for every non-cancelled order) with a single upsert method
  in AccountingDao to enforce idempotency (delete+insert).
- Removed secondary DEBIT insert path by routing OrderProcessor.completeOrder through upsert.
- Switched customer ledger totals to AccountingDao (ledger-based) to avoid amountPaid mixing.
- Payment recording now enforces amount > 0 and splits overpayments into a customer-level credit.
