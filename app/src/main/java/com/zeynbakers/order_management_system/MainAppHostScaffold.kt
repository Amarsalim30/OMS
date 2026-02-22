package com.zeynbakers.order_management_system

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.ui.AppScaffold
import com.zeynbakers.order_management_system.core.ui.MoreAction
import com.zeynbakers.order_management_system.core.ui.TopLevelDestination
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.order.ui.OrderCreditPrompt
import com.zeynbakers.order_management_system.order.ui.OrderViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun MainAppHostScaffold(
    activity: ComponentActivity,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    activeTopLevelRoute: String,
    selectedTopLevelRoute: String,
    onSelectedTopLevelRouteChange: (String) -> Unit,
    showMoreSheet: Boolean,
    onShowMoreSheetChange: (Boolean) -> Unit,
    openImportContacts: () -> Unit,
    calendarState: AppCalendarState,
    ordersState: AppOrdersState,
    customersState: AppCustomersState,
    accountsState: AppAccountsState,
    calendarCallbacks: AppCalendarCallbacks,
    customersCallbacks: AppCustomersCallbacks,
    accountsCallbacks: AppAccountsCallbacks,
    navigationActions: AppFeatureNavigationActions,
    supportActions: AppFeatureSupportActions,
    orderViewModel: OrderViewModel,
    customerViewModel: CustomerAccountsViewModel,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    paymentHistoryViewModel: PaymentIntakeHistoryViewModel,
    appSnackbarHostState: SnackbarHostState,
    pendingCreditPrompt: OrderCreditPrompt?,
    onDismissCreditPrompt: () -> Unit,
    onApplyCreditPrompt: (OrderCreditPrompt) -> Unit,
    showUpdateDialog: Boolean,
    updateNotes: List<String>,
    onDismissUpdateDialog: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val showNavigation =
            currentRoute != AppRoutes.Splash &&
                currentRoute != AppRoutes.Intro &&
                currentRoute != AppRoutes.SetupChecklist &&
                currentRoute != AppRoutes.BusinessProfile &&
                currentRoute != AppRoutes.ContactsPermissionPrimer &&
                currentRoute != AppRoutes.NotificationsPermissionPrimer &&
                currentRoute != AppRoutes.MicrophonePermissionPrimer &&
                currentRoute != AppRoutes.OverlayPermissionPrimer &&
                currentRoute != AppRoutes.FirstRunTutorial &&
                currentRoute != AppRoutes.CalendarTutorial &&
                currentRoute != AppRoutes.Tutorial
        val windowSizeClass = calculateWindowSizeClass(activity)
        val topLevelDestinations = listOf(
            TopLevelDestination(
                AppRoutes.Calendar,
                stringResource(R.string.nav_calendar),
                Icons.Filled.CalendarToday
            ),
            TopLevelDestination(
                AppRoutes.Orders,
                stringResource(R.string.nav_orders),
                Icons.AutoMirrored.Filled.ListAlt
            ),
            TopLevelDestination(
                AppRoutes.Customers,
                stringResource(R.string.nav_customers),
                Icons.Filled.People
            ),
            TopLevelDestination(
                AppRoutes.Money,
                stringResource(R.string.nav_money),
                Icons.Filled.AccountBalanceWallet
            )
        )

        val moreActions = listOf(
            MoreAction(stringResource(R.string.more_floating_helper), Icons.Filled.Mic) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.HelperSettings)
            },
            MoreAction(stringResource(R.string.more_notes_history), Icons.AutoMirrored.Filled.Notes) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.NotesHistory)
            },
            MoreAction(stringResource(R.string.more_notifications), Icons.Filled.Notifications) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.Notifications)
            },
            MoreAction(stringResource(R.string.more_import_contacts), Icons.Filled.PersonAdd) {
                onShowMoreSheetChange(false)
                openImportContacts()
            },
            MoreAction(stringResource(R.string.more_backup_restore), Icons.Filled.Settings) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.Backup)
            },
            MoreAction(stringResource(R.string.more_tutorial), Icons.Filled.School) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.Tutorial) {
                    launchSingleTop = true
                }
            }
        )

        AppScaffold(
            windowSizeClass = windowSizeClass,
            destinations = topLevelDestinations,
            selectedRoute = activeTopLevelRoute,
            onDestinationSelected = { route ->
                onSelectedTopLevelRouteChange(route)
                navigateTopLevel(navController, route, resetToRoot = true)
            },
            showNavigation = showNavigation,
            showMoreSheet = showMoreSheet,
            onOpenMore = { onShowMoreSheetChange(true) },
            onDismissMore = { onShowMoreSheetChange(false) },
            moreActions = moreActions
        ) { padding ->
            AppFeatureNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize().padding(padding),
                orderViewModel = orderViewModel,
                customerViewModel = customerViewModel,
                paymentIntakeViewModel = paymentIntakeViewModel,
                paymentHistoryViewModel = paymentHistoryViewModel,
                calendarState = calendarState,
                ordersState = ordersState,
                customersState = customersState,
                accountsState = accountsState,
                calendarCallbacks = calendarCallbacks,
                customersCallbacks = customersCallbacks,
                accountsCallbacks = accountsCallbacks,
                navigationActions = navigationActions,
                supportActions = supportActions
            )
        }

        SnackbarHost(
            hostState = appSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        pendingCreditPrompt?.let { prompt ->
            CreditPromptDialog(
                prompt = prompt,
                onDismiss = onDismissCreditPrompt,
                onApplyCredit = { onApplyCreditPrompt(prompt) }
            )
        }

        if (showUpdateDialog) {
            WhatsNewDialog(
                notes = updateNotes,
                onDismiss = onDismissUpdateDialog
            )
        }
    }
}
