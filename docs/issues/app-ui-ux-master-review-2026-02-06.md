# App-Wide UI/UX Master Review (2026-02-06)

## Summary
The app has strong operational depth, but UX quality is limited by fragmented navigation, duplicated form flows, and inconsistent feedback patterns. This issue consolidates existing UI/UX issue files and current code findings into one prioritized plan aimed at a top-tier, modern, fast business workflow.

Primary business goal:
- A new or busy staff user should complete common tasks with minimal training and minimal taps:
- `Add order`, `Receive payment`, `Find customer balance`, `Open statement`, `Correct allocation`.

## Evidence base
- Existing review docs in `docs/issues`:
- `docs/issues/iconography-navigation-workflow-review.md`
- `docs/issues/customer-module-ui-ux-review.md`
- `docs/issues/orders-unpaid-toasted-utility-redesign.md`
- `docs/issues/mpesa-import-selection-selects-wrong-items.md`
- `docs/issues/bottom-nav-*.md`
- `docs/issues/insets-topbar-excess-gap.md`
- `docs/issues/bottom-nav-insets-excess-space.md`
- Current UI code audit:
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:117`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt:108`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt:131`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt:72`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt:61`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt:111`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:82`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt:52`

## Critical findings and required fixes

### P0-1) Navigation and orchestration are too centralized
Evidence:
- `MainActivity.kt` is a 1000+ line hub with route logic, permission flows, intent routing, and cross-feature state.
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:117`
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:405`
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:892`

Impact:
- High regression risk.
- Hard to evolve UX consistently across features.
- Top-level flow behavior is harder to reason about and test.

Fix:
- Split navigation into feature graphs (`calendar`, `orders`, `customers`, `money`, `settings`).
- Move transient route context (`manualCustomerId`, `statementCustomerId`, `paymentIntakeText`) into feature-scoped state holders.
- Keep `MainActivity` as shell only (theme, root scaffold, root nav host).

### P0-2) Order entry UX is duplicated and diverging
Evidence:
- Calendar quick-add and day-detail editor duplicate field logic, validation, focus choreography, and voice routing.
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt:420`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt:539`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt:510`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt:640`

Impact:
- Inconsistent behavior risk between add and edit.
- Slower UX iteration and more bug surface.

Fix:
- Build one reusable `OrderEditorSheet` component plus shared `OrderEditorState`.
- Reuse one validation model and one submit pipeline for add/edit.
- Keep only screen-specific wrappers for entry context.

### P0-3) Feedback system is inconsistent and mostly toast-based
Evidence:
- Many actions across money, backup, imports, and history use toasts.
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt:131`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt:195`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt:176`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:118`
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt:194`

Impact:
- Messages are easy to miss.
- No unified action recovery pattern (undo/retry/details).

Fix:
- Standardize to one app-level `SnackbarHost` + inline status cards for long operations.
- Introduce typed `UiEvent` contracts: `Success`, `Error`, `Warning`, `Undoable`.
- Reserve toast only for platform-level edge notifications.

### P1-1) Money and accounts information architecture is still ambiguous
Evidence:
- Top-level `Money` screen mixes intake, manual collection, and statements while also exposing legacy all-customer ledger mode.
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt:21`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt:49`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt:156`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/LedgerScreen.kt:54`

Impact:
- Users must infer differences between `Statements`, `Ledger`, `History`, and `Payments`.
- Increases training cost and wrong-path taps.

Fix:
- Reframe money IA into explicit tasks:
- `Collect` (M-PESA + Manual intake)
- `Allocate` (matching and movement)
- `History` (receipts and actions)
- `Statements` (customer-level financial truth)
- Keep global ledger as admin sub-screen only, not a peer workflow tab.

### P1-2) Key customer actions still depend on long press and overflow
Evidence:
- Row actions open via long press / combined click and bottom sheet.
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:292`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:531`

Impact:
- Discoverability gap for first-time users.
- Slower repeated workflows.

Fix:
- Add visible quick actions on row cards (`Pay`, `New order`, `Message`).
- Keep long press as optional power shortcut, not primary access.

### P1-3) Visual language is inconsistent across modules
Evidence:
- Different card density, type treatment, and tone between Orders, Customers, and Accounting screens.
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:223`
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:525`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt:612`
- Typography uses generic sans serif everywhere.
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/theme/Type.kt:14`

Impact:
- App feels stitched from different design passes.
- Lower perceived quality despite strong features.

Fix:
- Create a small internal design system:
- `AppCard`, `AppHeader`, `AppSection`, `AppInlineStat`, `AppStatusChip`, `AppEmptyState`.
- Standardize spacing, corner radius, elevation, and semantic colors by token.
- Introduce branded font pair (readability-first, performance-safe).

### P2-1) Accessibility and readability need hard constraints
Evidence:
- Repeated horizontally scrollable chip bars and dense control rows across screens.
- `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt:232`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt:399`
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt:480`

Impact:
- Hidden options on small devices.
- Higher cognitive load and lower scan speed.

Fix:
- Enforce minimum touch target and text scaling checks.
- Replace overflow chip strips with segmented groups + `More filters` drawer when count is high.

### P2-2) Copy quality and text encoding issues exist
Evidence:
- Garbled separator in manual payment order picker row text.
- `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt:381`

Impact:
- Reduces trust and product polish.

Fix:
- Add copy pass and encoding guard in CI (UTF-8 integrity check for ui strings).
- Move user-facing strings into centralized resources for consistency and localization readiness.

## Recommended implementation plan

### Phase 1 (1-2 weeks): UX stabilization
- Replace critical toasts with snackbars in payment + backup + import flows.
- Fix copy/encoding bugs (`ManualPaymentScreen` garbled bullet).
- Add visible quick actions on customer rows.
- Align top-level icon/action ordering using one documented map.

### Phase 2 (2-4 weeks): shared UX components and IA cleanup
- Extract shared `OrderEditorSheet`.
- Introduce UI foundation components and spacing/color/type tokens.
- Restructure `Money` into explicit workflow tabs and hide advanced ledger under admin menu.

### Phase 3 (4-8 weeks): architectural hardening + polish
- Decompose `MainActivity` navigation into feature nav graphs.
- Add screen-level state holders for each feature flow.
- Run accessibility, responsiveness, and interaction polish pass.

## Improvement options

### Option A: Quick polish only
- Time: ~1-2 weeks
- Delivers: copy cleanup, feedback consistency, small discoverability fixes
- Tradeoff: core IA and duplication debt remain

### Option B (Recommended): Foundation + workflow modernization
- Time: ~4-8 weeks
- Delivers: top-tier UX baseline with structural maintainability
- Tradeoff: moderate refactor effort

### Option C: Full redesign and re-platformed UI architecture
- Time: ~10-12 weeks
- Delivers: complete visual and interaction reset
- Tradeoff: highest scope and regression management effort

## Acceptance criteria for closure
- Core tasks (`add order`, `record payment`, `open statement`, `fix allocation`) complete in <= 2 primary navigation steps after entering feature.
- At least 80 percent of current toast paths replaced by snackbar/inline event patterns.
- One shared order editor used by calendar quick add and day edit.
- Customer list primary actions visible without long press.
- Money IA labels and routes are unambiguous in usability checks.
- No text encoding artifacts in UI strings.
- Accessibility pass completed for touch target, text scaling, and contrast on key screens.

## Tracking notes
This issue supersedes and consolidates, not deletes, prior focused UI/UX issues. Keep detailed issue files for tactical implementation, but use this file as the master program and prioritization source.
