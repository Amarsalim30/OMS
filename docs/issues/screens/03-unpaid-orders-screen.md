# Unpaid Orders Screen UI/UX Issues

Status: Implemented (redundant header removal + context clarity pass)

## Scope
- Route: `orders`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`

## Owner goals
- Prioritize payment follow-ups quickly.
- Take payment action immediately.
- Protect privacy when showing balances publicly.

## Implemented pass (2026-02-18)

### P1
- Removed redundant `Collections queue` owner header card.
  - Reason: it duplicated the purpose of the existing Outstanding Summary card and reduced visible list space.
- Kept Outstanding Summary as the single primary top summary.
- Added active context pill below sort chips:
  - `Filter <state>`
  - `Filter <state> - Search "<query>"`
  - Improves orientation while switching sort/search quickly.
- Improved empty search feedback with query-specific copy:
  - `No results for "<query>".`
- Tightened top-level list spacing slightly to increase visible rows per screen.

### Residual gaps
- No bulk follow-up actions yet (message/remind multi-select still pending).
- Date headers still do not provide jump controls.

Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`
- `app/src/main/res/values/strings.xml`

## Acceptance criteria
- [x] Redundant collections header removed in favor of single summary card.
- [x] Current filter/search context is visible without extra taps.
- [x] Empty search state clearly reflects the active query.
- [ ] Follow-up actions (message/remind) are directly available.
- [ ] Priority sort mode can be set and retained as default.
