package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import android.content.Intent
import android.net.Uri
import com.zeynbakers.order_management_system.order.data.OrderImportParser
import com.zeynbakers.order_management_system.order.data.ImportResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.os.Build
import java.io.File
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderExporter
import com.zeynbakers.order_management_system.product.data.ProductEntity
import com.zeynbakers.order_management_system.order.printing.BluetoothPrintPermissions
import com.zeynbakers.order_management_system.order.printing.BluetoothPrinterManager
import com.zeynbakers.order_management_system.order.printing.PairedBluetoothPrinter
import com.zeynbakers.order_management_system.order.printing.PrinterPreferences
import com.zeynbakers.order_management_system.order.printing.ReceiptFormatter
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayDetailScreen(
        date: LocalDate,
        orders: List<OrderEntity>,
        dayTotal: BigDecimal,
        customerNames: Map<Long, String>,
        customerPhones: Map<Long, String>,
        orderPaidAmounts: Map<Long, BigDecimal>,
        onBack: () -> Unit,
        onSaveOrder: (String, BigDecimal, String, String, String?, Long?, List<CartItem>) -> Unit,
        onDeleteOrder: (Long) -> Unit,
        loadOrderPaymentAllocations: suspend (Long) -> List<OrderPaymentAllocationUi>,
        loadMoveOrderOptions: suspend (Long?, Long) -> List<OrderMoveOption>,
        onDeleteOrderWithPayments:
                suspend (
                        Long,
                        LocalDate,
                        List<Long>,
                        OrderPaymentAction,
                        ReceiptAllocation?,
                        Boolean) -> Boolean,
        onOrderPaymentHistory: (Long) -> Unit,
        onReceivePayment: (OrderEntity) -> Unit,
        loadCustomerById: suspend (Long) -> CustomerEntity?,
        searchCustomers: suspend (String) -> List<CustomerEntity>,
        searchProducts: suspend (String) -> List<ProductEntity>,
        ensureProduct: suspend (String, BigDecimal, String) -> ProductEntity,
        onImportOrders: (List<OrderImportAction>) -> Unit = {},
        initialFocusOrderId: Long? = null,
        draft: OrderDraft?,
        onDraftChange: (OrderDraft?) -> Unit,
        storeName: String = ""
) {
    val dateKey = remember(date) { date.toString() }
    var notes by rememberSaveable(dateKey) { mutableStateOf(draft?.notes ?: "") }
    var totalText by rememberSaveable(dateKey) { mutableStateOf(draft?.totalText ?: "") }
    var customerName by rememberSaveable(dateKey) { mutableStateOf(draft?.customerName ?: "") }
    var customerPhone by rememberSaveable(dateKey) { mutableStateOf(draft?.customerPhone ?: "") }
    var pickupTimeText by rememberSaveable(dateKey) { mutableStateOf(draft?.pickupTime ?: "") }
    var editingOrderId by rememberSaveable(dateKey) { mutableStateOf<Long?>(draft?.editingOrderId) }
    var isEditorOpen by rememberSaveable(dateKey) { mutableStateOf(false) }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var customerConfirmed by rememberSaveable(dateKey) {
        mutableStateOf((draft?.customerPhone ?: "").isNotBlank())
    }
    var productQuery by remember { mutableStateOf("") }
    var productMatches by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var suppressedNoMatchQuery by rememberSaveable(dateKey) { mutableStateOf("") }
    var pendingDeleteOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var deleteAllocations by remember {
        mutableStateOf<List<OrderPaymentAllocationUi>>(emptyList())
    }
    var deleteSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var deleteAction by remember { mutableStateOf(OrderPaymentAction.MOVE) }
    var deleteMoveTarget by remember { mutableStateOf(DeleteMoveTarget.OLDEST_ORDERS) }
    var deleteMoveOrderOptions by remember { mutableStateOf<List<OrderMoveOption>>(emptyList()) }
    var deleteSelectedOrderId by remember { mutableStateOf<Long?>(null) }
    var deleteMoveFullReceipts by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val printerPrefs = remember { PrinterPreferences(context) }
    val printerManager = remember(context) { BluetoothPrinterManager(context) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<PairedBluetoothPrinter>>(emptyList()) }
    var pendingPrintOrderId by remember { mutableStateOf<Long?>(null) }
    var printTargetOrderId by remember { mutableStateOf<Long?>(null) }
    var forcePrinterPicker by remember { mutableStateOf(false) }
    val printSuccessMessage = stringResource(R.string.order_print_success)
    val printFailedMessage = stringResource(R.string.order_print_failed)
    val permissionDeniedMessage = stringResource(R.string.order_print_permission_denied)
    var isExportDialogOpen by remember { mutableStateOf(false) }
    var isImportDialogOpen by remember { mutableStateOf(false) }
    var importData by remember { mutableStateOf<com.zeynbakers.order_management_system.order.data.OrderExportData?>(null) }
    var showImportPreview by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("json") }
    val amountRegistry = LocalAmountFieldRegistry.current
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceRouter = LocalVoiceInputRouter.current

    suspend fun printOrder(order: OrderEntity, macAddress: String, printerName: String) {
        val customerLabel = order.customerId?.let { customerNames[it] }
        val customerPhone = order.customerId?.let { customerPhones[it] }
        val receiptText = ReceiptFormatter.formatOrder(storeName, order, customerLabel, customerPhone)
        val result = printerManager.printReceipt(macAddress, receiptText)
        if (result.isSuccess) {
            printerPrefs.savePrinter(macAddress, printerName)
            snackbarHostState.showSnackbar(printSuccessMessage)
        } else {
            val detail =
                result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Printer error"
            snackbarHostState.showSnackbar(printFailedMessage.format(detail))
        }
    }

    suspend fun proceedToPrint(order: OrderEntity) {
        printTargetOrderId = order.id
        val savedMac = if (forcePrinterPicker) null else printerPrefs.getPrinterMac()
        forcePrinterPicker = false
        if (savedMac != null) {
            printOrder(
                order = order,
                macAddress = savedMac,
                printerName = printerPrefs.getPrinterName() ?: savedMac
            )
        } else {
            pairedPrinters = printerManager.getPairedPrinters()
            showPrinterPicker = true
        }
    }

    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            val allGranted = grants.values.all { it }
            val orderId = pendingPrintOrderId
            pendingPrintOrderId = null
            if (!allGranted || orderId == null) {
                scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
                return@rememberLauncherForActivityResult
            }
            val order = orders.firstOrNull { it.id == orderId } ?: return@rememberLauncherForActivityResult
            scope.launch { proceedToPrint(order) }
        }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (content != null) {
                        when (val result = OrderImportParser.parse(content)) {
                            is ImportResult.Success -> {
                                importData = result.data
                                showImportPreview = true
                            }
                            is ImportResult.Error -> {
                                snackbarHostState.showSnackbar("Import failed: ${result.message}")
                            }
                        }
                    } else {
                        snackbarHostState.showSnackbar("Failed to read file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error reading file: ${e.message}")
                }
            }
        }
    }

    fun requestPrintReceipt(order: OrderEntity, changePrinter: Boolean = false) {
        printTargetOrderId = order.id
        forcePrinterPicker = changePrinter
        if (!BluetoothPrintPermissions.hasAll(context)) {
            pendingPrintOrderId = order.id
            bluetoothPermissionLauncher.launch(BluetoothPrintPermissions.requiredPermissions())
            return
        }
        scope.launch { proceedToPrint(order) }
    }
    var orderFilter by rememberSaveable(dateKey) { mutableStateOf(DayOrderFilter.All) }
    var searchQuery by rememberSaveable(dateKey) { mutableStateOf("") }
    var isSearchExpanded by rememberSaveable(dateKey) { mutableStateOf(false) }
    var pendingFocusOrderId by rememberSaveable(dateKey) { mutableStateOf(initialFocusOrderId) }
    var highlightedOrderId by rememberSaveable(dateKey) { mutableStateOf(initialFocusOrderId) }
    val listState = rememberLazyListState()
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("en-KE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    LaunchedEffect(notes, totalText, customerName, customerPhone, pickupTimeText, editingOrderId) {
        val hasDraftContent =
                notes.isNotBlank() ||
                        totalText.isNotBlank() ||
                        customerName.isNotBlank() ||
                        customerPhone.isNotBlank() ||
                        pickupTimeText.isNotBlank() ||
                        editingOrderId != null
        if (hasDraftContent) {
            onDraftChange(
                    OrderDraft(
                            notes = notes,
                            totalText = totalText,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            pickupTime = pickupTimeText,
                            editingOrderId = editingOrderId
                    )
            )
        } else {
            onDraftChange(null)
        }
    }
    LaunchedEffect(editingOrderId) {
        if (editingOrderId == null) {
            customerName = ""
            customerPhone = ""
            pickupTimeText = ""
            return@LaunchedEffect
        }
        val order = orders.firstOrNull { it.id == editingOrderId }
        if (order == null) {
            customerName = ""
            customerPhone = ""
            pickupTimeText = ""
            return@LaunchedEffect
        }
        pickupTimeText = order.pickupTime.orEmpty()
        val customerId = order.customerId
        if (customerId == null) {
            customerName = ""
            customerPhone = ""
            return@LaunchedEffect
        }
        val customer = loadCustomerById(customerId) ?: return@LaunchedEffect
        customerName = customer.name
        customerPhone = customer.phone
        customerConfirmed = true
    }
    LaunchedEffect(isEditorOpen) { overlaySuppressed.value = isEditorOpen }
    DisposableEffect(Unit) { onDispose { overlaySuppressed.value = false } }
    LaunchedEffect(customerName, customerPhone, customerConfirmed) {
        if (customerConfirmed) {
            suggestions = emptyList()
            suppressedNoMatchQuery = ""
            return@LaunchedEffect
        }
        val query = customerName.trim()
        val normalizedQuery = query.lowercase()
        if (normalizedQuery.isBlank()) {
            suggestions = emptyList()
            suppressedNoMatchQuery = ""
            return@LaunchedEffect
        }
        if (
                suppressedNoMatchQuery.isNotBlank() &&
                        normalizedQuery.startsWith(suppressedNoMatchQuery)
        ) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        val matches = searchCustomers(query)
        suggestions = matches
        suppressedNoMatchQuery = if (matches.isEmpty()) normalizedQuery else ""
    }
    LaunchedEffect(productQuery) {
        delay(250)
        productMatches =
            if (productQuery.isBlank()) {
                emptyList()
            } else {
                searchProducts(productQuery)
            }
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchExpanded = true
        }
    }
    LaunchedEffect(dateKey, initialFocusOrderId) {
        if (initialFocusOrderId != null) {
            orderFilter = DayOrderFilter.All
            searchQuery = ""
            isSearchExpanded = false
            pendingFocusOrderId = initialFocusOrderId
            highlightedOrderId = initialFocusOrderId
        }
    }
    LaunchedEffect(pendingDeleteOrder?.id) {
        val order = pendingDeleteOrder ?: return@LaunchedEffect
        deleteAllocations = loadOrderPaymentAllocations(order.id)
        deleteSelection = deleteAllocations.map { it.allocationId }.toSet()
        deleteAction = OrderPaymentAction.MOVE
        deleteMoveTarget =
                if (order.customerId != null) {
                    DeleteMoveTarget.OLDEST_ORDERS
                } else {
                    DeleteMoveTarget.ORDER
                }
        deleteMoveOrderOptions = loadMoveOrderOptions(order.customerId, order.id)
        deleteSelectedOrderId = deleteMoveOrderOptions.firstOrNull()?.orderId
        deleteMoveFullReceipts = true
    }
    BackHandler(enabled = isEditorOpen) { isEditorOpen = false }
    BackHandler(enabled = !isEditorOpen, onBack = onBack)
    val dayStats =
            remember(orders, orderPaidAmounts, dayTotal) {
                computeDayStats(orders, orderPaidAmounts, dayTotal)
            }
    val dayOfWeekLabel = remember(date) { titleCase(date.dayOfWeek.name) }
    val monthLabel = remember(date) { titleCase(date.month.name) }
    val dateLabel = remember(date, monthLabel) { "$monthLabel ${date.dayOfMonth}, ${date.year}" }
    val filteredOrders by
            remember(orders, orderFilter, orderPaidAmounts, searchQuery, customerNames) {
                derivedStateOf {
                    val normalizedQuery = searchQuery.trim().lowercase()
                    val filtered = orders.filter { order ->
                        val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
                        val paymentState = resolvePaymentState(order.totalAmount, paidAmount)
                        val matchesStatus =
                                when (orderFilter) {
                                    DayOrderFilter.All -> true
                                    DayOrderFilter.Due ->
                                        paymentState == PaymentState.UNPAID || paymentState == PaymentState.PARTIAL
                                    DayOrderFilter.NoPayment -> paymentState == PaymentState.UNPAID
                                    DayOrderFilter.Partial -> paymentState == PaymentState.PARTIAL
                                    DayOrderFilter.Paid -> paymentState == PaymentState.PAID
                                    DayOrderFilter.Overpaid -> paymentState == PaymentState.OVERPAID
                                }
                        if (!matchesStatus) return@filter false
                        if (normalizedQuery.isBlank()) return@filter true
                        val customerLabel =
                                order.customerId?.let { customerNames[it] }.orEmpty().lowercase()
                        val amountLabel = order.totalAmount.stripTrailingZeros().toPlainString().lowercase()
                        val pickupLabel = plannerPickupDisplay(order.pickupTime).orEmpty().lowercase()
                        val pickupRaw = order.pickupTime.orEmpty().lowercase()
                        (order.notes?.lowercase()?.contains(normalizedQuery) == true) ||
                                customerLabel.contains(normalizedQuery) ||
                                amountLabel.contains(normalizedQuery) ||
                                pickupLabel.contains(normalizedQuery) ||
                                pickupRaw.contains(normalizedQuery)
                    }
                    sortOrdersForPlanner(filtered)
                }
            }
    LaunchedEffect(pendingFocusOrderId, filteredOrders) {
        val targetOrderId = pendingFocusOrderId ?: return@LaunchedEffect
        val targetIndex = filteredOrders.indexOfFirst { it.id == targetOrderId }
        if (targetIndex < 0) {
            return@LaunchedEffect
        }
        // First two items are the summary card and filter/search controls.
        listState.animateScrollToItem(targetIndex + 2)
        pendingFocusOrderId = null
        delay(2200)
        if (highlightedOrderId == targetOrderId) {
            highlightedOrderId = null
        }
    }
    val onBackClick = {
        if (isEditorOpen) {
            isEditorOpen = false
        } else {
            onBack()
        }
    }
    val emptyStateRes: Pair<Int, Int> =
            remember(orders, orderFilter, searchQuery) {
                dayEmptyStateRes(
                        orders = orders,
                        orderFilter = orderFilter,
                        searchQuery = searchQuery
                )
            }
    val emptyTitleRes = emptyStateRes.first
    val emptySubtitleRes = emptyStateRes.second
    Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Text(
                                        text = dayOfWeekLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isExportDialogOpen = true }) {
                                Icon(
                                    imageVector = Icons.Filled.IosShare,
                                    contentDescription = stringResource(R.string.day_export_orders)
                                )
                            }
                            IconButton(onClick = { isImportDialogOpen = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FileUpload,
                                    contentDescription = stringResource(R.string.day_import_orders)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors()
                )
            },
            floatingActionButton = {
                val existingDraft = draft
                val canRestoreDraft =
                        existingDraft?.let { draftValue ->
                            draftValue.editingOrderId == null &&
                                    (draftValue.notes.isNotBlank() ||
                                            draftValue.totalText.isNotBlank() ||
                                            draftValue.customerName.isNotBlank() ||
                                            draftValue.customerPhone.isNotBlank())
                        }
                                ?: false
                FloatingActionButton(
                        onClick = {
                            editingOrderId = null
                            if (!canRestoreDraft) {
                                notes = ""
                                totalText = ""
                                customerName = ""
                                customerPhone = ""
                            }
                            notesError = null
                            totalError = null
                            customerError = null
                            isEditorOpen = true
                        }
                ) {
                    Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.day_add_order)
                    )
                }
            }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding)
        ) {
            item { DaySummaryCard(dayTotal = dayTotal, stats = dayStats) }
            if (orders.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        AppFilterRow(
                                options = dayOrderFilterOptions(orders.size, dayStats),
                                selectedKey = orderFilter.name,
                                onSelect = { selected ->
                                    orderFilter =
                                        runCatching { DayOrderFilter.valueOf(selected) }
                                            .getOrDefault(DayOrderFilter.All)
                                },
                                showMoreAsIcon = true
                        )
                        Spacer(Modifier.height(8.dp))
                        val orderCountLabel =
                                if (searchQuery.isBlank() && orderFilter == DayOrderFilter.All) {
                                    stringResource(R.string.day_orders_count, filteredOrders.size)
                                } else {
                                    stringResource(
                                            R.string.day_showing_orders_count,
                                            filteredOrders.size,
                                            orders.size
                                    )
                                }
                        val searchVisible = isSearchExpanded
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = orderCountLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                    onClick = {
                                        if (searchVisible) {
                                            isSearchExpanded = false
                                            searchQuery = ""
                                        } else {
                                            isSearchExpanded = true
                                        }
                                    }
                            ) {
                                Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.action_search)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                        text =
                                                if (searchVisible) {
                                                    stringResource(R.string.day_hide_search)
                                                } else {
                                                    stringResource(R.string.day_show_search)
                                                }
                                )
                            }
                        }
                        AnimatedVisibility(
                                visible = searchVisible,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        label = {
                                            Text(stringResource(R.string.day_search_orders))
                                        },
                                        placeholder = {
                                            Text(
                                                    stringResource(
                                                            R.string.day_search_notes_customer_amount_pickup
                                                    )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                    imageVector = Icons.Filled.Search,
                                                    contentDescription =
                                                            stringResource(R.string.action_search)
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(
                                                            imageVector = Icons.Filled.Close,
                                                            contentDescription =
                                                                    stringResource(
                                                                            R.string
                                                                                    .day_clear_search
                                                                    )
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        val activeContextLabel =
                                when {
                                    orderFilter != DayOrderFilter.All &&
                                            searchQuery.isNotBlank() -> {
                                        stringResource(
                                                R.string.day_active_context_filter_search,
                                                stringResource(orderFilter.labelRes),
                                                searchQuery
                                        )
                                    }
                                    orderFilter != DayOrderFilter.All -> {
                                        stringResource(
                                                R.string.day_active_context_filter_only,
                                                stringResource(orderFilter.labelRes)
                                        )
                                    }
                                    searchQuery.isNotBlank() -> {
                                        stringResource(
                                                R.string.day_active_context_search_only,
                                                searchQuery
                                        )
                                    }
                                    else -> null
                                }
                        if (activeContextLabel != null) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                        text = activeContextLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 6.dp
                                                )
                                )
                            }
                        }
                    }
                }
            }
            if (filteredOrders.isEmpty()) {
                item {
                    EmptyDayState(
                            title = stringResource(emptyTitleRes),
                            subtitle = stringResource(emptySubtitleRes)
                    )
                }
            } else {
                items(items = filteredOrders, key = { it.id }) { order ->
                    val customerLabel =
                            order.customerId?.let { customerNames[it] }?.takeIf { it.isNotBlank() }
                    val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
                    val paymentState = resolvePaymentState(order.totalAmount, paidAmount)
                    val dismissState =
                            rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        when (it) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                pendingDeleteOrder = order
                                                false
                                            }
                                            SwipeToDismissBoxValue.StartToEnd,
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    }
                            )
                    SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = { DayDeleteSwipeBackground(dismissState) }
                    ) {
                        OrderListItem(
                                order = order,
                                customerLabel = customerLabel,
                                paidAmount = paidAmount,
                                paymentState = paymentState,
                                isFocused = highlightedOrderId == order.id,
                                onEdit = {
                                    notes = order.notes ?: ""
                                    totalText = order.totalAmount.toPlainString()
                                    editingOrderId = order.id
                                    notesError = null
                                    totalError = null
                                    customerError = null
                                    isEditorOpen = true
                                },
                                onPaymentHistory = { onOrderPaymentHistory(order.id) },
                                onReceivePayment = { onReceivePayment(order) },
                                onPrintReceipt = { requestPrintReceipt(order) }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
    DayOrderEditorDialog(
        orderDate = date,
        isEditorOpen = isEditorOpen,
        editingOrderId = editingOrderId,
        orderPaidAmounts = orderPaidAmounts,
        totalText = totalText,
        notes = notes,
        pickupTimeText = pickupTimeText,
        customerName = customerName,
        customerPhone = customerPhone,
        suggestions = suggestions,
        notesError = notesError,
        totalError = totalError,
        customerError = customerError,
        formatter = formatter,
        amountRegistry = amountRegistry,
        voiceRouter = voiceRouter,
        onSaveOrder = onSaveOrder,
        onDraftChange = onDraftChange,
        onSetNotes = { notes = it },
        onSetTotalText = { totalText = it },
        onSetCustomerName = { customerName = it },
        onSetCustomerPhone = { customerPhone = it },
        onSetPickupTimeText = { pickupTimeText = it },
        onSetEditingOrderId = { editingOrderId = it },
        onSetSuggestions = { suggestions = it },
        onSetNotesError = { notesError = it },
        onSetTotalError = { totalError = it },
        onSetCustomerError = { customerError = it },
        onSetEditorOpen = { isEditorOpen = it },
        customerConfirmed = customerConfirmed,
        onSetCustomerConfirmed = { customerConfirmed = it },
        productMatches = productMatches,
        onProductQueryChange = { productQuery = it },
        onEnsureProduct = ensureProduct
    )
    if (showPrinterPicker) {
        val orderId = printTargetOrderId
        BluetoothPrinterPickerDialog(
            printers = pairedPrinters,
            onDismiss = {
                showPrinterPicker = false
                printTargetOrderId = null
            },
            onPrinterSelected = { printer ->
                showPrinterPicker = false
                val order = orderId?.let { id -> orders.firstOrNull { it.id == id } }
                if (order != null) {
                    scope.launch {
                        printOrder(order, printer.macAddress, printer.name)
                    }
                }
            },
            onChangePrinter =
                if (printerPrefs.getPrinterMac() != null) {
                    {
                        showPrinterPicker = false
                        val order = orderId?.let { id -> orders.firstOrNull { it.id == id } }
                        if (order != null) {
                            requestPrintReceipt(order, changePrinter = true)
                        }
                    }
                } else {
                    null
                }
        )
    }
    DayDeleteOrderDialog(
            pendingDeleteOrder = pendingDeleteOrder,
            customerNames = customerNames,
            date = date,
            deleteAllocations = deleteAllocations,
            deleteSelection = deleteSelection,
            deleteAction = deleteAction,
            deleteMoveTarget = deleteMoveTarget,
            deleteMoveOrderOptions = deleteMoveOrderOptions,
            deleteSelectedOrderId = deleteSelectedOrderId,
            deleteMoveFullReceipts = deleteMoveFullReceipts,
            onSetPendingDeleteOrder = { pendingDeleteOrder = it },
            onSetDeleteSelection = { deleteSelection = it },
            onSetDeleteAction = { deleteAction = it },
            onSetDeleteMoveTarget = { deleteMoveTarget = it },
            onSetDeleteSelectedOrderId = { deleteSelectedOrderId = it },
            onSetDeleteMoveFullReceipts = { deleteMoveFullReceipts = it },
            onDeleteOrder = onDeleteOrder,
            onDeleteOrderWithPayments = onDeleteOrderWithPayments
    )

    // Export Dialog
    if (isExportDialogOpen) {
        val exportSuccessMessage = stringResource(R.string.day_export_success)
        val exportFailedMessage = stringResource(R.string.day_export_failed, "")
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isExportDialogOpen = false },
            title = { Text(stringResource(R.string.day_export_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.day_export_message, orders.size, date))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = exportFormat == "json",
                            onClick = { exportFormat = "json" },
                            label = { Text(stringResource(R.string.day_export_format_json)) }
                        )
                        androidx.compose.material3.FilterChip(
                            selected = exportFormat == "csv",
                            onClick = { exportFormat = "csv" },
                            label = { Text(stringResource(R.string.day_export_format_csv)) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isExportDialogOpen = false
                        scope.launch {
                            val (data, mimeType, fileName) = if (exportFormat == "json") {
                                Triple(
                                    OrderExporter.exportOrders(orders, customerNames, customerPhones),
                                    "application/json",
                                    "orders_$date.json"
                                )
                            } else {
                                Triple(
                                    OrderExporter.exportOrdersToCsv(orders, customerNames, customerPhones),
                                    "text/csv",
                                    "orders_$date.csv"
                                )
                            }
                            
                            try {
                                val cacheDir = File(context.cacheDir, "exports")
                                if (!cacheDir.exists()) {
                                    cacheDir.mkdirs()
                                }
                                val file = File(cacheDir, fileName)
                                file.writeText(data)
                                
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Orders for $date")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                
                                context.startActivity(Intent.createChooser(intent, "Export orders"))
                                snackbarHostState.showSnackbar(exportSuccessMessage)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(exportFailedMessage.format(e.message ?: ""))
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.day_export_orders))
                }
            },
            dismissButton = {
                TextButton(onClick = { isExportDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Import Dialog
    if (isImportDialogOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isImportDialogOpen = false },
            title = { Text(stringResource(R.string.day_import_title)) },
            text = { Text(stringResource(R.string.day_import_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isImportDialogOpen = false
                        filePickerLauncher.launch("*/*")
                    }
                ) {
                    Text(stringResource(R.string.day_import_pick_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { isImportDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Import Preview Dialog
    val importingMessage = stringResource(R.string.day_importing_orders, 0)
    
    DayImportPreviewDialog(
        isOpen = showImportPreview,
        importData = importData,
        existingOrders = orders,
        currentDate = date,
        onDismiss = { 
            showImportPreview = false
            importData = null
        },
        onConfirmImport = { actions ->
            onImportOrders(actions)
            showImportPreview = false
            importData = null
            scope.launch {
                snackbarHostState.showSnackbar(importingMessage.format(actions.size))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDeleteSwipeBackground(dismissState: SwipeToDismissBoxState) {
    if (dismissState.dismissDirection != SwipeToDismissBoxValue.EndToStart) return
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(999.dp)
        ) {
            Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(6.dp))
                Text(
                        text = stringResource(R.string.day_delete_order),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
