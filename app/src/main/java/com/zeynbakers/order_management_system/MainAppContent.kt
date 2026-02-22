package com.zeynbakers.order_management_system
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.backup.BackupScheduler
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.helper.HelperCaptureActivity
import com.zeynbakers.order_management_system.core.helper.HelperCaptureMode
import com.zeynbakers.order_management_system.core.helper.HelperOverlayController
import com.zeynbakers.order_management_system.core.helper.HelperPreferences
import com.zeynbakers.order_management_system.core.helper.HelperSettingsState
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.navigation.AppShortcuts
import com.zeynbakers.order_management_system.core.navigation.extractSharedText
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import com.zeynbakers.order_management_system.core.ui.AmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.AppScaffold
import com.zeynbakers.order_management_system.core.ui.AppViewModelFactory
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.MoneyTab
import com.zeynbakers.order_management_system.core.ui.MoreAction
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.TopLevelDestination
import com.zeynbakers.order_management_system.core.ui.UiEvent
import com.zeynbakers.order_management_system.core.ui.UiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.VoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.VoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import com.zeynbakers.order_management_system.core.updates.UpdatePreferences
import com.zeynbakers.order_management_system.core.widget.WidgetUpdater
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import com.zeynbakers.order_management_system.order.ui.OrderCreditPrompt
import com.zeynbakers.order_management_system.order.ui.OrderDraft
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.State
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun MainAppContent(
    activity: ComponentActivity,
    launchIntentState: State<Intent?>,
    startDestination: String
) {
            Order_management_systemTheme {
                val context = LocalContext.current
                val database = remember { DatabaseProvider.getDatabase(activity.applicationContext) }
                val viewModelFactory = remember {
                    AppViewModelFactory(
                        database = database,
                        appContext = context.applicationContext
                    )
                }
                val orderViewModel: OrderViewModel = viewModel(factory = viewModelFactory)
                val customerViewModel: CustomerAccountsViewModel = viewModel(factory = viewModelFactory)
                val paymentIntakeViewModel: PaymentIntakeViewModel = viewModel(factory = viewModelFactory)
                val paymentHistoryViewModel: PaymentIntakeHistoryViewModel = viewModel(factory = viewModelFactory)
                val amountRegistry = remember { AmountFieldRegistry() }
                val voiceRouter = remember { VoiceInputRouter(onApplyTotal = amountRegistry::applyAmount) }
                val overlaySuppressed = remember { mutableStateOf(false) }
                val appSnackbarHostState = remember { SnackbarHostState() }
                val contactsPermissionMessage =
                    stringResource(R.string.contacts_permission_required_for_import)
                val uiEventDispatcher = remember(appSnackbarHostState) {
                    UiEventDispatcher { event ->
                        when (event) {
                            is UiEvent.Snackbar -> appSnackbarHostState.showSnackbar(
                                message = event.message,
                                actionLabel = event.actionLabel,
                                withDismissAction = event.withDismissAction,
                                duration = event.duration
                            )
                        }
                    }
                }
                val scope = rememberCoroutineScope()
                var currentMonth by rememberSaveable { mutableStateOf(0) }
                var currentYear by rememberSaveable { mutableStateOf(0) }
                var baseMonth by rememberSaveable { mutableStateOf(0) }
                var baseYear by rememberSaveable { mutableStateOf(0) }
                var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
                var summaryDate by remember { mutableStateOf<LocalDate?>(null) }
                var quickAddDate by remember { mutableStateOf<LocalDate?>(null) }
                var customerQuery by rememberSaveable { mutableStateOf("") }
                var paymentIntakeText by rememberSaveable { mutableStateOf<String?>(null) }
                var moneyTabName by rememberSaveable { mutableStateOf(MoneyTab.Collect.name) }
                var manualCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
                var showMoreSheet by rememberSaveable { mutableStateOf(false) }
                var selectedTopLevelRoute by rememberSaveable { mutableStateOf(AppRoutes.Calendar) }
                var importContacts by remember { mutableStateOf<List<ImportContact>>(emptyList()) }
                var selectedContactPhones by remember { mutableStateOf<Set<String>>(emptySet()) }
                var isContactsLoading by remember { mutableStateOf(false) }
                val dayDrafts = remember { mutableStateMapOf<LocalDate, OrderDraft>() }
                var hasRecordPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val updatePrefs = remember { UpdatePreferences(context) }
                val helperPrefs = remember { HelperPreferences(context) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                val helperState by helperPrefs.state.collectAsState(initial = HelperSettingsState())
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val activeTopLevelRoute = topLevelRouteFor(currentRoute) ?: selectedTopLevelRoute
                val moneyTab = runCatching { MoneyTab.valueOf(moneyTabName) }.getOrDefault(MoneyTab.Collect)
                val updateNotes = remember {
                    listOf(
                        "Share M-PESA messages from Messages directly into the app.",
                        "Oldest-order allocation is now the default when applying payments.",
                        "Each order has its own payment history, plus move/void controls.",
                        "Clearer order labels across screens for faster recognition.",
                        "Improved M-PESA amount parsing for cleaner imports.",
                        "Better voice recognition for notes and customer details.",
                        "Faster voice calculator responses when adding totals."
                    )
                }
                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted: Boolean ->
                    if (granted) {
                        navToImportContacts(
                            context = context,
                            onNavigate = { route -> navigateTopLevel(navController, route, resetToRoot = true) },
                            onOpen = { navController.navigate(AppRoutes.ImportContacts) }
                        )
                    } else {
                        scope.launch {
                            uiEventDispatcher.showSnackbar(contactsPermissionMessage)
                        }
                    }
                }
                val recordPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted: Boolean ->
                    hasRecordPermission = granted
                }
                val calendarDays by orderViewModel.calendarDays.collectAsState()
                val monthTotal by orderViewModel.monthTotal.collectAsState()
                val monthBadgeCount by orderViewModel.monthBadgeCount.collectAsState()
                val ordersForDate by orderViewModel.ordersForDate.collectAsState()
                val dayTotal by orderViewModel.dayTotal.collectAsState()
                val orderCustomerNames by orderViewModel.orderCustomerNames.collectAsState()
                val orderPaidAmounts by orderViewModel.orderPaidAmounts.collectAsState()
                val monthSnapshots by orderViewModel.monthSnapshots.collectAsState()
                val summaryOrders by orderViewModel.summaryOrders.collectAsState()
                val summaryTotal by orderViewModel.summaryTotal.collectAsState()
                val summaryCustomerNames by orderViewModel.summaryCustomerNames.collectAsState()
                val unpaidOrders by orderViewModel.unpaidOrders.collectAsState()
                val unpaidPaidAmounts by orderViewModel.unpaidPaidAmounts.collectAsState()
                val unpaidCustomerNames by orderViewModel.unpaidCustomerNames.collectAsState()
                val creditPrompt by orderViewModel.creditPrompt.collectAsState()
                val customerSummaries by customerViewModel.summaries.collectAsState()
                val customerDetail by customerViewModel.customer.collectAsState()
                val customerLedger by customerViewModel.ledger.collectAsState()
                val customerBalance by customerViewModel.balance.collectAsState()
                val customerFinanceSummary by customerViewModel.financeSummary.collectAsState()
                val customerOrders by customerViewModel.orders.collectAsState()
                val customerOrderLabels by customerViewModel.orderLabels.collectAsState()
                val customerStatementRows by customerViewModel.statementRows.collectAsState()
                val isCustomerStatementLoading by customerViewModel.isStatementLoading.collectAsState()
                var pendingCreditPrompt by remember { mutableStateOf<OrderCreditPrompt?>(null) }
                val incomingIntent by launchIntentState
                val currentDate: () -> LocalDate = {
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                }
                LaunchedEffect(currentMonth, currentYear) {
                    if (currentMonth > 0 && currentYear > 0) {
                        orderViewModel.loadMonth(month = currentMonth, year = currentYear)
                        orderViewModel.prefetchAdjacentMonths(year = currentYear, month = currentMonth)
                    }
                }
                LaunchedEffect(Unit) {
                    BackupScheduler.ensureScheduled(context)
                    NotificationScheduler.ensureScheduled(context)
                    AppShortcuts.ensure(context.applicationContext)
                    WidgetUpdater.enqueue(context)
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    currentMonth = now.monthNumber
                    currentYear = now.year
                    selectedDate = now.date
                    if (baseMonth == 0 && baseYear == 0) {
                        baseMonth = now.monthNumber
                        baseYear = now.year
                    }
                }
                LaunchedEffect(helperState.enabled) {
                    if (helperState.enabled) {
                        HelperOverlayController.start(context.applicationContext)
                    } else {
                        HelperOverlayController.stop(context.applicationContext)
                    }
                }
                LaunchedEffect(currentRoute, activeTopLevelRoute) {
                    if (currentRoute == AppRoutes.Calendar &&
                        activeTopLevelRoute == AppRoutes.Calendar &&
                        updatePrefs.shouldShowUpdate(BuildConfig.VERSION_NAME)
                    ) {
                        showUpdateDialog = true
                    }
                }
                LaunchedEffect(creditPrompt) {
                    if (creditPrompt != null) {
                        pendingCreditPrompt = creditPrompt
                    }
                }
                val openImportContacts: () -> Unit = {
                    val hasPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        navController.navigate(AppRoutes.ImportContacts)
                    } else {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }
                val refreshAfterPayments: () -> Unit = {
                    if (currentMonth > 0 && currentYear > 0) {
                        orderViewModel.loadMonth(month = currentMonth, year = currentYear)
                    }
                    orderViewModel.loadUnpaidOrders()
                    selectedDate?.let { orderViewModel.loadOrdersForDate(it) }
                    customerViewModel.searchCustomers(customerQuery)
                    customerDetail?.id?.let { activeCustomerId ->
                        customerViewModel.loadCustomer(activeCustomerId)
                    }
                    WidgetUpdater.enqueue(context)
                    NotificationScheduler.enqueueNow(context)
                }
                val voiceCalcAccess =
                    remember(hasRecordPermission) {
                        VoiceCalcAccess(
                            hasPermission = hasRecordPermission,
                            onRequestPermission = {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onApplyAmount = { amount -> amountRegistry.applyAmount(amount) }
                        )
                    }
                LaunchedEffect(incomingIntent) {
                    val intent = incomingIntent
                    val action = intent?.action
                    if (intent != null) {
                        AppShortcuts.reportShortcutUsed(context.applicationContext, action)
                    }
                    when (action) {
                        AppIntents.ACTION_SHOW_TODAY -> {
                            val targetDate = currentDate()
                            selectedDate = targetDate
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateCalendarExternal(navController, AppRoutes.day(targetDate))
                        }
                        AppIntents.ACTION_SHOW_DAY -> {
                            val targetDate =
                                intent?.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: currentDate()
                            selectedDate = targetDate
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateCalendarExternal(navController, AppRoutes.day(targetDate))
                        }
                        AppIntents.ACTION_SHOW_UNPAID -> {
                            selectedTopLevelRoute = AppRoutes.Orders
                            navigateTopLevel(navController, AppRoutes.Orders, resetToRoot = true)
                        }
                        AppIntents.ACTION_SHOW_SUMMARY -> {
                            summaryDate =
                                intent?.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: currentDate()
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateCalendarExternal(navController, AppRoutes.Summary)
                        }
                        AppIntents.ACTION_NEW_ORDER -> {
                            val targetDate = currentDate()
                            selectedDate = targetDate
                            quickAddDate = targetDate
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
                        }
                        AppIntents.ACTION_SHOW_BACKUP -> {
                            navController.navigate(AppRoutes.Backup) {
                                launchSingleTop = true
                            }
                        }
                        AppIntents.ACTION_SHOW_NOTES_HISTORY -> {
                            navController.navigate(AppRoutes.NotesHistory) {
                                launchSingleTop = true
                            }
                        }
                        AppIntents.ACTION_CAPTURE_VOICE_NOTE -> {
                            val captureIntent =
                                Intent(context, HelperCaptureActivity::class.java).apply {
                                    putExtra(HelperCaptureActivity.EXTRA_MODE, HelperCaptureMode.VoiceNote.wireValue)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(captureIntent)
                        }
                        AppIntents.ACTION_CAPTURE_VOICE_CALCULATOR -> {
                            val captureIntent =
                                Intent(context, HelperCaptureActivity::class.java).apply {
                                    putExtra(
                                        HelperCaptureActivity.EXTRA_MODE,
                                        HelperCaptureMode.VoiceCalculator.wireValue
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(captureIntent)
                        }
                        Intent.ACTION_SEND,
                        Intent.ACTION_SEND_MULTIPLE -> {
                            val sharedText = extractSharedText(intent) ?: return@LaunchedEffect
                            if (currentRoute == AppRoutes.Money && moneyTab == MoneyTab.Collect) {
                                paymentIntakeViewModel.appendRawText(sharedText)
                            } else {
                                paymentIntakeText = sharedText
                                moneyTabName = MoneyTab.Collect.name
                                selectedTopLevelRoute = AppRoutes.Money
                                navController.navigate(AppRoutes.Money) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                }
                CompositionLocalProvider(
                    LocalAmountFieldRegistry provides amountRegistry,
                    LocalVoiceCalcAccess provides voiceCalcAccess,
                    LocalVoiceOverlaySuppressed provides overlaySuppressed,
                    LocalVoiceInputRouter provides voiceRouter,
                    LocalUiEventDispatcher provides uiEventDispatcher
                ) {
                    val calendarState = AppCalendarState(
                        calendarDays = calendarDays,
                        currentYear = currentYear,
                        currentMonth = currentMonth,
                        baseYear = baseYear,
                        baseMonth = baseMonth,
                        monthSnapshots = monthSnapshots,
                        monthTotal = monthTotal,
                        monthBadgeCount = monthBadgeCount,
                        selectedDate = selectedDate,
                        summaryDate = summaryDate,
                        quickAddDate = quickAddDate,
                        ordersForDate = ordersForDate,
                        dayTotal = dayTotal,
                        orderCustomerNames = orderCustomerNames,
                        orderPaidAmounts = orderPaidAmounts,
                        summaryOrders = summaryOrders,
                        summaryTotal = summaryTotal,
                        summaryCustomerNames = summaryCustomerNames,
                        dayDrafts = dayDrafts
                    )
                    val ordersState = AppOrdersState(
                        unpaidOrders = unpaidOrders,
                        unpaidPaidAmounts = unpaidPaidAmounts,
                        unpaidCustomerNames = unpaidCustomerNames
                    )
                    val customersState = AppCustomersState(
                        customerSummaries = customerSummaries,
                        customerDetail = customerDetail,
                        customerLedger = customerLedger,
                        customerBalance = customerBalance,
                        customerFinanceSummary = customerFinanceSummary,
                        customerOrders = customerOrders,
                        customerOrderLabels = customerOrderLabels,
                        customerStatementRows = customerStatementRows,
                        isCustomerStatementLoading = isCustomerStatementLoading,
                        customerQuery = customerQuery,
                        importContacts = importContacts,
                        selectedContactPhones = selectedContactPhones,
                        isContactsLoading = isContactsLoading
                    )
                    val accountsState = AppAccountsState(
                        moneyTab = moneyTab,
                        paymentIntakeText = paymentIntakeText,
                        manualCustomerId = manualCustomerId
                    )
                    val calendarCallbacks = AppCalendarCallbacks(
                        onSelectedDateChange = { selectedDate = it },
                        onSummaryDateChange = { summaryDate = it },
                        onQuickAddDateChange = { quickAddDate = it },
                        onMonthSettled = { year, month ->
                            currentYear = year
                            currentMonth = month
                        }
                    )
                    val customersCallbacks = AppCustomersCallbacks(
                        onCustomerQueryChange = { customerQuery = it },
                        onImportContactsChange = { importContacts = it },
                        onSelectedContactPhonesChange = { selectedContactPhones = it },
                        onContactsLoadingChange = { isContactsLoading = it }
                    )
                    val accountsCallbacks = AppAccountsCallbacks(
                        onMoneyTabChange = { moneyTabName = it.name },
                        onPaymentIntakeTextChange = { paymentIntakeText = it },
                        onManualCustomerIdChange = { manualCustomerId = it }
                    )
                    val navigationActions = AppFeatureNavigationActions(
                        onOpenMore = { showMoreSheet = true },
                        openImportContacts = openImportContacts,
                        navigateToMoneyRecord = { customerId ->
                            manualCustomerId = customerId
                            moneyTabName = MoneyTab.Record.name
                            selectedTopLevelRoute = AppRoutes.Money
                            navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                        },
                        navigateToCalendarQuickAdd = { targetDate ->
                            selectedDate = targetDate
                            quickAddDate = targetDate
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
                        },
                        navigateToPaymentHistory = { filter, focusReceiptId ->
                            navigateToPaymentHistory(navController, filter, focusReceiptId)
                        }
                    )
                    val supportActions = AppFeatureSupportActions(
                        refreshAfterPayments = refreshAfterPayments,
                        currentDate = currentDate,
                        monthLabel = ::monthLabel,
                        onShowMessage = { message ->
                            scope.launch { uiEventDispatcher.showSnackbar(message) }
                        },
                        loadContacts = {
                            withContext(Dispatchers.IO) {
                                loadAllContacts(context)
                            }
                        }
                    )
                    MainAppHostScaffold(
                        activity = activity,
                        navController = navController,
                        startDestination = startDestination,
                        currentRoute = currentRoute,
                        activeTopLevelRoute = activeTopLevelRoute,
                        selectedTopLevelRoute = selectedTopLevelRoute,
                        onSelectedTopLevelRouteChange = { selectedTopLevelRoute = it },
                        showMoreSheet = showMoreSheet,
                        onShowMoreSheetChange = { showMoreSheet = it },
                        openImportContacts = openImportContacts,
                        calendarState = calendarState,
                        ordersState = ordersState,
                        customersState = customersState,
                        accountsState = accountsState,
                        calendarCallbacks = calendarCallbacks,
                        customersCallbacks = customersCallbacks,
                        accountsCallbacks = accountsCallbacks,
                        navigationActions = navigationActions,
                        supportActions = supportActions,
                        orderViewModel = orderViewModel,
                        customerViewModel = customerViewModel,
                        paymentIntakeViewModel = paymentIntakeViewModel,
                        paymentHistoryViewModel = paymentHistoryViewModel,
                        appSnackbarHostState = appSnackbarHostState,
                        pendingCreditPrompt = pendingCreditPrompt,
                        onDismissCreditPrompt = {
                            pendingCreditPrompt = null
                            orderViewModel.clearCreditPrompt()
                        },
                        onApplyCreditPrompt = { prompt ->
                            pendingCreditPrompt = null
                            orderViewModel.applyAvailableCreditToOrder(
                                orderId = prompt.orderId,
                                customerId = prompt.customerId
                            )
                            refreshAfterPayments()
                        },
                        showUpdateDialog = showUpdateDialog,
                        updateNotes = updateNotes,
                        onDismissUpdateDialog = {
                            showUpdateDialog = false
                            updatePrefs.markVersionSeen(BuildConfig.VERSION_NAME)
                        }
                    )
                }
            }
}
