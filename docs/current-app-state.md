# OMS App Current State

Last updated: 2026-02-19

## Product structure
- Bottom navigation remains task-based: Orders, Customers, Money, More.
- UI has been simplified for small phones with fewer dense headers/cards and clearer primary actions.

## Orders module
- Calendar screen uses a single top summary card (lighter layout, better grid visibility).
- Day detail is optimized for fast triage and entry:
  - collapsible search
  - compact filters
  - tighter order cards
  - delete as icon action
- Unpaid orders top bar is simplified to search-first behavior only (no extra top action clutter).

## Customers module
- Customer list:
  - compact, scannable customer rows
  - filter/sort chips and clear active-state behavior
  - refined search UX aligned with unpaid orders search pattern
- Customer detail:
  - redundant top icons removed
  - denser/legacy statement block removed
  - clearer summary card action hierarchy
  - orders section redesigned for scan speed and lower cognitive load
- Customer statement:
  - now a dedicated customer flow (not in Money)
  - chronological timeline rows only
  - right-aligned amounts + running balance per row
  - no internal order IDs shown
  - balance shown at top and final balance at bottom
  - payment allocation details on tap
  - explicit bad debt rows and flow

## Money module
- Money tabs are now exactly:
  - `Collect`
  - `Record`
- `Statements` tab removed.

## Settings and utility screens
- Removed redundant/non-essential cards and links:
  - Money shining header card
  - Payment history summary header card
  - Backup data protection card
  - Backup misplaced import contacts button
  - Notification reminder operations card
  - Contacts customer onboarding card
  - More menu: Order Summary and Payment History links

## Current UX direction
- Low-literacy-first copy and flows.
- Banking-style scan patterns for money rows (left meaning, right amount/balance).
- Reduced context switching by moving account understanding into customer flows.

## Active backlog location
- Open issues remain in `docs/issues` and `docs/issues/screens`.
- Resolved/superseded items are being removed to keep backlog focused.
