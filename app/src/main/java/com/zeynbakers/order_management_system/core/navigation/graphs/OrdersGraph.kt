package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.zeynbakers.order_management_system.AppCalendarCallbacks
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppOrdersState
import com.zeynbakers.order_management_system.MoneyRecordContext
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.order.ui.calendar.CalendarViewModel
import com.zeynbakers.order_management_system.order.ui.unpaid.UnpaidOrdersScreen
import java.math.BigDecimal

internal fun NavGraphBuilder.ordersGraph(
    navController: NavHostController,
    orderViewModel: CalendarViewModel,
    ordersState: AppOrdersState,
    calendarCallbacks: AppCalendarCallbacks,
    navigationActions: AppFeatureNavigationActions
) {
    composable(AppRoutes.Orders) {
        LaunchedEffect(Unit) { orderViewModel.loadUnpaidOrders() }
        UnpaidOrdersScreen(
            orders = ordersState.unpaidOrders,
            paidAmounts = ordersState.unpaidPaidAmounts,
            customerNames = ordersState.unpaidCustomerNames,
            customerPhones = ordersState.unpaidCustomerPhones,
            onBack = { navController.popBackStack() },
            onOpenDay = { date, orderId ->
                calendarCallbacks.onSelectedDateChange(date)
                navController.navigate(AppRoutes.day(date, orderId))
            },
            onReceivePayment = { order ->
                val paid = ordersState.unpaidPaidAmounts[order.id] ?: BigDecimal.ZERO
                val outstanding = (order.totalAmount - paid).max(BigDecimal.ZERO)
                navigationActions.navigateToMoneyRecord(
                    MoneyRecordContext(
                        customerId = order.customerId,
                        orderId = order.id,
                        outstandingAmount = outstanding
                    )
                )
            },
            onDeleteOrder = { order ->
                orderViewModel.cancelOrder(order.id, order.orderDate)
            },
            title = stringResource(R.string.nav_orders),
            showBack = false
        )
    }
}
