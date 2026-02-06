# Credit Prompt Missing On Order Total Edit

## Summary
When a payment is larger than the original order total (for example order `1000`, payment `1050`), users may edit the order total to include delivery (`1050`).  
Before the fix, no "Apply available credit?" prompt appeared in this edit flow, even when customer-level credit existed.

## Reproduction
1. Create an order with total `1000`.
2. Forward/import M-PESA payment of `1050` for that customer.
3. Edit the same order total from `1000` to `1050`.
4. Save the order.

## Actual Behavior (Before Fix)
- No credit prompt appears after edit.
- User can miss applying available customer credit to this order.

## Expected Behavior
- On save, if customer has available credit and the edited order still has outstanding balance, show:
  - `Apply available credit?`

## Impact
- Missed credit application opportunities.
- Extra manual reconciliation steps in accounting.
- Confusing UX because prompt appeared on new orders but not equivalent edit scenarios.

## Root Cause
- Credit prompt eligibility was previously gated to new orders only.
- Edit flows were excluded even when settlement context changed (total/customer updates).

## Fix Implemented
- Prompt logic now evaluates both new and edited orders when settlement-relevant fields change.
- Prompt now triggers when all are true:
  - customer exists,
  - available customer credit > `0`,
  - order still has outstanding amount after save/reconcile.

## Evidence
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:193`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:204`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:703`
- `app/src/test/java/com/zeynbakers/order_management_system/order/ui/OrderViewModelCreditPromptTest.kt`

## Status
- Fixed in current branch/workspace.
