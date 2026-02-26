package com.zeynbakers.order_management_system.core.navigation.graphs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.AppCustomersCallbacks
import com.zeynbakers.order_management_system.AppCustomersState
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.AppFeatureSupportActions
import com.zeynbakers.order_management_system.MoneyRecordContext
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.onboarding.OnboardingPreferences
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.customer.ui.ImportContactsScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerStatementScreen
import kotlinx.coroutines.launch

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
            onSyncContacts = navigationActions.openImportContacts,
            onPaymentHistory = { customerId ->
                navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Customer(customerId), null)
            },
            onRecordPayment = { customerId ->
                navigationActions.navigateToMoneyRecord(
                    MoneyRecordContext(customerId = customerId)
                )
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
                navigationActions.navigateToMoneyRecord(
                    MoneyRecordContext(customerId = customerId)
                )
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
            onRecordPayment = {
                navigationActions.navigateToMoneyRecord(
                    MoneyRecordContext(customerId = customerId)
                )
            },
            onMarkBadDebt = { amount, note ->
                customerViewModel.markBadDebt(customerId, amount, note)
                supportActions.refreshAfterPayments()
            }
        )
    }

    composable(AppRoutes.ImportContacts) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val onboardingPrefs = remember { OnboardingPreferences(context) }
        val selectAtLeastOneMessage = stringResource(R.string.import_contacts_select_at_least_one)
        val permissionRequiredMessage = stringResource(R.string.contacts_permission_required_for_import)
        val loadFailedMessage = stringResource(R.string.import_contacts_load_failed)
        var hasPermission by remember { mutableStateOf(hasContactsPermission(context)) }
        var permissionRequested by rememberSaveable { mutableStateOf(false) }
        var loadError by rememberSaveable { mutableStateOf<String?>(null) }
        val activity = context.findActivity()
        val permissionPermanentlyDenied =
            !hasPermission &&
                permissionRequested &&
                activity?.let {
                    !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.READ_CONTACTS
                    )
                } == true

        suspend fun loadContactsSafely() {
            if (!hasPermission) {
                customersCallbacks.onImportContactsChange(emptyList())
                customersCallbacks.onSelectedContactPhonesChange(emptySet())
                customersCallbacks.onContactsLoadingChange(false)
                loadError = permissionRequiredMessage
                return
            }
            customersCallbacks.onContactsLoadingChange(true)
            try {
                val loaded = supportActions.loadContacts()
                customersCallbacks.onImportContactsChange(loaded)
                customersCallbacks.onSelectedContactPhonesChange(emptySet())
                loadError = null
            } catch (_: SecurityException) {
                hasPermission = false
                customersCallbacks.onImportContactsChange(emptyList())
                customersCallbacks.onSelectedContactPhonesChange(emptySet())
                loadError = permissionRequiredMessage
            } catch (_: Throwable) {
                customersCallbacks.onImportContactsChange(emptyList())
                customersCallbacks.onSelectedContactPhonesChange(emptySet())
                loadError = loadFailedMessage
            } finally {
                customersCallbacks.onContactsLoadingChange(false)
            }
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permissionRequested = true
                hasPermission = granted
                if (!granted) {
                    loadError = permissionRequiredMessage
                }
            }

        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                loadContactsSafely()
            } else {
                customersCallbacks.onImportContactsChange(emptyList())
                customersCallbacks.onSelectedContactPhonesChange(emptySet())
                customersCallbacks.onContactsLoadingChange(false)
            }
        }

        ImportContactsScreen(
            contacts = customersState.importContacts,
            selectedPhones = customersState.selectedContactPhones,
            isLoading = customersState.isContactsLoading,
            hasPermission = hasPermission,
            isPermissionPermanentlyDenied = permissionPermanentlyDenied,
            errorMessage = loadError,
            onBack = { navController.popBackStack() },
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onOpenSettings = {
                openAppSettings(context)
            },
            onRetryLoad = {
                scope.launch { loadContactsSafely() }
            },
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
                    supportActions.onShowMessage(selectAtLeastOneMessage)
                } else {
                    val contactsByPhone = customersState.importContacts.associateBy { it.phone }
                    customersState.selectedContactPhones.forEach { phone ->
                        val contact = contactsByPhone[phone] ?: return@forEach
                        customerViewModel.importCustomer(contact.name, contact.phone)
                    }
                    scope.launch { onboardingPrefs.setContactsSetupDone(true) }
                    navController.popBackStack()
                }
            }
        )
    }
}

private fun hasContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
