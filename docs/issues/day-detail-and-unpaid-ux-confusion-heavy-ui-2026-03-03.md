# Title: Day Detail and Unpaid Orders are confusing and visually heavy

## Date
2026-03-03

## Reported problem
Customer feedback:
1. Day Detail is confusing when used together with Unpaid Orders.
2. Both screens feel heavy in UI/UX and harder than necessary for daily operations.

## Why this is happening (current-state findings)

### P0 confusion and mental-model breaks
- **Unpaid definition mismatch across screens**:
  - Day Detail `Unpaid` only reflects `UNPAID`, while collections flow expects "still owes" (`UNPAID + PARTIAL`).
  - References: `DayDetailScreen.kt:226`, `DayDetailModels.kt:123`, `DayDetailModels.kt:175`.
- **Unpaid row tap loses order context**:
  - Tapping an unpaid row opens a day, not the exact order, so users must search again.
  - References: `UnpaidOrdersSections.kt:159`, `UnpaidOrdersSections.kt:171`, `UnpaidOrdersScreen.kt:395`.
- **Sort intent does not match visible list behavior**:
  - Unpaid applies sort, then re-groups by date with fixed section ordering, so chips like `OLDEST`/`LARGEST_DUE` feel incorrect.
  - References: `UnpaidOrdersScreen.kt:107`, `UnpaidOrdersScreen.kt:163`.

### P1 high-friction/heavy interaction patterns
- **Too many actions per row**:
  - Rows combine full-card tap, pay button, swipe-to-pay, swipe-to-delete.
  - References: `UnpaidOrdersScreen.kt:140`, `UnpaidOrdersScreen.kt:366`, `UnpaidOrdersSections.kt:271`, `DayDetailSections.kt:168`.
- **Mixed modes in Day Detail rows**:
  - Edit + payment + history + delete on one dense card increases accidental taps and cognitive load.
  - References: `DayDetailSections.kt:239`, `DayDetailSections.kt:248`.
- **Delete behavior is inconsistent**:
  - Unpaid uses simple confirm; Day Detail uses payment-impact dialog.
  - References: `UnpaidOrdersScreen.kt:406`, `DayDetailScreen.kt:540`.

### P2 scan/readability issues
- **Hidden active filtering/search**:
  - Day Detail can hide search UI while query remains active.
  - References: `DayDetailScreen.kt:364`, `DayDetailScreen.kt:392`, `DayDetailScreen.kt:453`.
- **Persisted filter across days creates false-empty states**:
  - `orderFilter` persists between dates.
  - Reference: `DayDetailScreen.kt:119`.
- **Inconsistent hierarchy between screens**:
  - Unpaid emphasizes customer-first while Day Detail is more notes-first; same order looks different.
  - References: `UnpaidOrdersSections.kt:159`, `DayDetailSections.kt:181`.

## UX goal
Create one clear mental model:
- **Day Detail** = planning/editing orders for that date.
- **Unpaid Orders** = collections workspace for money follow-up.

And reduce visual/interaction weight so users can scan and act fast with minimal mis-taps.

## Proposed solution

### 1) Align terminology and filters
- Replace ambiguous `Unpaid` filter in Day Detail with:
  - `Due` (`UNPAID + PARTIAL`) as primary financial filter.
  - Optional secondary `No payment` for strict `UNPAID`.
- Keep wording consistent across Day Detail and Unpaid.

### 2) Preserve order context across screens
- From Unpaid row tap, navigate with `orderId` (deep-link) so Day Detail opens and highlights/focuses that exact order.
- Keep row identity hierarchy consistent (same primary text rule across both screens).

### 3) Reduce action overload
- One primary payment affordance per row (button or swipe, not both active by default).
- Move secondary actions (history/edit/delete) into overflow.
- Keep full-row tap behavior explicit and predictable.

### 4) Make search/filter state always obvious
- Do not allow hidden active query without visible indicator.
- If search is collapsed, either clear query automatically or show persistent active filter chip.
- Reset date-specific filters on date change, or key filters by `dateKey`.

### 5) Fix sort/list logic in Unpaid
- For non-date sorts (`OLDEST`, `LARGEST_DUE`), render a flat list or section order driven by selected sort.
- Keep sticky date headers only when sort is date-based.

### 6) Unify destructive-action safeguards
- Reuse Day Detail payment-impact delete flow in Unpaid, or route delete through one shared flow.
- Add undo/confirm for swipe actions with financial impact.

### 7) Visual weight reduction
- Reduce row chrome and stacked metadata density.
- Separate status and amount into clean, single-line utility row.
- Keep high-contrast emphasis only on due amount and primary action.

## Implementation targets
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailSections.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailModels.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersSections.kt`
- `app/src/main/res/values/strings.xml`

## Acceptance criteria
1. Day Detail and Unpaid use consistent financial state language (`Due`, `Paid`, etc.).
2. Tapping an unpaid row opens Day Detail focused on that specific order, not just the date.
3. Row-level actions are reduced to one primary CTA plus overflow; accidental action rate is reduced in usability test.
4. No hidden active search/filter state exists without visible indicator.
5. Unpaid sort chips produce list ordering that matches user expectation.
6. Delete confirmation/payment-impact messaging is consistent between Day Detail and Unpaid.
7. Users can identify what to do next (edit vs collect payment) within 3 seconds in moderated test.

## Manual verification checklist
1. In Day Detail, enable financial filtering and verify `Due` includes both unpaid and partial orders.
2. In Unpaid, tap a row and confirm app opens the target day with the tapped order focused/highlighted.
3. Toggle search/filter states and confirm no hidden active constraints remain.
4. Switch date on Day Detail and confirm filter behavior is predictable (reset or date-scoped persistence).
5. Test each sort chip in Unpaid and confirm visible list order matches selected sort semantics.
6. Trigger delete from both screens and confirm same warning semantics and recovery options.

## Priority
High (core daily workflow confusion and avoidable action overhead).

## Customer Feedback
1. likes the swipe integration for quick payment in orders ,
2. also it is a given that orders in orders tab are those that have not completed payments so leave orders mostly as is.only fix technical issues if there are.
3. priotize efficiency and UX over functionality.