Revised UI Refactoring Plan: Feature-Based Organization
Organize the order/ui package by feature into sub-packages to improve clarity and maintainability,
while keeping major components decoupled and screens lean.
Proposed Changes
[New Package Structure]
Create the following sub-packages under com.zeynbakers.order_management_system.order.ui:
•
calendar/: Core calendar screen and its specific components.
•
day_detail/: Day detail view, including filters, stats, and dialogs.
•
order_editor/: All components related to the order creation/editing flow.
•
summary/: Summary screen and reporting components.
•
unpaid/: Unpaid orders tracking.
•
common/: Shared UI models and utilities (e.g., PaymentState, OrderItemDraft).
[Feature Reorganization]
[Calendar Feature]
•
Move to ui/calendar/: CalendarScreen.kt, CalendarViewModel.kt (if applicable),
CalendarTutorialOverlay.kt.
•
Move to ui/calendar/components/:
◦
CalendarDayCellModern.kt -> DayCell.kt
◦
CalendarScreenSections.kt -> CalendarComponents.kt
•
Move to ui/calendar/util/: CalendarDateUtils.kt.
[Day Detail Feature]
•
Move to ui/day_detail/: DayDetailScreen.kt.
•
Move to ui/day_detail/components/:
◦
DayDetailSections.kt -> DayDetailComponents.kt
◦
DayDetailDialogs.kt -> DayDetailDialogs.kt
◦
DayDetailImportDialogs.kt -> DayDetailImportDialogs.kt
•
Move to ui/day_detail/models/: DayDetailModels.kt.
[Order Editor Feature]
•
Move to ui/order_editor/: OrderEditorSheet.kt.
•
Move to ui/order_editor/components/:
◦
OrderEditorFields.kt
◦
OrderEditorInput.kt
◦
OrderEditorCustomerSection.kt
◦
OrderCartSummary.kt
◦
AddProductBottomSheet.kt
•
Move to ui/order_editor/dialogs/: OrderPrintDialogs.kt.
[Summary & Unpaid Features]
•
Move to ui/summary/: SummaryScreen.kt, SummarySections.kt (Keep separate if large, otherwise merge).
•
Move to ui/unpaid/: UnpaidOrdersScreen.kt, UnpaidOrdersSections.kt.
[Common UI Models]
•
Move to ui/common/:
◦
OrderItemDraft.kt
◦
CalendarDayUi.kt (Contains PaymentState and CalendarDayUi)
◦
OrderUiState.kt (Review if needed)
[Trivial Merges]
•
Merge SummarySections.kt into SummaryScreen.kt if it contains only a few small components.
•
Merge UnpaidOrdersSections.kt into UnpaidOrdersScreen.kt.
Refactoring Steps

1.

Preparation: Create the directory structure.

2.

Move & Rename: Move files to their new locations, renaming them to be more concise (e.g.,
CalendarDayCellModern.kt to DayCell.kt inside the calendar package).

3.

Update Imports: Systematic update of all imports across the project.

4.

Verification: Rebuild the project and run tests.
Verification Plan
Automated Tests
•
Run existing unit tests:
./gradlew testDebugUnitTest
Manual Verification
•
Verify the app compiles and runs.
•
Check navigation and UI state persistence.
•
Ensure all dialogs and bottom sheets in the Order Editor still function as expected.