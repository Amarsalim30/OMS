package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.AppCustomersCallbacks
import com.zeynbakers.order_management_system.AppCustomersState
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppFeatureSupportActions
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.customer.ui.ImportContactsScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerStatementScreen

internal fun NavGraphBuilder.customersGraph(
    navController: NavHostController,
    customerViewModel: CustomerAccountsViewModel,
    customersState: AppCustomersState,
    customersCallbacks: AppCustomersCallbacks,
    navigationActions: AppFeatureNavigationActions,
    supportActions: AppFeatureSupportActions
) {
    composable(AppRoutes.Customers) {
        LaunchedEffect(customersState.customerQuery) {
            customerViewModel.searchCustomers(customersState.customerQuery)
        }
        CustomerListScreen(
            query = customersState.customerQuery,
            summaries = customersState.customerSummaries,
            onQueryChange = { customersCallbacks.onCustomerQueryChange(it) },
            onCustomerClick = { id ->
                navController.navigate(AppRoutes.customerDetail(id))
            },
            onBack = { navController.popBackStack() },
            onAddCustomer = navigationActions.openImportContacts,
            onPaymentHistory = { customerId ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Customer(customerId), null)
            },
            onRecordPayment = { customerId ->
                navigationActions.navigateToMoneyRecord(customerId)
            },
            onAddOrder = {
                val targetDate = supportActions.currentDate()
                navigationActions.navigateToCalendarQuickAdd(targetDate)
            },
            onArchiveCustomer = { customerId ->
                customerViewModel.archiveCustomer(customerId)
            },
            onDeleteCustomer = { customerId ->
                customerViewModel.deleteCustomer(customerId)
            },
            onRestoreCustomer = { customerId ->
                customerViewModel.unarchiveCustomer(customerId)
            },
            showBack = false
        )
    }

    composable(
        route = AppRoutes.CustomerDetail,
        arguments = listOf(navArgument(AppRoutes.ARG_CUSTOMER_ID) { type = NavType.LongType })
    ) { entry ->
        val customerId = entry.arguments?.getLong(AppRoutes.ARG_CUSTOMER_ID) ?: return@composable
        LaunchedEffect(customerId) {
            customerViewModel.loadCustomer(customerId)
        }
        CustomerDetailScreen(
            customer = customersState.customerDetail,
            balance = customersState.customerBalance,
            financeSummary = customersState.customerFinanceSummary,
            orders = customersState.customerOrders,
            onBack = { navController.popBackStack() },
            onPaymentHistory = { id ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Customer(id), null)
            },
            onOpenStatement = { id ->
                navController.navigate(AppRoutes.customerStatement(id))
            },
            onReceivePayment = {
                navigationActions.navigateToMoneyRecord(customerId)
            },
            onOrderPaymentHistory = { orderId ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Order(orderId), null)
            },
            onUpdateOrderStatusOverride = { orderId, override ->
                customerViewModel.setOrderStatusOverride(orderId, override)
                supportActions.refreshAfterPayments()
            },
            onWriteOffOrder = { orderId ->
                customerViewModel.writeOffOrder(orderId)
                supportActions.refreshAfterPayments()
            }
        )
    }

    composable(
        route = AppRoutes.CustomerStatement,
        arguments = listOf(navArgument(AppRoutes.ARG_CUSTOMER_ID) { type = NavType.LongType })
    ) { entry ->
        val customerId = entry.arguments?.getLong(AppRoutes.ARG_CUSTOMER_ID) ?: return@composable
        LaunchedEffect(customerId) {
            customerViewModel.loadCustomer(customerId)
        }
        CustomerStatementScreen(
            customer = customersState.customerDetail,
            balance = customersState.customerBalance,
            financeSummary = customersState.customerFinanceSummary,
            rows = customersState.customerStatementRows,
            isLoading = customersState.isCustomerStatementLoading,
            onBack = { navController.popBackStack() },
            onAddOrder = {
                val targetDate = supportActions.currentDate()
                navigationActions.navigateToCalendarQuickAdd(targetDate)
            },
            onRecordPayment = { navigationActions.navigateToMoneyRecord(customerId) },
            onMarkBadDebt = { amount, note ->
                customerViewModel.markBadDebt(customerId, amount, note)
                supportActions.refreshAfterPayments()
            }
        )
    }

    composable(AppRoutes.ImportContacts) {
        LaunchedEffect(Unit) {
            customersCallbacks.onContactsLoadingChange(true)
            customersCallbacks.onImportContactsChange(supportActions.loadContacts())
            customersCallbacks.onSelectedContactPhonesChange(emptySet())
            customersCallbacks.onContactsLoadingChange(false)
        }
        ImportContactsScreen(
            contacts = customersState.importContacts,
            selectedPhones = customersState.selectedContactPhones,
            isLoading = customersState.isContactsLoading,
            onBack = { navController.popBackStack() },
            onToggleSelect = { phone ->
                customersCallbacks.onSelectedContactPhonesChange(
                    if (customersState.selectedContactPhones.contains(phone)) {
                        customersState.selectedContactPhones - phone
                    } else {
                        customersState.selectedContactPhones + phone
                    }
                )
            },
            onToggleSelectAll = { visiblePhones ->
                val visibleSet = visiblePhones.toSet()
                val allVisibleSelected =
                    visibleSet.isNotEmpty() &&
                        visibleSet.all { customersState.selectedContactPhones.contains(it) }
                customersCallbacks.onSelectedContactPhonesChange(
                    if (allVisibleSelected) {
                        customersState.selectedContactPhones - visibleSet
                    } else {
                        customersState.selectedContactPhones + visibleSet
                    }
                )
            },
            onImport = {
                if (customersState.selectedContactPhones.isEmpty()) {
                    supportActions.onShowMessage("Select at least one contact to import")
                } else {
                    val contactsByPhone = customersState.importContacts.associateBy { it.phone }
                    customersState.selectedContactPhones.forEach { phone ->
                        val contact = contactsByPhone[phone] ?: return@forEach
                        customerViewModel.importCustomer(contact.name, contact.phone)
                    }
                    navController.popBackStack()
                }
            }
        )
    }
}
