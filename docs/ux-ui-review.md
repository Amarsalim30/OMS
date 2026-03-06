# UX / UI Review

## Scope
Review of interaction quality, feedback states, data-entry efficiency, and accessibility basics for the current live flows.

## Current state
- The live order-entry flow is centered on `OrderEditorInput.kt` and `OrderEditorSheet.kt`.
- Legacy `OrderEditor.kt` and `OrderCard.kt` were not part of the live path and were removed in this pass.
- Customer suffix parsing in order notes is now stricter, which removes a real source of misleading typeahead suggestions.
- The app already has meaningful feature depth and practical operator workflows.

## Findings

### U1. Order-entry customer typeahead defect was fixed in this pass
- Severity: high
- Files:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorInput.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/order/ui/OrderEditorInputTest.kt`
- Why it matters: a fast data-entry flow must not turn trailing product or amount text into a customer suggestion.
- Recommended fix: restrict customer query extraction to the actual trailing suffix only.
- Implement now or later: implemented now
- Risk: low
- Status: implemented and regression-tested

### U2. Loading, empty, and error patterns still work but are not fully standardized across flows
- Severity: medium
- Files: multiple screens across order, customer, accounting, and settings flows
- Why it matters: inconsistent wording and visual treatment increase user hesitation, especially in error recovery paths.
- Recommended fix: continue converging on shared empty and error-state patterns as screens are touched.
- Implement now or later: later
- Risk: low-medium
- Status: open

### U3. Accessibility still needs a device-backed pass on critical flows
- Severity: medium
- Files and areas: auth, backup or restore, overlay permissions, order entry, payment intake
- Why it matters: focus order, keyboard flow, content descriptions, and permission rationale quality cannot be fully proven by unit tests or lint alone.
- Recommended fix: run a focused accessibility pass on a device before GA.
- Implement now or later: later
- Risk: low-medium
- Status: open

### U4. Permission-sensitive and recovery-sensitive flows still need manual polish verification
- Severity: medium
- Files and areas: licensing gate, backup or restore, helper overlay permission flow, microphone permission flow
- Why it matters: the most trust-sensitive user journeys are the ones most likely to fail in real usage if copy, timing, or fallback behavior is unclear.
- Recommended fix: run manual acceptance scenarios on a device and capture any friction in issue docs before release.
- Implement now or later: later
- Risk: medium
- Status: open

## What is working well
- Feature coverage is strong and grounded in real operational workflows.
- Navigation is already more structured than the initial audit implied because the app shell and feature graphs are split.
- Order entry now behaves more predictably for customer suffix handling.
- Dead legacy UI files were removed, reducing confusion for future UX iterations.

## UX priorities before GA
1. Run a device-backed pass across auth, licensing, backup or restore, and permission-request flows.
2. Standardize remaining empty, loading, and error wording in the screens touched most often by operators.
3. Add or verify accessibility labels and focus order on critical task flows rather than attempting a broad cosmetic redesign.
