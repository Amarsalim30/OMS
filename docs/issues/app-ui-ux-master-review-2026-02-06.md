# App-Wide UI/UX Master Review (2026-02-06, implementation refresh 2)

## Summary
This implementation wave materially improved app UX foundations: feature navigation is modularized, shared UI primitives are active, copy localization coverage expanded, progressive filters now cover multiple high-traffic surfaces, and regression tests were expanded.

The largest remaining UX engineering risk is still orchestration/screen size in a few core files, especially `MainActivity`, `CalendarScreen`, and `DayDetailScreen`.

## Evidence base
- Existing issue docs in `docs/issues`.
- Code audit + implementation verification on 2026-02-06.
- Validation commands passed:
- `./gradlew assembleDebug`
- `./gradlew assembleDebugAndroidTest`
- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- Lint summary: `0 errors, 18 warnings, 8 hints`

## Implemented in this pass

### Done-1) Feature nav graph split is complete
Evidence:
- Host reduced to wiring-only: `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt`
- Feature graphs:
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CalendarGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OrdersGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/CustomersGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/MoneyGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`

Impact:
- Feature route changes are now isolated by graph module.

### Done-2) Main activity extraction progressed further
Evidence:
- Navigation helper extraction: `app/src/main/java/com/zeynbakers/order_management_system/AppNavigationHelpers.kt`
- Contact loading extraction: `app/src/main/java/com/zeynbakers/order_management_system/ContactsLoader.kt`
- Dialog extraction: `app/src/main/java/com/zeynbakers/order_management_system/MainDialogs.kt`
- `MainActivity` reduced from prior ~737 lines to ~561 lines.

Impact:
- Lower local complexity and clearer orchestration boundaries.

### Done-3) Money wording/copy map + dialog copy standardized
Evidence:
- Money copy map: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MoneyCopy.kt`
- Centralized dialog strings in resources: `app/src/main/res/values/strings.xml`
- Resource-based dialogs: `app/src/main/java/com/zeynbakers/order_management_system/MainDialogs.kt`

Impact:
- Better terminology consistency and localization readiness.

### Done-4) Shared UI primitives are in place and used
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppCard.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppSection.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppEmptyState.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppFilterRow.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppSpacing.kt`

Impact:
- Shared spacing/surface/filter patterns reduce drift across feature screens.

### Done-5) Progressive filter UX now covers additional dense surfaces
Evidence:
- Payment history move-allocation filters: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeHistoryScreen.kt`
- Statement range filters: `app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/CustomerStatementsScreen.kt`
- Customer filters: `app/src/main/java/com/zeynbakers/order_management_system/customer/ui/CustomerListScreen.kt`
- Day-detail order filters: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`

Impact:
- Better discoverability on narrow screens via primary-visible + more-filters interaction.

### Done-6) Localization coverage expanded significantly
Evidence:
- Resource additions: `app/src/main/res/values/strings.xml`
- Core flow migration in money/customer/history/manual screens.
- `stringResource(...)` usage now ~95 call sites (up from baseline 14 in this program).

Impact:
- Much stronger copy governance and localization path for critical workflows.

### Done-7) UI regression coverage expanded
Evidence:
- `app/src/androidTest/java/com/zeynbakers/order_management_system/customer/ui/CustomerListActionsTest.kt`
- `app/src/androidTest/java/com/zeynbakers/order_management_system/core/ui/components/AppFilterRowTest.kt`
- `app/src/androidTest/java/com/zeynbakers/order_management_system/order/ui/CalendarMonthScreenTest.kt` (compatibility updates)

Impact:
- Better guardrails for customer primary actions and progressive filter behavior.

### Done-8) Lint hygiene cleanup for DS/resources/shortcuts completed
Evidence:
- DS composable API order fixed:
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppEmptyState.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppFilterRow.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/components/AppSection.kt`
- Resource cleanup:
- `app/src/main/res/values/strings.xml` (`money_result_count` converted to plurals, unused resource removed)
- Shortcut usage reporting:
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppShortcuts.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt`

Impact:
- Reduced lint noise and improved API consistency/localization hygiene.

## Highest-priority remaining issues

### P0-1) `MainActivity` is improved but still large
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt` (~561 lines).

Impact:
- Still a wide regression surface for app-wide flow updates.

Required fix:
- Extract coordinator/state assembly (`calendarState`, `customersState`, callbacks, navigation actions) into dedicated coordinator files.

### P1-1) Core order screens are still oversized
Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt` (~1202 lines)
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/CalendarScreen.kt` (still large)

Impact:
- Harder targeted previews, focused testing, and safe iterative UX changes.

Required fix:
- Continue section extraction into dedicated files with pure props/callbacks.

### P1-2) DS adoption remains partial
Evidence:
- New DS primitives exist, but not all high-traffic cards/headers/empty states use them.

Impact:
- Residual visual inconsistency across modules.

Required fix:
- Apply DS components systematically in orders/calendar/intake surfaces.

### P2-1) Accessibility checklist automation still incomplete
Evidence:
- Progressive filter behavior is tested, but no dedicated text-scale/touch-target UI checks yet.

Impact:
- Remaining risk for small-screen/high-font-scale usability regressions.

Required fix:
- Add explicit accessibility smoke tests for key flows.

## Refined Option B status

### Phase 1 (1-2 weeks): consistency hardening
- Standardize event handling contract (`UiEvent`) and snackbar routing. [done]
- Normalize Money/Accounts labels and key task names. [done]
- Add accessibility checklist gates for key flows. [pending]

### Phase 2 (2-4 weeks): structural UX modernization
- Split large screens into section composables and reusable primitives. [partially done]
- Introduce internal design-system components and spacing/type tokens. [done] (baseline)
- Expand string resource migration for workflow copy. [done] (core money/customer/history flows)

### Phase 3 (4-8 weeks): architecture and quality scale-up
- Split feature nav graphs and reduce `MainActivity` orchestration scope. [partially done]
- Add targeted UI tests for critical workflows and regressions. [partially done]
- Run polish pass for hierarchy, motion, responsiveness. [pending]

## Updated acceptance criteria
- Core workflows complete in <=2 primary steps after entering a feature. [partially met]
- Shared feedback contract is used by payment, customer, backup, and import flows. [met]
- Shared order editor remains the single source for add/edit forms. [met]
- Customer row primary actions remain visible without long press. [met]
- Money labels are consistent across nav, tabs, and primary titles. [partially met]
- Key workflow copy is resource-backed for localization. [met for core money/customer/history flows]
- Accessibility checks completed for touch target, text scaling, and filter discoverability. [not met]

## Tracking note
This file remains the master UI/UX program document. Update this file first when status changes, then cascade updates into tactical issue files.
