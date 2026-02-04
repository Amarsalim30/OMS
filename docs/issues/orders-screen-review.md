# Orders Screen Review Issues (Refined)

Scope: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`

## ✅ Resolved & Implemented
*   **Fast Triage:** "Due" amount is now bold, right-aligned, and visually distinct.
*   **Card Density:** Moved to a modern, high-density card layout with avatars.
*   **Action Fatigue:** "Pay" button is now a contextual prompt, not a full-width primary button.
*   **Verbose Notes:** Notes are now single-line with ellipsis to preserve vertical space.
*   **Progress Bar Noise:** Progress bars are now conditional or subtle.
*   **Sticky Headers:** Redesigned with clear separation, uppercase typography, and status icons (e.g., Warning for Overdue).
*   **Walk-in Visibility:** Walk-ins now generate a specific "Walk-in" avatar or initials.
*   **Overdue Indicators:** Added explicit icons and red styling to overdue headers.

## 🚀 Pending "Top Tier" Refinements (Next Steps)

### 1. Motion & Interaction Design
*   **List Animation:** Implement `animateItemPlacement()` in `LazyColumn` so that changing filters (e.g., "Largest Due" to "Overdue") animates the reordering smoothly instead of snapping.
*   **Haptic Feedback:** Add `HapticFeedbackType.LongPress` or `TextHandleMove` vibration on the "Pay" button for tactile confirmation of transactions.
*   **Shared Elements:** Use shared element transitions for the Avatar when navigating from the list to the Day Detail or Customer Detail screen.

### 2. Advanced Efficiency Features
*   **Swipe Gestures:** Implement `SwipeToDismiss` or generic swipe actions on list rows:
    *   **Swipe Right:** Quick Pay (fills default amount).
    *   **Swipe Left:** Remind Customer (opens SMS/WhatsApp intent).
*   **Search Capability:** The current `FilterChip` row is good, but a search icon in the Top Bar is missing. Users often need to find "Jane's order" instantly without scrolling.

### 3. Visual Polish
*   **Money Privacy:** Add a "Hide Balances" eye toggle in the top bar (common in fintech apps) for using the app in public/busy shop environments.
*   **Dynamic Empty States:** The empty state is static. A playful, animated vector (using `Lottie` or dynamic Compose drawing) for "All Paid Up" would delight the user.
*   **Skeleton Loading:** If the database query takes time (unlikely but possible), show a shimmer skeleton instead of a blank screen or spinner.

### 4. Bulk Operations
*   **Batch Actions:** Long-press to select multiple orders, enabling a "Remind All" or "Batch Pay" action for high-volume days.

