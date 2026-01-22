package com.zeynbakers.order_management_system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.backup.BackupScheduler
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.navigation.AppShortcuts
import com.zeynbakers.order_management_system.core.navigation.extractSharedText
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen
import com.zeynbakers.order_management_system.core.widget.WidgetUpdater
import com.zeynbakers.order_management_system.core.ui.AmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.VoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.VoiceCalculatorOverlay
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumber
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import com.zeynbakers.order_management_system.customer.ui.ImportContactsScreen
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.OrderDraft
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import com.zeynbakers.order_management_system.order.ui.UnpaidOrdersScreen
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val launchIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchIntent.value = intent

        setContent {
            Order_management_systemTheme {
                val database = remember { DatabaseProvider.getDatabase(applicationContext) }
                val viewModel = remember { OrderViewModel(database = database) }
                val customerViewModel = remember { CustomerAccountsViewModel(database = database) }
                val paymentIntakeViewModel = remember { PaymentIntakeViewModel(database = database) }
                val paymentHistoryViewModel = remember { PaymentIntakeHistoryViewModel(database = database) }
                val amountRegistry = remember { AmountFieldRegistry() }
                val voiceRouter = remember { VoiceInputRouter(onApplyTotal = amountRegistry::applyAmount) }
                var currentMonth by remember { mutableStateOf(0) }
                var currentYear by remember { mutableStateOf(0) }
                var baseMonth by remember { mutableStateOf(0) }
                var baseYear by remember { mutableStateOf(0) }
                var screen by remember { mutableStateOf<Screen>(Screen.Calendar) }
                var customerQuery by remember { mutableStateOf("") }
                val context = LocalContext.current
                var importContacts by remember { mutableStateOf<List<ImportContact>>(emptyList()) }
                var selectedContactPhones by remember { mutableStateOf<Set<String>>(emptySet()) }
                var isContactsLoading by remember { mutableStateOf(false) }
                var paymentIntakeText by remember { mutableStateOf<String?>(null) }
                var paymentReturnScreen by remember { mutableStateOf<Screen>(Screen.Calendar) }
                var paymentHistoryReturnScreen by remember { mutableStateOf<Screen>(Screen.Calendar) }
                var hasRecordPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val overlaySuppressed = remember { mutableStateOf(false) }

                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted: Boolean ->
                    if (granted) {
                        screen = Screen.ImportContacts
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
                val dayDrafts = remember { mutableStateMapOf<LocalDate, OrderDraft>() }

                val calendarDays by viewModel.calendarDays.collectAsState()
                val monthTotal by viewModel.monthTotal.collectAsState()
                val monthBadgeCount by viewModel.monthBadgeCount.collectAsState()
                val ordersForDate by viewModel.ordersForDate.collectAsState()
                val dayTotal by viewModel.dayTotal.collectAsState()
                val orderCustomerNames by viewModel.orderCustomerNames.collectAsState()
                val orderPaidAmounts by viewModel.orderPaidAmounts.collectAsState()
                val monthSnapshots by viewModel.monthSnapshots.collectAsState()
                val summaryOrders by viewModel.summaryOrders.collectAsState()
                val summaryTotal by viewModel.summaryTotal.collectAsState()
                val summaryCustomerNames by viewModel.summaryCustomerNames.collectAsState()
                val unpaidOrders by viewModel.unpaidOrders.collectAsState()
                val unpaidPaidAmounts by viewModel.unpaidPaidAmounts.collectAsState()
                val unpaidCustomerNames by viewModel.unpaidCustomerNames.collectAsState()

                val customerSummaries by customerViewModel.summaries.collectAsState()
                val customerDetail by customerViewModel.customer.collectAsState()
                val customerLedger by customerViewModel.ledger.collectAsState()
                val customerBalance by customerViewModel.balance.collectAsState()
                val customerOrders by customerViewModel.orders.collectAsState()

                var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
                var summaryDate by remember { mutableStateOf<LocalDate?>(null) }
                var quickAddDate by remember { mutableStateOf<LocalDate?>(null) }
                val incomingIntent by launchIntent
                val today = remember {
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                }
                val openPaymentIntake: (String?, Screen) -> Unit = openPaymentIntake@{ sharedText, returnTo ->
                    val isActive = screen is Screen.PaymentIntake
                    if (!isActive) {
                        val safeReturn =
                            if (returnTo is Screen.PaymentIntake) Screen.Calendar else returnTo
                        paymentReturnScreen = safeReturn
                    }
                    if (!sharedText.isNullOrBlank()) {
                        if (isActive) {
                            paymentIntakeViewModel.appendRawText(sharedText)
                            return@openPaymentIntake
                        }
                        paymentIntakeText = sharedText
                    } else if (!isActive) {
                        paymentIntakeViewModel.setRawText("")
                        paymentIntakeText = null
                    }
                    if (!isActive) {
                        screen = Screen.PaymentIntake(sharedText)
                    }
                }
                val openPaymentHistory: (PaymentHistoryFilter) -> Unit = { filter ->
                    val isActive = screen is Screen.PaymentIntakeHistory
                    if (!isActive) {
                        val safeReturn =
                            if (screen is Screen.PaymentIntakeHistory) Screen.Calendar else screen
                        paymentHistoryReturnScreen = safeReturn
                    }
                    screen = Screen.PaymentIntakeHistory(filter)
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

                LaunchedEffect(currentMonth, currentYear) {
                    if (currentMonth > 0 && currentYear > 0) {
                        viewModel.loadMonth(month = currentMonth, year = currentYear)
                        viewModel.prefetchAdjacentMonths(year = currentYear, month = currentMonth)
                    }
                }

                LaunchedEffect(incomingIntent) {
                    val intent = incomingIntent ?: return@LaunchedEffect
                    when (intent.action) {
                        AppIntents.ACTION_SHOW_TODAY -> {
                            selectedDate = today
                            viewModel.loadOrdersForDate(today)
                            screen = Screen.Day(today)
                        }
                        AppIntents.ACTION_SHOW_DAY -> {
                            val targetDate =
                                intent.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: today
                            selectedDate = targetDate
                            viewModel.loadOrdersForDate(targetDate)
                            screen = Screen.Day(targetDate)
                        }
                        AppIntents.ACTION_SHOW_UNPAID -> {
                            viewModel.loadUnpaidOrders()
                            screen = Screen.Unpaid
                        }
                        AppIntents.ACTION_SHOW_SUMMARY -> {
                            summaryDate =
                                intent.getStringExtra(AppIntents.EXTRA_TARGET_DATE)?.let {
                                    runCatching { LocalDate.parse(it) }.getOrNull()
                                } ?: today
                            screen = Screen.Summary
                        }
                        AppIntents.ACTION_NEW_ORDER -> {
                            selectedDate = today
                            quickAddDate = today
                            screen = Screen.Calendar
                        }
                        Intent.ACTION_SEND,
                        Intent.ACTION_SEND_MULTIPLE -> {
                            val sharedText = extractSharedText(intent) ?: return@LaunchedEffect
                            openPaymentIntake(sharedText, screen)
                        }
                    }
                }

                LaunchedEffect(screen, customerQuery) {
                    val currentScreen = screen
                    if (currentScreen is Screen.CustomerList) {
                        customerViewModel.searchCustomers(customerQuery)
                    }
                }

                LaunchedEffect(screen) {
                    val currentScreen = screen
                    if (currentScreen is Screen.CustomerDetail) {
                        customerViewModel.loadCustomer(currentScreen.customerId)
                    }
                }

                LaunchedEffect(screen) {
                    val currentScreen = screen
                    if (currentScreen is Screen.ImportContacts) {
                        isContactsLoading = true
                        importContacts =
                            withContext(Dispatchers.IO) {
                                loadAllContacts(context)
                            }
                        selectedContactPhones = emptySet()
                        isContactsLoading = false
                    }
                }

                LaunchedEffect(screen) {
                    val currentScreen = screen
                    if (currentScreen is Screen.Unpaid) {
                        viewModel.loadUnpaidOrders()
                    }
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

                CompositionLocalProvider(
                    LocalAmountFieldRegistry provides amountRegistry,
                    LocalVoiceCalcAccess provides voiceCalcAccess,
                    LocalVoiceOverlaySuppressed provides overlaySuppressed,
                    LocalVoiceInputRouter provides voiceRouter
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val active = screen) {
                            Screen.Calendar -> {
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
                                    onSaveOrder = { date, notes, total, name, phone, pickupTime ->
                                        viewModel.saveOrder(
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
                                    searchCustomers = { query -> viewModel.searchCustomers(query) },
                                    onCustomersClick = { screen = Screen.CustomerList },
                                    onSummaryClick = {
                                        val date = selectedDate ?: today
                                        summaryDate = date
                                        screen = Screen.Summary
                                    },
                                    onMonthSettled = { year, month ->
                                        currentYear = year
                                        currentMonth = month
                                    },
                                    onOpenDay = { date ->
                                        viewModel.loadOrdersForDate(date)
                                        screen = Screen.Day(date)
                                    },
                                    openQuickAddDate = quickAddDate,
                                    onQuickAddConsumed = { quickAddDate = null }
                                )
                            }
                            is Screen.Day -> {
                                DayDetailScreen(
                                    date = active.date,
                                    orders = ordersForDate,
                                    dayTotal = dayTotal,
                                    customerNames = orderCustomerNames,
                                    orderPaidAmounts = orderPaidAmounts,
                                    onBack = { screen = Screen.Calendar },
                                    onSaveOrder = { notes, total, name, phone, pickupTime, orderId ->
                                        viewModel.saveOrder(
                                            date = active.date,
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
                                        viewModel.cancelOrder(orderId, active.date)
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                    },
                                    loadCustomerById = { id -> viewModel.getCustomerById(id) },
                                    searchCustomers = { query -> viewModel.searchCustomers(query) },
                                    draft = dayDrafts[active.date],
                                    onDraftChange = { updated ->
                                        if (updated == null) {
                                            dayDrafts.remove(active.date)
                                        } else {
                                            dayDrafts[active.date] = updated
                                        }
                                    }
                                )
                            }
                            Screen.Summary -> {
                                BackHandler { screen = Screen.Calendar }
                                SummaryScreen(
                                    monthLabel = monthLabel(currentYear, currentMonth),
                                    monthTotal = monthTotal,
                                    initialDate = summaryDate ?: selectedDate ?: today,
                                    orders = summaryOrders,
                                    rangeTotal = summaryTotal,
                                    customerNames = summaryCustomerNames,
                                    onAnchorDateChange = { updated -> summaryDate = updated },
                                    onLoadRange = { start, end ->
                                        viewModel.loadSummaryRange(startInclusive = start, endExclusive = end)
                                    },
                                    onPaymentIntakeClick = {
                                        openPaymentIntake(null, Screen.Summary)
                                    },
                                    onBackupClick = { screen = Screen.Backup },
                                    onNotificationsClick = { screen = Screen.Notifications },
                                    onBack = { screen = Screen.Calendar }
                                )
                            }
                            Screen.Backup -> {
                                BackHandler { screen = Screen.Summary }
                                BackupSettingsScreen(onBack = { screen = Screen.Summary })
                            }
                            Screen.CustomerList -> {
                                BackHandler { screen = Screen.Calendar }
                                CustomerListScreen(
                                    query = customerQuery,
                                    summaries = customerSummaries,
                                    onQueryChange = { customerQuery = it },
                                    onCustomerClick = { id ->
                                        screen = Screen.CustomerDetail(id)
                                    },
                                    onAddCustomer = {
                                        val hasPermission =
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_CONTACTS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            screen = Screen.ImportContacts
                                        } else {
                                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        }
                                    },
                                    onPaymentHistory = { customerId ->
                                        openPaymentHistory(PaymentHistoryFilter.Customer(customerId))
                                    },
                                    onBack = { screen = Screen.Calendar }
                                )
                            }
                            Screen.ImportContacts -> {
                                BackHandler { screen = Screen.CustomerList }
                                ImportContactsScreen(
                                    contacts = importContacts,
                                    selectedPhones = selectedContactPhones,
                                    isLoading = isContactsLoading,
                                    onBack = { screen = Screen.CustomerList },
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
                                            val contactsByPhone =
                                                importContacts.associateBy { it.phone }
                                            selectedContactPhones.forEach { phone ->
                                                val contact = contactsByPhone[phone] ?: return@forEach
                                                customerViewModel.importCustomer(contact.name, contact.phone)
                                            }
                                            screen = Screen.CustomerList
                                        }
                                    }
                                )
                            }
                            is Screen.CustomerDetail -> {
                                BackHandler { screen = Screen.CustomerList }
                                CustomerDetailScreen(
                                    customer = customerDetail,
                                    ledger = customerLedger,
                                    balance = customerBalance,
                                    orders = customerOrders,
                                    onBack = { screen = Screen.CustomerList },
                                    onRecordPayment = { amount, method, note, orderId ->
                                        customerViewModel.recordPayment(
                                            customerId = active.customerId,
                                            amount = amount,
                                            method = method,
                                            note = note,
                                            orderId = orderId
                                        )
                                        viewModel.loadUnpaidOrders()
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                    },
                                    onPaymentHistory = { customerId ->
                                        openPaymentHistory(PaymentHistoryFilter.Customer(customerId))
                                    },
                                    onOrderPaymentHistory = { orderId ->
                                        openPaymentHistory(PaymentHistoryFilter.Order(orderId))
                                    },
                                    onUpdateOrderStatusOverride = { orderId, override ->
                                        customerViewModel.setOrderStatusOverride(orderId, override)
                                        viewModel.loadUnpaidOrders()
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                    },
                                    onWriteOffOrder = { orderId ->
                                        customerViewModel.writeOffOrder(orderId)
                                        viewModel.loadUnpaidOrders()
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                    }
                                )
                            }
                            Screen.Unpaid -> {
                                BackHandler { screen = Screen.Calendar }
                                UnpaidOrdersScreen(
                                    orders = unpaidOrders,
                                    paidAmounts = unpaidPaidAmounts,
                                    customerNames = unpaidCustomerNames,
                                    onBack = { screen = Screen.Calendar },
                                    onOpenDay = { date ->
                                        selectedDate = date
                                        viewModel.loadOrdersForDate(date)
                                        screen = Screen.Day(date)
                                    }
                                )
                            }
                            Screen.Notifications -> {
                                BackHandler { screen = Screen.Summary }
                                NotificationSettingsScreen(onBack = { screen = Screen.Summary })
                            }
                            is Screen.PaymentIntake -> {
                                BackHandler { screen = paymentReturnScreen }
                                PaymentIntakeScreen(
                                    viewModel = paymentIntakeViewModel,
                                    initialText = active.sharedText ?: paymentIntakeText,
                                    onClose = {
                                        paymentIntakeText = null
                                        screen = paymentReturnScreen
                                    },
                                    onApplied = {
                                        if (currentMonth > 0 && currentYear > 0) {
                                            viewModel.loadMonth(month = currentMonth, year = currentYear)
                                        }
                                        viewModel.loadUnpaidOrders()
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                        paymentIntakeText = null
                                        screen = paymentReturnScreen
                                    }
                                )
                            }
                            is Screen.PaymentIntakeHistory -> {
                                BackHandler { screen = paymentHistoryReturnScreen }
                                PaymentIntakeHistoryScreen(
                                    viewModel = paymentHistoryViewModel,
                                    filter = active.filter,
                                    onBack = { screen = paymentHistoryReturnScreen },
                                    onOpenCustomer = { customerId ->
                                        screen = Screen.CustomerDetail(customerId)
                                    },
                                    onOpenOrder = { date ->
                                        selectedDate = date
                                        viewModel.loadOrdersForDate(date)
                                        screen = Screen.Day(date)
                                    },
                                    onRemoved = {
                                        if (currentMonth > 0 && currentYear > 0) {
                                            viewModel.loadMonth(month = currentMonth, year = currentYear)
                                        }
                                        viewModel.loadUnpaidOrders()
                                        val returnScreen = paymentHistoryReturnScreen
                                        if (returnScreen is Screen.CustomerDetail) {
                                            customerViewModel.loadCustomer(returnScreen.customerId)
                                        }
                                        if (returnScreen is Screen.Day) {
                                            viewModel.loadOrdersForDate(returnScreen.date)
                                        }
                                        WidgetUpdater.enqueue(context)
                                        NotificationScheduler.enqueueNow(context)
                                    }
                                )
                            }
                        }

                        VoiceCalculatorOverlay(
                            hasPermission = hasRecordPermission,
                            onRequestPermission = voiceCalcAccess.onRequestPermission,
                            isSuppressed = overlaySuppressed.value,
                            defaultIdleYDp = 72.dp
                        )
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

private sealed class Screen {
    data object Calendar : Screen()
    data class Day(val date: LocalDate) : Screen()
    data object Summary : Screen()
    data object Backup : Screen()
    data object Unpaid : Screen()
    data object Notifications : Screen()
    data object CustomerList : Screen()
    data object ImportContacts : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
    data class PaymentIntake(val sharedText: String?) : Screen()
    data class PaymentIntakeHistory(val filter: PaymentHistoryFilter) : Screen()
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

private fun loadAllContacts(context: Context): List<ImportContact> {
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
