# Orders Screen Review Issues (Refined)

Scope: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`

## Resolved And Implemented
- Fast triage: due amount remains bold, right-aligned, and visually distinct.
- Card density: high-density row layout with avatar + compact details.
- Action fatigue: pay is contextual, not a full-width dominant CTA.
- Verbose notes: notes are constrained with ellipsis.
- Progress bar noise: reduced via subtle, compact indicator.
- Sticky headers: clear visual separation and overdue signal.
- Walk-in visibility: explicit fallback identity.
- Overdue indicators: icon + warning color coding.
- Swipe-to-pay: start-to-end swipe triggers quick pay action.
- Search discoverability: top bar search action is present.
- Privacy mode: eye toggle hides/shows amounts on summary and rows.
- Reorder motion: list rows now use `animateItem()` for smoother filter reorder transitions.

## Pending Top-Tier Refinements
### 1. Motion and Interaction
- Shared element transition from row/avatar into day detail or customer detail.

### 2. Efficiency Features
- Add second-side swipe action:
- Swipe left should open remind customer (SMS/WhatsApp intent).
- Add optional inline "Remind" action for overdue rows.

### 3. Visual Polish
- Upgrade empty state from static to dynamic (animation/illustration).
- Add loading skeletons for slow data fetch scenarios.

### 4. Bulk Operations
- Long-press multi-select with batch actions (`Remind all`, `Batch pay`).

