# Manual Payment Screen UI/UX Issues

Status: Open

## Scope
- Embedded in `money` tab (Record)
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt`

## Owner goals
- Find customer quickly.
- Enter amount fast and accurately.
- Choose correct allocation target with confidence.

## Open issues

### P1
- Customer selection list can be improved with due-priority ordering.
- Allocation mode and selected target need stronger visibility.
- Save action is bottom-only; a sticky context summary would help.

### P2
- Method selection is clear but can include richer defaults/history.
- Optional note field can be collapsed when not used.

## Acceptance criteria
- Customer pick, amount entry, and allocation choice are all visible in one flow.
- Target order selection cannot be mistaken.
- Save confirmation reflects exactly where payment was applied.
