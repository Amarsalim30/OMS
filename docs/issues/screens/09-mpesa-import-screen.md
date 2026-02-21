# M-PESA Import Screen UI/UX Issues

Status: Open

## Scope
- Embedded in `money` tab (Collect)
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt`

## Owner goals
- Paste messages once.
- Match accurately.
- Apply payments safely at speed.

## Open issues

### P1
- Raw text input, filters, and selection tools are functionally rich but visually dense.
- Needs/duplicates/selected states can still be confusing under time pressure.
- Allocation confidence and suggestion reasoning need stronger cues.

### P2
- Batch apply controls could provide stronger impact preview before posting.
- Duplicate resolution actions can be simplified by scenario.

## Acceptance criteria
- User can process a typical intake batch with minimal re-checking.
- Each row state is immediately understandable without opening details.
- Apply action shows clear expected outcome before confirmation.
