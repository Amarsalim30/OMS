package com.zeynbakers.order_management_system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.backup.BackupScheduler
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.navigation.AppShortcuts
import com.zeynbakers.order_management_system.core.navigation.extractSharedText
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen
import com.zeynbakers.order_management_system.core.ui.AmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.AppScaffold
import com.zeynbakers.order_management_system.core.ui.AppViewModelFactory
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.MoneyScreen
import com.zeynbakers.order_management_system.core.ui.MoneyTab
import com.zeynbakers.order_management_system.core.ui.MoreAction
import com.zeynbakers.order_management_system.core.ui.TopLevelDestination
import com.zeynbakers.order_management_system.core.ui.VoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.VoiceCalculatorOverlay
import com.zeynbakers.order_management_system.core.ui.VoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumber
import com.zeynbakers.order_management_system.core.updates.UpdatePreferences
import com.zeynbakers.order_management_system.core.widget.WidgetUpdater
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import com.zeynbakers.order_management_system.customer.ui.ImportContactsScreen
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.OrderCreditPrompt
import com.zeynbakers.order_management_system.order.ui.OrderDraft
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import com.zeynbakers.order_management_system.order.ui.UnpaidOrdersScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MainActivity : ComponentActivity() {
    private val launchIntent = mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchIntent.value = intent

        setContent {
            Order_management_systemTheme {
                val context = LocalContext.current
                val database = remember { DatabaseProvider.getDatabase(applicationContext) }
                val viewModelFactory = remember { AppViewModelFactory(database) }
                val orderViewModel: OrderViewModel = viewModel(factory = viewModelFactory)
                val customerViewModel: CustomerAccountsViewModel = viewModel(factory = viewModelFactory)
                val paymentIntakeViewModel: PaymentIntakeViewModel = viewModel(factory = viewModelFactory)
                val paymentHistoryViewModel: PaymentIntakeHistoryViewModel = viewModel(factory = viewModelFactory)
                val ledgerViewModel: LedgerViewModel = viewModel(factory = viewModelFactory)

                val amountRegistry = remember { AmountFieldRegistry() }
                val voiceRouter = remember { VoiceInputRouter(onApplyTotal = amountRegistry::applyAmount) }
                val overlaySuppressed = remember { mutableStateOf(false) }

                var currentMonth by rememberSaveable { mutableStateOf(0) }
                var currentYear by rememberSaveable { mutableStateOf(0) }
                var baseMonth by rememberSaveable { mutableStateOf(0) }
                var baseYear by rememberSaveable { mutableStateOf(0) }

                var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
                var summaryDate by remember { mutableStateOf<LocalDate?>(null) }
                var quickAddDate by remember { mutableStateOf<LocalDate?>(null) }

                var customerQuery by rememberSaveable { mutableStateOf("") }
                var paymentIntakeText by rememberSaveable { mutableStateOf<String?>(null) }
                var moneyTabName by rememberSaveable { mutableStateOf(MoneyTab.Mpesa.name) }
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
                var showUpdateDialog by remember { mutableStateOf(false) }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val activeTopLevelRoute = topLevelRouteFor(currentRoute) ?: selectedTopLevelRoute

                val moneyTab = runCatching { MoneyTab.valueOf(moneyTabName) }.getOrDefault(MoneyTab.Mpesa)

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
                        Toast.makeText(
                            context,
                            "Contacts permission is required to import customers",
                            Toast.LENGTH_SHORT
                        ).show()
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

                var pendingCreditPrompt by remember { mutableStateOf<OrderCreditPrompt?>(null) }
                val incomingIntent by launchIntent
                val today = remember {
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

                LaunchedEffect(activeTopLevelRoute) {
                    if (activeTopLevelRoute == AppRoutes.Calendar &&
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
                    val intent = incomingIntent ?: return@LaunchedEffect
                    when (intent.action) {
                        AppIntents.ACTION_SHOW_TODAY -> {
                            selectedDate = today
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navController.navigate(AppRoutes.day(today))
                        }
                        AppIntents.ACTION_SHOW_DAY -> {
                            val targetDate =
                                intent.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: today
                            selectedDate = targetDate
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navController.navigate(AppRoutes.day(targetDate))
                        }
                        AppIntents.ACTION_SHOW_UNPAID -> {
                            selectedTopLevelRoute = AppRoutes.Orders
                            navigateTopLevel(navController, AppRoutes.Orders, resetToRoot = true)
                        }
                        AppIntents.ACTION_SHOW_SUMMARY -> {
                            summaryDate =
                                intent.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: today
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navController.navigate(AppRoutes.Summary)
                        }
                        AppIntents.ACTION_NEW_ORDER -> {
                            selectedDate = today
                            quickAddDate = today
                            selectedTopLevelRoute = AppRoutes.Calendar
                            navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
                        }
                        Intent.ACTION_SEND,
                        Intent.ACTION_SEND_MULTIPLE -> {
                            val sharedText = extractSharedText(intent) ?: return@LaunchedEffect
                            if (currentRoute == AppRoutes.Money && moneyTab == MoneyTab.Mpesa) {
                                paymentIntakeViewModel.appendRawText(sharedText)
                            } else {
                                paymentIntakeText = sharedText
                                moneyTabName = MoneyTab.Mpesa.name
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
                    LocalVoiceInputRouter provides voiceRouter
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
                        val topLevelDestinations = listOf(
                            TopLevelDestination(AppRoutes.Calendar, "Calendar", Icons.Filled.CalendarToday),
                            TopLevelDestination(AppRoutes.Orders, "Orders", Icons.Filled.ListAlt),
                            TopLevelDestination(AppRoutes.Customers, "Customers", Icons.Filled.People),
                            TopLevelDestination(AppRoutes.Money, "Money", Icons.Filled.Payments)
                        )

                        val moreActions = listOf(
                            MoreAction("Summary", Icons.Filled.ReceiptLong) {
                                showMoreSheet = false
                                selectedTopLevelRoute = AppRoutes.Calendar
                                navController.navigate(AppRoutes.Summary)
                            },
                            MoreAction("Ledger", Icons.Filled.ReceiptLong) {
                                showMoreSheet = false
                                moneyTabName = MoneyTab.Ledger.name
                                selectedTopLevelRoute = AppRoutes.Money
                                navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                            },
                            MoreAction("Payment history", Icons.Filled.Payments) {
                                showMoreSheet = false
                                selectedTopLevelRoute = AppRoutes.Money
                                navigateToPaymentHistory(navController, PaymentHistoryFilter.All, null)
                            },
                            MoreAction("Backup & restore", Icons.Filled.Settings) {
                                showMoreSheet = false
                                navController.navigate(AppRoutes.Backup)
                            },
                            MoreAction("Notifications", Icons.Filled.Notifications) {
                                showMoreSheet = false
                                navController.navigate(AppRoutes.Notifications)
                            },
                            MoreAction("Import contacts", Icons.Filled.PersonAdd) {
                                showMoreSheet = false
                                openImportContacts()
                            }
                        )

                        AppScaffold(
                            windowSizeClass = windowSizeClass,
                            destinations = topLevelDestinations,
                            selectedRoute = activeTopLevelRoute,
                            onDestinationSelected = { route ->
                                selectedTopLevelRoute = route
                                navigateTopLevel(navController, route, resetToRoot = true)
                            },
                            showMoreSheet = showMoreSheet,
                            onOpenMore = { showMoreSheet = true },
                            onDismissMore = { showMoreSheet = false },
                            moreActions = moreActions
                        ) { padding ->
                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.Calendar,
                                modifier = Modifier.fillMaxSize().padding(padding)
                            ) {
                                composable(AppRoutes.Calendar) {
                                    CalendarScreen(
                                        days = calendarDays,
                                        currentYear = currentYear,
                                        currentMonth = currentMonth,
                                        baseYear = baseYear,
                                        baseMonth = baseMonth,
                                        monthSnapshots = monthSnapshots,
                                        monthTotal = monthTotal,
                                        monthBadgeCount = monthBadgeCount,
                                        selectedDate = selectedDate,
                                        onSelectDate = { selectedDate = it },
                                        onOpenDay = { date ->
                                            selectedDate = date
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
                                            WidgetUpdater.enqueue(context)
                                            NotificationScheduler.enqueueNow(context)
                                        },
                                        searchCustomers = { query -> orderViewModel.searchCustomers(query) },
                                        onCustomersClick = {
                                            selectedTopLevelRoute = AppRoutes.Customers
                                            navigateTopLevel(navController, AppRoutes.Customers, resetToRoot = true)
                                        },
                                        onSummaryClick = { showMoreSheet = true },
                                        onMonthSettled = { year, month ->
                                            currentYear = year
                                            currentMonth = month
                                        },
                                        openQuickAddDate = quickAddDate,
                                        onQuickAddConsumed = { quickAddDate = null }
                                    )
                                }

                                composable(
                                    route = AppRoutes.Day,
                                    arguments = listOf(navArgument(AppRoutes.ARG_DATE) { type = NavType.StringType })
                                ) { entry ->
                                    val dateArg = entry.arguments?.getString(AppRoutes.ARG_DATE)
                                    val date = dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
                                    LaunchedEffect(date) {
                                        selectedDate = date
                                        orderViewModel.loadOrdersForDate(date)
                                    }
                                    DayDetailScreen(
                                        date = date,
                                        orders = ordersForDate,
                                        dayTotal = dayTotal,
                                        customerNames = orderCustomerNames,
                                        orderPaidAmounts = orderPaidAmounts,
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
                                            WidgetUpdater.enqueue(context)
                                            NotificationScheduler.enqueueNow(context)
                                        },
                                        onDeleteOrder = { orderId ->
                                            orderViewModel.cancelOrder(orderId, date)
                                            WidgetUpdater.enqueue(context)
                                            NotificationScheduler.enqueueNow(context)
                                        },
                                        loadOrderPaymentAllocations = { orderId ->
                                            orderViewModel.loadOrderPaymentAllocations(orderId)
                                        },
                                        loadMoveOrderOptions = { customerId, excludeOrderId ->
                                            orderViewModel.loadMoveOrderOptions(customerId, excludeOrderId)
                                        },
                                        onDeleteOrderWithPayments = { orderId, day, allocationIds, action, target, moveFull ->
                                            val result = orderViewModel.deleteOrderWithPayments(
                                                orderId = orderId,
                                                date = day,
                                                allocationIds = allocationIds,
                                                action = action,
                                                target = target,
                                                moveFullReceipts = moveFull
                                            )
                                            if (result) {
                                                WidgetUpdater.enqueue(context)
                                                NotificationScheduler.enqueueNow(context)
                                            }
                                            result
                                        },
                                        onOrderPaymentHistory = { orderId ->
                                            navigateToPaymentHistory(
                                                navController,
                                                PaymentHistoryFilter.Order(orderId),
                                                null
                                            )
                                        },
                                        onReceivePayment = { order ->
                                            manualCustomerId = order.customerId
                                            moneyTabName = MoneyTab.Manual.name
                                            selectedTopLevelRoute = AppRoutes.Money
                                            navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                                        },
                                        loadCustomerById = { id -> orderViewModel.getCustomerById(id) },
                                        searchCustomers = { query -> orderViewModel.searchCustomers(query) },
                                        draft = dayDrafts[date],
                                        onDraftChange = { updated ->
                                            if (updated == null) {
                                                dayDrafts.remove(date)
                                            } else {
                                                dayDrafts[date] = updated
                                            }
                                        }
                                    )
                                }

                                composable(AppRoutes.Orders) {
                                    LaunchedEffect(Unit) { orderViewModel.loadUnpaidOrders() }
                                    UnpaidOrdersScreen(
                                        orders = unpaidOrders,
                                        paidAmounts = unpaidPaidAmounts,
                                        customerNames = unpaidCustomerNames,
                                        onBack = { navController.popBackStack() },
                                        onOpenDay = { date ->
                                            selectedDate = date
                                            navController.navigate(AppRoutes.day(date))
                                        },
                                        onReceivePayment = { order ->
                                            manualCustomerId = order.customerId
                                            moneyTabName = MoneyTab.Manual.name
                                            selectedTopLevelRoute = AppRoutes.Money
                                            navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                                        },
                                        title = "Orders",
                                        showBack = false
                                    )
                                }

                                composable(AppRoutes.Customers) {
                                    LaunchedEffect(customerQuery) {
                                        customerViewModel.searchCustomers(customerQuery)
                                    }
                                    CustomerListScreen(
                                        query = customerQuery,
                                        summaries = customerSummaries,
                                        onQueryChange = { customerQuery = it },
                                        onCustomerClick = { id ->
                                            navController.navigate(AppRoutes.customerDetail(id))
                                        },
                                        onBack = { navController.popBackStack() },
                                        onAddCustomer = openImportContacts,
                                        onPaymentHistory = { customerId ->
                                            navigateToPaymentHistory(
                                                navController,
                                                PaymentHistoryFilter.Customer(customerId),
                                                null
                                            )
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
                                        customer = customerDetail,
                                        ledger = customerLedger,
                                        balance = customerBalance,
                                        financeSummary = customerFinanceSummary,
                                        orders = customerOrders,
                                        orderLabels = customerOrderLabels,
                                        onBack = { navController.popBackStack() },
                                        onPaymentHistory = { id ->
                                            navigateToPaymentHistory(
                                                navController,
                                                PaymentHistoryFilter.Customer(id),
                                                null
                                            )
                                        },
                                        onReceivePayment = {
                                            manualCustomerId = customerId
                                            moneyTabName = MoneyTab.Manual.name
                                            selectedTopLevelRoute = AppRoutes.Money
                                            navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                                        },
                                        onOrderPaymentHistory = { orderId ->
                                            navigateToPaymentHistory(
                                                navController,
                                                PaymentHistoryFilter.Order(orderId),
                                                null
                                            )
                                        },
                                        onUpdateOrderStatusOverride = { orderId, override ->
                                            customerViewModel.setOrderStatusOverride(orderId, override)
                                            refreshAfterPayments()
                                        },
                                        onWriteOffOrder = { orderId ->
                                            customerViewModel.writeOffOrder(orderId)
                                            refreshAfterPayments()
                                        }
                                    )
                                }

                                composable(AppRoutes.ImportContacts) {
                                    LaunchedEffect(Unit) {
                                        isContactsLoading = true
                                        importContacts = withContext(Dispatchers.IO) {
                                            loadAllContacts(context)
                                        }
                                        selectedContactPhones = emptySet()
                                        isContactsLoading = false
                                    }
                                    ImportContactsScreen(
                                        contacts = importContacts,
                                        selectedPhones = selectedContactPhones,
                                        isLoading = isContactsLoading,
                                        onBack = { navController.popBackStack() },
                                        onToggleSelect = { phone ->
                                            selectedContactPhones =
                                                if (selectedContactPhones.contains(phone)) {
                                                    selectedContactPhones - phone
                                                } else {
                                                    selectedContactPhones + phone
                                                }
                                        },
                                        onToggleSelectAll = {
                                            val allPhones = importContacts.map { it.phone }.toSet()
                                            selectedContactPhones =
                                                if (selectedContactPhones.size == allPhones.size) {
                                                    emptySet()
                                                } else {
                                                    allPhones
                                                }
                                        },
                                        onImport = {
                                            if (selectedContactPhones.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    "Select at least one contact to import",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val contactsByPhone = importContacts.associateBy { it.phone }
                                                selectedContactPhones.forEach { phone ->
                                                    val contact = contactsByPhone[phone] ?: return@forEach
                                                    customerViewModel.importCustomer(contact.name, contact.phone)
                                                }
                                                navController.popBackStack()
                                            }
                                        }
                                    )
                                }

                                composable(AppRoutes.Money) {
                                    MoneyScreen(
                                        selectedTab = moneyTab,
                                        onTabChange = { moneyTabName = it.name },
                                        paymentIntakeViewModel = paymentIntakeViewModel,
                                        customerViewModel = customerViewModel,
                                        ledgerViewModel = ledgerViewModel,
                                        initialText = paymentIntakeText,
                                        manualCustomerId = manualCustomerId,
                                        onManualContextConsumed = {
                                            manualCustomerId = null
                                        },
                                        onManualSaved = { refreshAfterPayments() },
                                        onApplied = {
                                            paymentIntakeText = null
                                            refreshAfterPayments()
                                        },
                                        onAppliedInPlace = { refreshAfterPayments() },
                                        onOpenReceiptHistory = { receiptId ->
                                            navigateToPaymentHistory(
                                                navController,
                                                PaymentHistoryFilter.All,
                                                receiptId
                                            )
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
                                    val focusId = entry.arguments
                                        ?.getLong(AppRoutes.ARG_FOCUS_RECEIPT_ID)
                                        ?.takeIf { it > 0 }
                                    PaymentIntakeHistoryScreen(
                                        viewModel = paymentHistoryViewModel,
                                        filter = PaymentHistoryFilter.All,
                                        focusReceiptId = focusId,
                                        onBack = { navController.popBackStack() },
                                        onRemoved = { refreshAfterPayments() }
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
                                            refreshAfterPayments()
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
                                        onRemoved = { refreshAfterPayments() }
                                    )
                                }

                                composable(AppRoutes.Summary) {
                                    SummaryScreen(
                                        monthLabel = monthLabel(currentYear, currentMonth),
                                        monthTotal = monthTotal,
                                        initialDate = summaryDate ?: selectedDate ?: today,
                                        orders = summaryOrders,
                                        rangeTotal = summaryTotal,
                                        customerNames = summaryCustomerNames,
                                        onAnchorDateChange = { updated -> summaryDate = updated },
                                        onLoadRange = { start, end ->
                                            orderViewModel.loadSummaryRange(startInclusive = start, endExclusive = end)
                                        },
                                        onLedgerClick = {
                                            moneyTabName = MoneyTab.Ledger.name
                                            selectedTopLevelRoute = AppRoutes.Money
                                            navController.navigate(AppRoutes.Money) { launchSingleTop = true }
                                        },
                                        onBackupClick = { navController.navigate(AppRoutes.Backup) },
                                        onNotificationsClick = { navController.navigate(AppRoutes.Notifications) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(AppRoutes.Backup) {
                                    BackupSettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        onImportContacts = openImportContacts
                                    )
                                }

                                composable(AppRoutes.Notifications) {
                                    NotificationSettingsScreen(onBack = { navController.popBackStack() })
                                }
                            }
                        }

                        VoiceCalculatorOverlay(
                            hasPermission = hasRecordPermission,
                            onRequestPermission = { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            isSuppressed = overlaySuppressed.value,
                            defaultIdleYDp = 72.dp
                        )

                        pendingCreditPrompt?.let { prompt ->
                            AlertDialog(
                                onDismissRequest = {
                                    pendingCreditPrompt = null
                                    orderViewModel.clearCreditPrompt()
                                },
                                title = { Text("Apply available credit?") },
                                text = {
                                    Text(
                                        "Customer has ${formatKes(prompt.availableCredit)} in credit. Apply it to ${prompt.orderLabel}?"
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        pendingCreditPrompt = null
                                        orderViewModel.applyAvailableCreditToOrder(
                                            orderId = prompt.orderId,
                                            customerId = prompt.customerId
                                        )
                                        refreshAfterPayments()
                                    }) {
                                        Text("Apply credit")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        pendingCreditPrompt = null
                                        orderViewModel.clearCreditPrompt()
                                    }) {
                                        Text("Skip")
                                    }
                                }
                            )
                        }

                        if (showUpdateDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showUpdateDialog = false
                                    updatePrefs.markVersionSeen(BuildConfig.VERSION_NAME)
                                },
                                title = { Text("What's new") },
                                text = {
                                    Column {
                                        updateNotes.forEach { note ->
                                            Text(text = "- $note")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showUpdateDialog = false
                                            updatePrefs.markVersionSeen(BuildConfig.VERSION_NAME)
                                        }
                                    ) {
                                        Text("Got it")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchIntent.value = intent
    }
}
private fun topLevelRouteFor(route: String?): String? {
    return when {
        route == null -> null
        route == AppRoutes.Calendar -> AppRoutes.Calendar
        route.startsWith("day/") -> AppRoutes.Calendar
        route == AppRoutes.Summary -> AppRoutes.Calendar
        route == AppRoutes.Orders -> AppRoutes.Orders
        route == AppRoutes.Customers -> AppRoutes.Customers
        route.startsWith("customer/") -> AppRoutes.Customers
        route == AppRoutes.ImportContacts -> AppRoutes.Customers
        route == AppRoutes.Money -> AppRoutes.Money
        route.startsWith("payment_history/") -> AppRoutes.Money
        else -> null
    }
}

private fun navigateTopLevel(
    navController: NavHostController,
    route: String,
    resetToRoot: Boolean = false
) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    if (resetToRoot) {
        if (currentRoute == route) {
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
        return
    }
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun navigateToPaymentHistory(
    navController: NavHostController,
    filter: PaymentHistoryFilter,
    focusReceiptId: Long?
) {
    val route = when (filter) {
        PaymentHistoryFilter.All -> AppRoutes.paymentHistoryAll(focusReceiptId)
        is PaymentHistoryFilter.Customer -> AppRoutes.paymentHistoryCustomer(filter.customerId)
        is PaymentHistoryFilter.Order -> AppRoutes.paymentHistoryOrder(filter.orderId)
    }
    navController.navigate(route)
}

private fun navToImportContacts(
    context: Context,
    onNavigate: (String) -> Unit,
    onOpen: () -> Unit
) {
    val hasPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    if (hasPermission) {
        onOpen()
    } else {
        onNavigate(AppRoutes.Customers)
    }
}

private fun monthLabel(year: Int, month: Int): String {
    if (month == 0 || year == 0) return "Loading..."
    val monthName = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }
    return "$monthName $year"
}

private suspend fun loadAllContacts(context: Context): List<ImportContact> {
    val resolver = context.contentResolver
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val cursor =
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        ) ?: return emptyList()

    val results = mutableListOf<ImportContact>()
    val seen = mutableSetOf<String>()
    cursor.use { phones ->
        val nameIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        if (nameIndex == -1 || numberIndex == -1) return emptyList()

        while (phones.moveToNext()) {
            val rawName = phones.getString(nameIndex) ?: ""
            val rawNumber = phones.getString(numberIndex) ?: ""
            val cleanNumber = normalizePhoneNumber(rawNumber)
            if (cleanNumber.isBlank() || !seen.add(cleanNumber)) continue
            val displayName = rawName.trim().ifBlank { cleanNumber }
            results.add(ImportContact(name = displayName, phone = cleanNumber))
        }
    }

    return results
}
