# Title: Iconography & navigation workflow review (professional Google‑level UX)

## Context
This app’s navigation and screen workflows rely heavily on icon meanings and icon placement. The current icon set is a mix of generic symbols (Calendar, List, People, Payments) plus top‑bar action icons that vary by screen. We need a deliberate, consistent icon system and workflow that feels “Google‑level”: fast, predictable, and obvious for new staff.

Primary navigation icons (current):
- Calendar (home): Icons.Filled.CalendarToday
- Orders: Icons.Filled.ListAlt
- Customers: Icons.Filled.People
- Money: Icons.Filled.Payments
Source:
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt (topLevelDestinations)

Other key action icons are distributed across screens (Summary, Calendar, Day detail, Payment history, etc.) without a consistent visual language or placement conventions.

## Problem
The app’s iconography and navigation workflows do not yet read as a cohesive system:
- Some icons are ambiguous (“ListAlt” for Orders vs Calendar vs Summary), leading to guesswork.
- The same action appears with different icons or placement across screens (ex: summary/ledger/receipts).
- Critical flows (Record payment, View receipt history, Jump to customer/summary) are icon‑only without clear prioritization or text affordance.
- The overall navigation workflow feels less efficient than a professional Google‑level product (too much hunting for the right icon or tab).

## Current behavior (facts)
- Bottom nav uses 4 icons only, no sub‑labels beyond the nav label text.
- Money screen uses tabs (M‑PESA, Manual, Ledger) while top‑level nav has a “Money” icon — some users may not know the difference between “Money” vs “Ledger”.
- Summary and receipt history are accessed from various top bars with inconsistent icons.

## Expected behavior (rules)
- Every primary action should have a single, consistent icon and placement across all screens.
- Icon meaning must be unambiguous for staff without training.
- Navigation should prioritize the fastest workflow paths:
  - Home (Calendar) → Day → Order → Record payment → Receipt history → Ledger
  - Customers → Customer detail → Record payment → Receipt history
- Icons for “Receipts/History/Ledger/Payments” must be visually distinct and consistently used.
- If an icon is ambiguous, add a text label (or move to primary button) instead of icon‑only.

## Scope / screens to review
- Calendar (home)
- Day detail
- Summary
- Orders list / Unpaid orders
- Customer list/detail
- Money (M‑PESA / Manual / Ledger)
- Payment history
- Bottom nav + More actions

## Acceptance criteria (DoD)
- A documented icon map: action → icon → screen placement.
- Remove ambiguous icons or replace them with clearer Google‑style symbols.
- Standardize top‑bar actions across screens (order of icons, grouping, and behavior).
- Navigation workflow is faster: most common actions reachable in ≤2 taps.
- User testing: new staff can complete “record a payment” flow without asking where the icon is.

## Notes
- This is a UX / design consistency issue; the fix may require both icon replacement and workflow restructuring.
- Consider adding a “primary action button” where icon‑only actions cause confusion.
