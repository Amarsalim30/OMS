# Customer Statements Screen UI/UX Issues

Status: Open

## Scope
- Embedded in `money` tab (Statements)
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt`

## Owner goals
- Find customer account.
- Select reporting range fast.
- Read opening, movement, and closing balance clearly.

## Open issues

### P1
- Customer list mode and statement detail mode in one screen can feel context-heavy.
- Date range controls (month/custom) need clearer default guidance.
- Summary card and ledger rows are useful but dense on small screens.

### P2
- Reversal visibility toggle can be explained better for non-accounting users.
- Invalid custom range state can provide stronger recovery hints.

## Acceptance criteria
- User can move from customer select to clear statement in under 3 steps.
- Range controls are self-explanatory and easy to reset.
- Statement summary is scannable at a glance.
