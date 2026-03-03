# App Navigation Shell UI/UX Issues

Status: Open

## Scope
- Top-level shell and More actions
- Primary files:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`

## Owner goals
- Move between core workflows quickly.
- Access advanced tools without hunting.
- Keep orientation across tabs and deep screens.

## Open issues

### P1
- [Fixed 2026-02-26] More sheet actions are now grouped by workflow (`Daily work`, `System setup`, `Learning`) for faster scanning.
- [Fixed 2026-02-26] Cross-tab back behavior now uses root-reset tab navigation to stay consistent and predictable.
- [Fixed 2026-02-26] Advanced tools now use clearer owner-focused labels with supporting descriptions.

### P2
- No customizable quick actions for each business owner preference.
- Navigation state memory (last sub-view per tab) can be improved.

## Acceptance criteria
- Top-level movement feels consistent in all tabs.
- More actions are grouped and self-explanatory.
- Back behavior matches user expectation in all major paths.
