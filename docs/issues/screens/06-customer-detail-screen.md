# Customer Detail Screen UI/UX Issues

Status: Open

## Scope
- Route: `customer/{customerId}`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerDetailScreen.kt`

## Owner goals
- Review one customer account health quickly.
- Act on open orders and payment history safely.
- Avoid risky write-off mistakes.

## Open issues

### P1
- Orders and ledger stacked in one long page can feel heavy and slow to navigate.
- Override and write-off actions are powerful; risk messaging can be stronger.
- Ledger filtering is good but month sections can still feel deep.

### P2
- Contact actions and finance actions can be grouped more clearly.
- Historic month expansion behavior can be more predictable.

## Acceptance criteria
- Account summary stays visible while drilling into orders/ledger.
- High-risk actions require clear, plain-language confirmation.
- Orders and ledger views support quick switching without scrolling fatigue.
