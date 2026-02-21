# Ledger Screen UI/UX Issues

Status: Open

## Scope
- Advanced ledger view from Money/Statements menu
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerScreen.kt`

## Owner goals
- Audit global accounting movement.
- Filter quickly by type.
- Spot anomalies early.

## Open issues

### P1
- Entry density is high; visual grouping by day/customer/order can be stronger.
- Filter chips need stronger active-state prominence.
- Net balance context can be clearer when filters are applied.

### P2
- No quick export/share from audit view.
- No anomaly highlighting for unusual reversals/write-offs.

## Acceptance criteria
- Filtered ledger always explains what subset is shown.
- Key totals remain visible while scrolling.
- High-risk entries stand out without opening every row.
