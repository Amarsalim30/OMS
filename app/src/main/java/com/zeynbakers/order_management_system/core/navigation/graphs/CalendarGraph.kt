package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.AppCalendarCallbacks
import com.zeynbakers.order_management_system.AppCalendarState
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppFeatureSupportActions
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import com.zeynbakers.order_management_system.core.widget.WidgetUpdater
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import kotlinx.datetime.LocalDate

internal fun NavGraphBuilder.calendarGraph(
    navController: NavHostController,
    orderViewModel: OrderViewModel,
    calendarState: AppCalendarState,
    calendarCallbacks: AppCalendarCallbacks,
    navigationActions: AppFeatureNavigationActions,
    supportActions: AppFeatureSupportActions
) {
    composable(AppRoutes.Calendar) {
        CalendarScreen(
            days = calendarState.calendarDays,
            currentYear = calendarState.currentYear,
            currentMonth = calendarState.currentMonth,
            baseYear = calendarState.baseYear,
            baseMonth = calendarState.baseMonth,
            monthSnapshots = calendarState.monthSnapshots,
            monthTotal = calendarState.monthTotal,
            monthBadgeCount = calendarState.monthBadgeCount,
            selectedDate = calendarState.selectedDate,
            onSelectDate = { calendarCallbacks.onSelectedDateChange(it) },
            onOpenDay = { date ->
                calendarCallbacks.onSelectedDateChange(date)
                navController.navigate(AppRoutes.day(date))
            },
            onSaveOrder = { date, notes, total, name, phone, pickupTime ->
                orderViewModel.saveOrder(
                    date = date,
                    notes = notes,
                    totalAmount = total,
                    customerName = name,
                    customerPhone = phone,
                    pickupTime = pickupTime,
                    existingOrderId = null
                )
                WidgetUpdater.enqueue(navController.context)
                NotificationScheduler.enqueueNow(navController.context)
            },
            searchCustomers = { query -> orderViewModel.searchCustomers(query) },
            onSummaryClick = { navController.navigate(AppRoutes.Summary) },
            onMonthSettled = { year, month -> calendarCallbacks.onMonthSettled(year, month) },
            openQuickAddDate = calendarState.quickAddDate,
            onQuickAddConsumed = { calendarCallbacks.onQuickAddDateChange(null) }
        )
    }

    composable(
        route = AppRoutes.Day,
        arguments = listOf(navArgument(AppRoutes.ARG_DATE) { type = NavType.StringType })
    ) { entry ->
        val dateArg = entry.arguments?.getString(AppRoutes.ARG_DATE)
        val date =
            dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: supportActions.currentDate()
        LaunchedEffect(date) {
            calendarCallbacks.onSelectedDateChange(date)
            orderViewModel.loadOrdersForDate(date)
        }
        DayDetailScreen(
            date = date,
            orders = calendarState.ordersForDate,
            dayTotal = calendarState.dayTotal,
            customerNames = calendarState.orderCustomerNames,
            orderPaidAmounts = calendarState.orderPaidAmounts,
            onBack = { navController.popBackStack() },
            onSaveOrder = { notes, total, name, phone, pickupTime, orderId ->
                orderViewModel.saveOrder(
                    date = date,
                    notes = notes,
                    totalAmount = total,
                    customerName = name,
                    customerPhone = phone,
                    pickupTime = pickupTime,
                    existingOrderId = orderId
                )
                WidgetUpdater.enqueue(navController.context)
                NotificationScheduler.enqueueNow(navController.context)
            },
            onDeleteOrder = { orderId ->
                orderViewModel.cancelOrder(orderId, date)
                WidgetUpdater.enqueue(navController.context)
                NotificationScheduler.enqueueNow(navController.context)
            },
            loadOrderPaymentAllocations = { orderId ->
                orderViewModel.loadOrderPaymentAllocations(orderId)
            },
            loadMoveOrderOptions = { customerId, excludeOrderId ->
                orderViewModel.loadMoveOrderOptions(customerId, excludeOrderId)
            },
            onDeleteOrderWithPayments = { orderId, day, allocationIds, action, target, moveFull ->
                val result =
                    orderViewModel.deleteOrderWithPayments(
                        orderId = orderId,
                        date = day,
                        allocationIds = allocationIds,
                        action = action,
                        target = target,
                        moveFullReceipts = moveFull
                    )
                if (result) {
                    WidgetUpdater.enqueue(navController.context)
                    NotificationScheduler.enqueueNow(navController.context)
                }
                result
            },
            onOrderPaymentHistory = { orderId ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Order(orderId), null)
            },
            onReceivePayment = { order ->
                navigationActions.navigateToMoneyRecord(order.customerId)
            },
            loadCustomerById = { id -> orderViewModel.getCustomerById(id) },
            searchCustomers = { query -> orderViewModel.searchCustomers(query) },
            draft = calendarState.dayDrafts[date],
            onDraftChange = { updated ->
                if (updated == null) {
                    calendarState.dayDrafts.remove(date)
                } else {
                    calendarState.dayDrafts[date] = updated
                }
            }
        )
    }

    composable(AppRoutes.Summary) {
        SummaryScreen(
            monthLabel = supportActions.monthLabel(calendarState.currentYear, calendarState.currentMonth),
            monthTotal = calendarState.monthTotal,
            initialDate =
                calendarState.summaryDate ?: calendarState.selectedDate ?: supportActions.currentDate(),
            orders = calendarState.summaryOrders,
            rangeTotal = calendarState.summaryTotal,
            customerNames = calendarState.summaryCustomerNames,
            onAnchorDateChange = { updated -> calendarCallbacks.onSummaryDateChange(updated) },
            onLoadRange = { start, end ->
                orderViewModel.loadSummaryRange(startInclusive = start, endExclusive = end)
            },
            onBack = { navController.popBackStack() }
        )
    }
}
