package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.zeynbakers.order_management_system.AppCalendarCallbacks
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppOrdersState
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.UnpaidOrdersScreen

internal fun NavGraphBuilder.ordersGraph(
    navController: NavHostController,
    orderViewModel: OrderViewModel,
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
            onBack = { navController.popBackStack() },
            onOpenDay = { date ->
                calendarCallbacks.onSelectedDateChange(date)
                navController.navigate(AppRoutes.day(date))
            },
            onReceivePayment = { order ->
                navigationActions.navigateToMoneyRecord(order.customerId)
            },
            onDeleteOrder = { order ->
                orderViewModel.cancelOrder(order.id, order.orderDate)
            },
            title = "Orders",
            showBack = false
        )
    }
}
