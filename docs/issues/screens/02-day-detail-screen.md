# Day Detail Screen UI/UX Issues

Status: Implemented (owner triage + safer delete flow + fast input UX)

## Scope
- Route: `day/{date}`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`

## Owner goals
- Triage day orders quickly.
- Edit with minimal taps.
- Record or follow up payments fast.

## Implemented fix (2026-02-17)

### P1
- Reduced top control density:
  - Search is now collapsible by default to keep more orders visible.
  - Active list state is now explicit with compact context text:
    - `Filter <state>`
    - `Search "<query>"`
    - combined filter + search context.
- Improved action scan on each order row:
  - Removed isolated trailing delete icon.
  - Grouped row actions into a single consistent action line (History, Record payment, Delete).
  - Added a small "tap card to edit" cue for quicker mental mapping.
- Simplified add/edit sheet for speed-first data entry:
  - Core and optional fields are directly visible (no section toggle needed).
  - Reduced context switching for operators entering many orders back-to-back.
- Refined editor field order for faster entry:
  - `Customer -> Notes -> Total -> Pickup/Delivery time`
  - Keyboard `Next` actions follow the same order to reduce thumb travel.

### P2
- Delete-with-payments flow now includes a plain-language impact summary before confirm:
  - no-linked-payments case,
  - no-selection case,
  - move-target-required case,
  - move summary with selected count/amount/target,
  - void summary with selected count/amount.

Evidence:
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailSections.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailDialogs.kt`
- `app/src/main/res/values/strings.xml`

## Input UX Review (2026-02-17)

### What is working
- Fast-entry order now matches operator workflow: `Customer -> Notes -> Total -> Pickup/Delivery time`.
- Keyboard progression now follows the same sequence end-to-end for lower tap count.
- Save gating is reliable (`Save` enabled only when required input is valid).
- Pickup/delivery field wording and placeholder now better reflect accepted formats.
- Search + filter context chip improves confidence in current list state.

### Implemented input refinements

### P1
- Added confirmation before full-form clear to prevent accidental data loss during fast entry.
- Added one-tap `Clear optional` action (customer + pickup/delivery time) without touching required fields.
- Removed hard block that required phone when customer name is typed:
  - name-only entry is allowed,
  - order still saves quickly,
  - customer linkage occurs when valid phone is provided.

### P2
- Added debounce to customer suggestions lookup to reduce jumpy updates and unnecessary queries while typing.
- Reduced amount validation noise by only showing the inline invalid helper when no explicit error is active.
- Search hide/show now preserves current query and filtered state instead of clearing it.

### Residual risks
- Name-only customer entries are faster but may increase duplicate customer records if phone capture is skipped repeatedly.
- Debounce timing may need minor tuning after device-level testing.

## Acceptance criteria
- [x] Owner can filter and find a target order in under 5 seconds.
- [x] Editor supports fast sequence entry: `Customer -> Notes -> Total -> Pickup/Delivery time`.
- [x] Editor protects against accidental full-clear and supports quick optional-field reset.
- [x] Customer suggestions feel stable during typing (debounced).
- [x] Destructive flows include plain-language summaries of impact.

## Polished UX (2026-02-18)
- Added animation to search bar visibility for smoother feel.
- Added key to order list items for better performance and state stability.
- Enhanced Day Summary Card to highlight balance with primary color when positive.
- Refined "Tap to edit" hint to be italic for subtle guidance.

## Planned + Implemented UX Pass (2026-02-18)

### Plan
- Remove duplicate close actions inside editor (bottom `Cancel` vs top `X`).
- Reduce action bar height and keep only core actions for compact screen use.
- Temporarily disable `Save + Next` action in editor footer.
- Make form visually shorter by tightening input placement:
  - keep customer search in customer section,
  - move pickup time next to total amount in required section,
  - keep notes full-width for readability.

### Implemented
- Removed bottom `Cancel`; top-right `X` is the single close affordance.
- Removed `Save + Next` button and its code path from the shared editor footer/workflow.
- Compact footer now uses one row: `Clear` (secondary) + `Save` (primary).
- Repositioned pickup time to appear earlier in the form flow.
- Added short time label (`Time`) to reduce visual density and improve scan speed.

## Refined Form UX Pass (2026-02-18)

### Applied improvements
- Reordered operator flow to better match entry intent:
  - `Customer search -> Pickup time -> Notes -> Total -> Save`.
- Moved pickup time into the customer/schedule section so schedule is captured earlier.
- Added quick pickup chips (`09:00`, `12:00`, `15:00`, `18:00`) for fewer taps.
- Updated customer field IME flow to move next focus toward pickup time.
- Made total field full-width and visually stronger for financial clarity.
- Reworked amount chips to additive quick-entry increments (`+100`, `+500`, `+1000`).
- Added sticky footer total summary (`Total: KSh ...`) above actions.
- Added mode-aware initial focus: new form starts at customer, edit mode starts at notes.
- Kept compact action footer (`Clear` + `Save`) with single top close affordance.
