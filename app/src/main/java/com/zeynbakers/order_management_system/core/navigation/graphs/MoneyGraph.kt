package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.AppAccountsCallbacks
import com.zeynbakers.order_management_system.AppAccountsState
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppFeatureSupportActions
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.ui.MoneyScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel

internal fun NavGraphBuilder.accountsGraph(
    navController: NavHostController,
    customerViewModel: CustomerAccountsViewModel,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    paymentHistoryViewModel: PaymentIntakeHistoryViewModel,
    ledgerViewModel: LedgerViewModel,
    accountsState: AppAccountsState,
    accountsCallbacks: AppAccountsCallbacks,
    navigationActions: AppFeatureNavigationActions,
    supportActions: AppFeatureSupportActions
) {
    composable(AppRoutes.Money) {
        MoneyScreen(
            selectedTab = accountsState.moneyTab,
            onTabChange = { accountsCallbacks.onMoneyTabChange(it) },
            paymentIntakeViewModel = paymentIntakeViewModel,
            customerViewModel = customerViewModel,
            ledgerViewModel = ledgerViewModel,
            initialText = accountsState.paymentIntakeText,
            manualCustomerId = accountsState.manualCustomerId,
            statementCustomerId = accountsState.statementCustomerId,
            onManualContextConsumed = {
                accountsCallbacks.onManualCustomerIdChange(null)
            },
            onStatementContextConsumed = {
                accountsCallbacks.onStatementCustomerIdChange(null)
            },
            onManualSaved = { supportActions.refreshAfterPayments() },
            onApplied = {
                accountsCallbacks.onPaymentIntakeTextChange(null)
                supportActions.refreshAfterPayments()
            },
            onAppliedInPlace = { supportActions.refreshAfterPayments() },
            onOpenReceiptHistory = { receiptId ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.All, receiptId)
            }
        )
    }

    composable(
        route = "${AppRoutes.PaymentHistoryAll}?${AppRoutes.ARG_FOCUS_RECEIPT_ID}={${AppRoutes.ARG_FOCUS_RECEIPT_ID}}",
        arguments = listOf(
            navArgument(AppRoutes.ARG_FOCUS_RECEIPT_ID) {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { entry ->
        val focusId =
            entry.arguments
                ?.getLong(AppRoutes.ARG_FOCUS_RECEIPT_ID)
                ?.takeIf { it > 0 }
        PaymentIntakeHistoryScreen(
            viewModel = paymentHistoryViewModel,
            filter = PaymentHistoryFilter.All,
            focusReceiptId = focusId,
            onBack = { navController.popBackStack() },
            onRemoved = { supportActions.refreshAfterPayments() }
        )
    }

    composable(
        route = AppRoutes.PaymentHistoryCustomer,
        arguments = listOf(navArgument(AppRoutes.ARG_CUSTOMER_ID) { type = NavType.LongType })
    ) { entry ->
        val customerId = entry.arguments?.getLong(AppRoutes.ARG_CUSTOMER_ID) ?: return@composable
        PaymentIntakeHistoryScreen(
            viewModel = paymentHistoryViewModel,
            filter = PaymentHistoryFilter.Customer(customerId),
            focusReceiptId = null,
            onBack = { navController.popBackStack() },
            onRemoved = {
                supportActions.refreshAfterPayments()
                customerViewModel.loadCustomer(customerId)
            }
        )
    }

    composable(
        route = AppRoutes.PaymentHistoryOrder,
        arguments = listOf(navArgument(AppRoutes.ARG_ORDER_ID) { type = NavType.LongType })
    ) { entry ->
        val orderId = entry.arguments?.getLong(AppRoutes.ARG_ORDER_ID) ?: return@composable
        PaymentIntakeHistoryScreen(
            viewModel = paymentHistoryViewModel,
            filter = PaymentHistoryFilter.Order(orderId),
            focusReceiptId = null,
            onBack = { navController.popBackStack() },
            onRemoved = { supportActions.refreshAfterPayments() }
        )
    }
}
