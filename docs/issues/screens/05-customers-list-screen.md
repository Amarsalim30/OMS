# Customers List Screen UI/UX Issues

Status: Open

## Scope
- Route: `customers`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt`

## Owner goals
- Find customer fast.
- See who owes quickly.
- Trigger payment/order actions with minimal friction.

## Open issues

### P1
- Many controls (search/filter/sort/toggles) create high first-load complexity.
- Long-press action sheet remains less discoverable than explicit row actions.
- Owing vs settled urgency can be clearer in row-level visual cues.

### P2
- Active filters are shown, but not always obvious which control caused result changes.
- No batch actions for high-volume follow-up.

## Acceptance criteria
- Top debtors are identifiable within first glance.
- Primary actions are visible without long-press.
- Filter/sort state is always explicit and reversible in one tap.
