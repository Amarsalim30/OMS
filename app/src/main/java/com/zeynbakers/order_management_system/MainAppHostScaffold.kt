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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.ui.AppScaffold
import com.zeynbakers.order_management_system.core.ui.MoneyTab
import com.zeynbakers.order_management_system.core.ui.MoreAction
import com.zeynbakers.order_management_system.core.ui.TopLevelDestination
import com.zeynbakers.order_management_system.core.tutorial.PracticalTutorialAction
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachAnchorRegistry
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachOverlay
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.practicalTutorialSteps
import com.zeynbakers.order_management_system.core.tutorial.routeMatchesPrefix
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
    tutorialAnchorRegistry: TutorialCoachAnchorRegistry,
    tutorialActive: Boolean,
    tutorialStepIndex: Int,
    onStartTutorial: (Int) -> Unit,
    onTutorialStepChange: (Int) -> Unit,
    onDismissTutorial: () -> Unit,
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
                currentRoute != AppRoutes.TutorialAfterCalendar &&
                currentRoute != AppRoutes.Tutorial
        val windowSizeClass = calculateWindowSizeClass(activity)
        val topLevelDestinations = listOf(
            TopLevelDestination(
                AppRoutes.Calendar,
                stringResource(R.string.nav_calendar),
                Icons.Filled.CalendarToday,
                tutorialTargetId = TutorialCoachTargets.NavCalendar
            ),
            TopLevelDestination(
                AppRoutes.Orders,
                stringResource(R.string.nav_orders),
                Icons.AutoMirrored.Filled.ListAlt,
                tutorialTargetId = TutorialCoachTargets.NavOrders
            ),
            TopLevelDestination(
                AppRoutes.Customers,
                stringResource(R.string.nav_customers),
                Icons.Filled.People,
                tutorialTargetId = TutorialCoachTargets.NavCustomers
            ),
            TopLevelDestination(
                AppRoutes.Money,
                stringResource(R.string.nav_money),
                Icons.Filled.AccountBalanceWallet,
                tutorialTargetId = TutorialCoachTargets.NavMoney
            )
        )

        val moreDailyWorkGroup = stringResource(R.string.more_group_daily_work)
        val moreSystemGroup = stringResource(R.string.more_group_system_setup)
        val moreLearningGroup = stringResource(R.string.more_group_learning)

        val moreActions = listOf(
            MoreAction(
                label = stringResource(R.string.more_notes_history),
                groupLabel = moreDailyWorkGroup,
                icon = Icons.AutoMirrored.Filled.Notes,
                supportingText = stringResource(R.string.more_notes_history_support),
                tutorialTargetId = TutorialCoachTargets.MoreNotesHistoryAction
            ) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.NotesHistory)
            },
            MoreAction(
                label = stringResource(R.string.more_import_contacts),
                groupLabel = moreDailyWorkGroup,
                icon = Icons.Filled.PersonAdd,
                supportingText = stringResource(R.string.more_import_contacts_support)
            ) {
                onShowMoreSheetChange(false)
                openImportContacts()
            },
            MoreAction(
                label = stringResource(R.string.more_floating_helper),
                groupLabel = moreSystemGroup,
                icon = Icons.Filled.Mic,
                supportingText = stringResource(R.string.more_floating_helper_support),
                tutorialTargetId = TutorialCoachTargets.MoreHelperAction
            ) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.HelperSettings)
            },
            MoreAction(
                label = stringResource(R.string.more_notifications),
                groupLabel = moreSystemGroup,
                icon = Icons.Filled.Notifications,
                supportingText = stringResource(R.string.more_notifications_support)
            ) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.Notifications)
            },
            MoreAction(
                label = stringResource(R.string.more_backup_restore),
                groupLabel = moreSystemGroup,
                icon = Icons.Filled.Settings,
                supportingText = stringResource(R.string.more_backup_restore_support)
            ) {
                onShowMoreSheetChange(false)
                navController.navigate(AppRoutes.Backup)
            },
            MoreAction(
                label = stringResource(R.string.more_tutorial),
                groupLabel = moreLearningGroup,
                icon = Icons.Filled.School,
                supportingText = stringResource(R.string.more_tutorial_support)
            ) {
                onShowMoreSheetChange(false)
                onStartTutorial(0)
            }
        )

        val tutorialSteps = remember { practicalTutorialSteps() }
        val clampedTutorialStep =
            if (tutorialSteps.isEmpty()) {
                0
            } else {
                tutorialStepIndex.coerceIn(0, tutorialSteps.lastIndex)
            }
        if (tutorialActive && tutorialStepIndex != clampedTutorialStep) {
            SideEffect {
                onTutorialStepChange(clampedTutorialStep)
            }
        }
        val tutorialStep = tutorialSteps.getOrNull(clampedTutorialStep)
        val isStepRouteActive =
            tutorialStep?.routePrefix?.let { prefix ->
                routeMatchesPrefix(currentRoute, prefix)
            } ?: true
        val tutorialTargetBounds =
            if (tutorialStep != null && isStepRouteActive) {
                tutorialStep.targetId?.let { tutorialAnchorRegistry.boundsFor(it) }
            } else {
                null
            }

        fun performTutorialPrimaryAction(action: PracticalTutorialAction) {
            when (action) {
                PracticalTutorialAction.None -> Unit
                PracticalTutorialAction.OpenCalendar -> {
                    onShowMoreSheetChange(false)
                    navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
                }
                PracticalTutorialAction.OpenOrders -> {
                    onShowMoreSheetChange(false)
                    navigateTopLevel(navController, AppRoutes.Orders, resetToRoot = true)
                }
                PracticalTutorialAction.OpenCustomers -> {
                    onShowMoreSheetChange(false)
                    navigateTopLevel(navController, AppRoutes.Customers, resetToRoot = true)
                }
                PracticalTutorialAction.OpenMoneyCollect -> {
                    onShowMoreSheetChange(false)
                    accountsCallbacks.onMoneyTabChange(MoneyTab.Collect)
                    navigateTopLevel(navController, AppRoutes.Money, resetToRoot = true)
                }
                PracticalTutorialAction.OpenMoneyRecord -> {
                    onShowMoreSheetChange(false)
                    accountsCallbacks.onMoneyTabChange(MoneyTab.Record)
                    navigateTopLevel(navController, AppRoutes.Money, resetToRoot = true)
                }
                PracticalTutorialAction.OpenPaymentHistoryAll -> {
                    onShowMoreSheetChange(false)
                    navController.navigate(AppRoutes.paymentHistoryAll()) {
                        launchSingleTop = true
                    }
                }
                PracticalTutorialAction.OpenCalendarMoreSheet -> {
                    navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
                    onShowMoreSheetChange(true)
                }
                PracticalTutorialAction.OpenNotesHistory -> {
                    onShowMoreSheetChange(false)
                    navController.navigate(AppRoutes.NotesHistory) {
                        launchSingleTop = true
                    }
                }
                PracticalTutorialAction.OpenHelperSettings -> {
                    onShowMoreSheetChange(false)
                    navController.navigate(AppRoutes.HelperSettings) {
                        launchSingleTop = true
                    }
                }
            }
        }

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

        if (tutorialActive && tutorialStep != null) {
            TutorialCoachOverlay(
                targetBounds = tutorialTargetBounds,
                progressText =
                    stringResource(
                        R.string.tutorial_coach_progress,
                        clampedTutorialStep + 1,
                        tutorialSteps.size
                    ),
                title = stringResource(tutorialStep.titleRes),
                body = stringResource(tutorialStep.bodyRes),
                backLabel = stringResource(R.string.tutorial_coach_previous),
                skipLabel = stringResource(R.string.tutorial_coach_skip),
                nextLabel = stringResource(R.string.tutorial_coach_next),
                finishLabel = stringResource(R.string.tutorial_coach_finish),
                primaryActionLabel = tutorialStep.primaryActionRes?.let { stringResource(it) },
                showBack = clampedTutorialStep > 0,
                isLastStep = clampedTutorialStep == tutorialSteps.lastIndex,
                onBack = {
                    onShowMoreSheetChange(false)
                    onTutorialStepChange((clampedTutorialStep - 1).coerceAtLeast(0))
                },
                onSkip = {
                    onShowMoreSheetChange(false)
                    onDismissTutorial()
                },
                onNext = {
                    onShowMoreSheetChange(false)
                    if (clampedTutorialStep == tutorialSteps.lastIndex) {
                        onDismissTutorial()
                    } else {
                        onTutorialStepChange((clampedTutorialStep + 1).coerceAtMost(tutorialSteps.lastIndex))
                    }
                },
                onPrimaryAction =
                    if (tutorialStep.primaryAction == PracticalTutorialAction.None) {
                        null
                    } else {
                        {
                            performTutorialPrimaryAction(tutorialStep.primaryAction)
                        }
                    }
            )
        }
    }
}
