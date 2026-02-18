# Payment History Screen UI/UX Issues

Status: Open

## Scope
- Routes:
  - `payment_history/all`
  - `payment_history/customer/{customerId}`
  - `payment_history/order/{orderId}`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryScreen.kt`

## Owner goals
- Audit receipts quickly.
- Move/void safely.
- Keep allocation traceability clear.

## Open issues

### P1
- Move/void dialogs are powerful but should show stronger impact previews.
- Receipt status is visible; target and allocation explanation can be clearer.
- High-risk actions need clearer guardrails for accidental taps.

### P2
- No inline filter chips by status/method for rapid triage.
- Context header can show stronger route-specific meaning.

## Acceptance criteria
- User can understand receipt target and status in one glance.
- Move/void flows include before/after summary.
- Risky actions require explicit confirmation language.
