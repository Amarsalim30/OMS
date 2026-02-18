# Unpaid Orders Screen UI/UX Issues

Status: Open

## Scope
- Route: `orders`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`

## Owner goals
- Prioritize collections.
- Take payment action immediately.
- Protect privacy when showing balances publicly.

## Open issues

### P1
- Search, sort chips, and visibility toggle are useful but still feel scattered.
- No fast “next best follow-up” guidance (largest due, oldest, and overdue require manual switching).
- Row-level “Pay” is clear, but reminder/follow-up actions are missing.

### P2
- No bulk follow-up workflow for many overdue orders.
- Date group headers do not provide quick jump controls.

## Acceptance criteria
- User can process top 10 follow-up orders with fewer taps.
- Follow-up actions (message/remind) are directly available.
- Priority sort mode can be set and retained as default.
