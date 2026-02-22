package com.zeynbakers.order_management_system.core.tutorial

import androidx.annotation.StringRes
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.navigation.AppRoutes

internal object TutorialCoachTargets {
    const val NavCalendar = "tutorial_nav_calendar"
    const val NavOrders = "tutorial_nav_orders"
    const val NavCustomers = "tutorial_nav_customers"
    const val NavMoney = "tutorial_nav_money"
    const val CalendarAddOrderFab = "tutorial_calendar_add_order_fab"
    const val CalendarMoreButton = "tutorial_calendar_more_button"
    const val OrdersSummaryCard = "tutorial_orders_summary_card"
    const val CustomersControlRow = "tutorial_customers_control_row"
    const val MoneyCollectTab = "tutorial_money_collect_tab"
    const val MoneyRecordTab = "tutorial_money_record_tab"
    const val PaymentHistoryTitle = "tutorial_payment_history_title"
    const val MoreNotesHistoryAction = "tutorial_more_notes_history_action"
    const val MoreHelperAction = "tutorial_more_helper_action"
    const val NotesHistorySearchAction = "tutorial_notes_history_search_action"
    const val HelperEnableSwitch = "tutorial_helper_enable_switch"
}

internal enum class PracticalTutorialAction {
    None,
    OpenCalendar,
    OpenOrders,
    OpenCustomers,
    OpenMoneyCollect,
    OpenMoneyRecord,
    OpenPaymentHistoryAll,
    OpenCalendarMoreSheet,
    OpenNotesHistory,
    OpenHelperSettings
}

internal data class PracticalTutorialStep(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val routePrefix: String? = null,
    val targetId: String? = null,
    @StringRes val primaryActionRes: Int? = null,
    val primaryAction: PracticalTutorialAction = PracticalTutorialAction.None
)

internal fun practicalTutorialSteps(): List<PracticalTutorialStep> {
    return listOf(
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step1_title,
            bodyRes = R.string.tutorial_practical_step1_body,
            routePrefix = AppRoutes.Calendar,
            targetId = TutorialCoachTargets.NavCalendar,
            primaryActionRes = R.string.tutorial_action_open_calendar,
            primaryAction = PracticalTutorialAction.OpenCalendar
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step2_title,
            bodyRes = R.string.tutorial_practical_step2_body,
            routePrefix = AppRoutes.Calendar,
            targetId = TutorialCoachTargets.CalendarAddOrderFab,
            primaryActionRes = R.string.tutorial_action_open_calendar,
            primaryAction = PracticalTutorialAction.OpenCalendar
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step3_title,
            bodyRes = R.string.tutorial_practical_step3_body,
            routePrefix = AppRoutes.Orders,
            targetId = TutorialCoachTargets.NavOrders,
            primaryActionRes = R.string.tutorial_action_open_orders,
            primaryAction = PracticalTutorialAction.OpenOrders
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step4_title,
            bodyRes = R.string.tutorial_practical_step4_body,
            routePrefix = AppRoutes.Orders,
            targetId = TutorialCoachTargets.OrdersSummaryCard
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step5_title,
            bodyRes = R.string.tutorial_practical_step5_body,
            routePrefix = AppRoutes.Customers,
            targetId = TutorialCoachTargets.NavCustomers,
            primaryActionRes = R.string.tutorial_action_open_customers,
            primaryAction = PracticalTutorialAction.OpenCustomers
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step6_title,
            bodyRes = R.string.tutorial_practical_step6_body,
            routePrefix = AppRoutes.Customers,
            targetId = TutorialCoachTargets.CustomersControlRow
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step7_title,
            bodyRes = R.string.tutorial_practical_step7_body,
            routePrefix = AppRoutes.Money,
            targetId = TutorialCoachTargets.NavMoney,
            primaryActionRes = R.string.tutorial_action_open_money,
            primaryAction = PracticalTutorialAction.OpenMoneyCollect
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step8_title,
            bodyRes = R.string.tutorial_practical_step8_body,
            routePrefix = AppRoutes.Money,
            targetId = TutorialCoachTargets.MoneyCollectTab,
            primaryActionRes = R.string.money_tab_collect,
            primaryAction = PracticalTutorialAction.OpenMoneyCollect
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step9_title,
            bodyRes = R.string.tutorial_practical_step9_body,
            routePrefix = AppRoutes.Money,
            targetId = TutorialCoachTargets.MoneyRecordTab,
            primaryActionRes = R.string.money_tab_record,
            primaryAction = PracticalTutorialAction.OpenMoneyRecord
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step10_title,
            bodyRes = R.string.tutorial_practical_step10_body,
            routePrefix = AppRoutes.PaymentHistoryAll,
            targetId = TutorialCoachTargets.PaymentHistoryTitle,
            primaryActionRes = R.string.customer_detail_receipt_history,
            primaryAction = PracticalTutorialAction.OpenPaymentHistoryAll
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step11_title,
            bodyRes = R.string.tutorial_practical_step11_body,
            routePrefix = AppRoutes.Calendar,
            targetId = TutorialCoachTargets.CalendarMoreButton,
            primaryActionRes = R.string.tutorial_action_open_calendar,
            primaryAction = PracticalTutorialAction.OpenCalendar
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step12_title,
            bodyRes = R.string.tutorial_practical_step12_body,
            routePrefix = AppRoutes.Calendar,
            targetId = TutorialCoachTargets.MoreNotesHistoryAction,
            primaryActionRes = R.string.action_more,
            primaryAction = PracticalTutorialAction.OpenCalendarMoreSheet
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step13_title,
            bodyRes = R.string.tutorial_practical_step13_body,
            routePrefix = AppRoutes.NotesHistory,
            targetId = TutorialCoachTargets.NotesHistorySearchAction,
            primaryActionRes = R.string.more_notes_history,
            primaryAction = PracticalTutorialAction.OpenNotesHistory
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step14_title,
            bodyRes = R.string.tutorial_practical_step14_body,
            routePrefix = AppRoutes.Calendar,
            targetId = TutorialCoachTargets.MoreHelperAction,
            primaryActionRes = R.string.action_more,
            primaryAction = PracticalTutorialAction.OpenCalendarMoreSheet
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step15_title,
            bodyRes = R.string.tutorial_practical_step15_body,
            routePrefix = AppRoutes.HelperSettings,
            targetId = TutorialCoachTargets.HelperEnableSwitch,
            primaryActionRes = R.string.more_floating_helper,
            primaryAction = PracticalTutorialAction.OpenHelperSettings
        ),
        PracticalTutorialStep(
            titleRes = R.string.tutorial_practical_step16_title,
            bodyRes = R.string.tutorial_practical_step16_body
        )
    )
}

internal fun routeMatchesPrefix(currentRoute: String?, routePrefix: String?): Boolean {
    if (routePrefix == null) return true
    val route = currentRoute ?: return false
    return route == routePrefix ||
        route.startsWith("$routePrefix/") ||
        route.startsWith("$routePrefix?") ||
        route.startsWith(routePrefix)
}

