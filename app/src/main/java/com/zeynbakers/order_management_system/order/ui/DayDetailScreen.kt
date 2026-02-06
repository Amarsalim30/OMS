package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceCalcAccess
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.core.util.normalizePickupTime
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.material3.FilterChip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    orders: List<OrderEntity>,
    dayTotal: BigDecimal,
    customerNames: Map<Long, String>,
    orderPaidAmounts: Map<Long, BigDecimal>,
    onBack: () -> Unit,
    onSaveOrder: (String, BigDecimal, String, String, String?, Long?) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    loadOrderPaymentAllocations: suspend (Long) -> List<OrderPaymentAllocationUi>,
    loadMoveOrderOptions: suspend (Long?, Long) -> List<OrderMoveOption>,
    onDeleteOrderWithPayments: suspend (
        Long,
        LocalDate,
        List<Long>,
        OrderPaymentAction,
        ReceiptAllocation?,
        Boolean
    ) -> Boolean,
    onOrderPaymentHistory: (Long) -> Unit,
    onReceivePayment: (OrderEntity) -> Unit,
    loadCustomerById: suspend (Long) -> CustomerEntity?,
    searchCustomers: suspend (String) -> List<CustomerEntity>,
    draft: OrderDraft?,
    onDraftChange: (OrderDraft?) -> Unit
) {
    var notes by rememberSaveable(date) { mutableStateOf(draft?.notes ?: "") }
    var totalText by rememberSaveable(date) { mutableStateOf(draft?.totalText ?: "") }
    var customerName by rememberSaveable(date) { mutableStateOf(draft?.customerName ?: "") }
    var customerPhone by rememberSaveable(date) { mutableStateOf(draft?.customerPhone ?: "") }
    var pickupTimeText by rememberSaveable(date) { mutableStateOf(draft?.pickupTime ?: "") }
    var editingOrderId by rememberSaveable(date) { mutableStateOf<Long?>(draft?.editingOrderId) }
    var isEditorOpen by rememberSaveable(date) { mutableStateOf(false) }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var pendingDeleteOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var deleteAllocations by remember { mutableStateOf<List<OrderPaymentAllocationUi>>(emptyList()) }
    var deleteSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var deleteAction by remember { mutableStateOf(OrderPaymentAction.MOVE) }
    var deleteMoveTarget by remember { mutableStateOf(DeleteMoveTarget.OLDEST_ORDERS) }
    var deleteMoveOrderOptions by remember { mutableStateOf<List<OrderMoveOption>>(emptyList()) }
    var deleteSelectedOrderId by remember { mutableStateOf<Long?>(null) }
    var deleteMoveFullReceipts by remember { mutableStateOf(true) }
    val amountRegistry = LocalAmountFieldRegistry.current
    val voiceCalcAccess = LocalVoiceCalcAccess.current
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceRouter = LocalVoiceInputRouter.current
    val scope = rememberCoroutineScope()
    var orderFilter by rememberSaveable { mutableStateOf(DayOrderFilter.All) }
    var searchQuery by rememberSaveable(date) { mutableStateOf("") }
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
    }

    LaunchedEffect(isEditorOpen) {
        overlaySuppressed.value = isEditorOpen
    }

    DisposableEffect(Unit) {
        onDispose {
            overlaySuppressed.value = false
        }
    }

    LaunchedEffect(customerName, customerPhone) {
        val query = when {
            customerName.isNotBlank() -> customerName
            customerPhone.isNotBlank() -> customerPhone
            else -> ""
        }
        suggestions = if (query.isBlank()) emptyList() else searchCustomers(query)
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

    BackHandler(enabled = !isEditorOpen, onBack = onBack)

    val dayStats = remember(orders, orderPaidAmounts, dayTotal) {
        computeDayStats(orders, orderPaidAmounts, dayTotal)
    }
    val dayOfWeekLabel = remember(date) { titleCase(date.dayOfWeek.name) }
    val monthLabel = remember(date) { titleCase(date.month.name) }
    val dateLabel = remember(date, monthLabel) { "$monthLabel ${date.dayOfMonth}, ${date.year}" }
    val filteredOrders by remember(
        orders,
        orderFilter,
        orderPaidAmounts,
        searchQuery,
        customerNames
    ) {
        derivedStateOf {
            val normalizedQuery = searchQuery.trim().lowercase()
            orders.filter { order ->
                val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
                val matchesStatus =
                    when (orderFilter) {
                        DayOrderFilter.All -> true
                        DayOrderFilter.Unpaid ->
                            resolvePaymentState(order.totalAmount, paidAmount) == PaymentState.UNPAID
                        DayOrderFilter.Partial ->
                            resolvePaymentState(order.totalAmount, paidAmount) == PaymentState.PARTIAL
                        DayOrderFilter.Paid ->
                            resolvePaymentState(order.totalAmount, paidAmount) == PaymentState.PAID
                        DayOrderFilter.Overpaid ->
                            resolvePaymentState(order.totalAmount, paidAmount) == PaymentState.OVERPAID
                    }
                if (!matchesStatus) return@filter false
                if (normalizedQuery.isBlank()) return@filter true
                val customerLabel =
                    order.customerId?.let { customerNames[it] }.orEmpty().lowercase()
                order.notes.lowercase().contains(normalizedQuery) ||
                    customerLabel.contains(normalizedQuery)
            }
        }
    }
    val onBackClick = {
        if (isEditorOpen) {
            isEditorOpen = false
        } else {
            onBack()
        }
    }
    val (emptyTitle, emptySubtitle) = remember(orders, orderFilter, searchQuery) {
        when {
            orders.isEmpty() ->
                Pair("No orders yet", "Tap + to add the first order.")
            searchQuery.isNotBlank() ->
                Pair("No matches", "Try a different name or note.")
            orderFilter == DayOrderFilter.Unpaid ->
                Pair("No unpaid orders", "All orders are paid or partial.")
            orderFilter == DayOrderFilter.Partial ->
                Pair("No partial payments", "No orders are partially paid.")
            orderFilter == DayOrderFilter.Paid ->
                Pair("No paid orders", "No orders are fully paid yet.")
            orderFilter == DayOrderFilter.Overpaid ->
                Pair("No overpaid orders", "No orders have extra payments.")
            else ->
                Pair("No orders", "Try a different filter.")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            val existingDraft = draft
            val canRestoreDraft = existingDraft?.let { draftValue ->
                draftValue.editingOrderId == null &&
                    (draftValue.notes.isNotBlank() ||
                        draftValue.totalText.isNotBlank() ||
                        draftValue.customerName.isNotBlank() ||
                        draftValue.customerPhone.isNotBlank())
            } ?: false
            FloatingActionButton(onClick = {
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
            }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add order")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                DaySummaryCard(
                    date = date,
                    dayTotal = dayTotal,
                    stats = dayStats
                )
            }

            if (orders.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOrderFilter.values().forEach { filter ->
                                val filterCount =
                                    when (filter) {
                                        DayOrderFilter.All -> orders.size
                                        DayOrderFilter.Unpaid -> dayStats.unpaidCount
                                        DayOrderFilter.Partial -> dayStats.partialCount
                                        DayOrderFilter.Paid -> dayStats.paidCount
                                        DayOrderFilter.Overpaid -> dayStats.overpaidCount
                                    }
                                val filterLabel =
                                    if (filter == DayOrderFilter.All) {
                                        "${filter.label} ($filterCount)"
                                    } else if (filterCount == 0) {
                                        filter.label
                                    } else {
                                        "${filter.label} ($filterCount)"
                                    }
                                FilterChip(
                                    selected = orderFilter == filter,
                                    onClick = { orderFilter = filter },
                                    label = { Text(filterLabel) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search orders") },
                            placeholder = { Text("Notes or customer") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        val orderCountLabel =
                            if (searchQuery.isBlank() && orderFilter == DayOrderFilter.All) {
                                "${filteredOrders.size} orders"
                            } else {
                                "Showing ${filteredOrders.size} of ${orders.size} orders"
                            }
                        Text(
                            text = orderCountLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    EmptyDayState(title = emptyTitle, subtitle = emptySubtitle)
                }
            } else {
                items(filteredOrders) { order ->
                    val customerLabel =
                        order.customerId?.let { customerNames[it] } ?: "No customer"
                    val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
                    val paymentState = resolvePaymentState(order.totalAmount, paidAmount)
                    OrderListItem(
                        order = order,
                        customerLabel = customerLabel,
                        paidAmount = paidAmount,
                        paymentState = paymentState,
                        onEdit = {
                            notes = order.notes
                            totalText = order.totalAmount.toPlainString()
                            editingOrderId = order.id
                            notesError = null
                            totalError = null
                            customerError = null
                            isEditorOpen = true
                        },
                        onDelete = {
                            pendingDeleteOrder = order
                        },
                        onPaymentHistory = { onOrderPaymentHistory(order.id) },
                        onReceivePayment = { onReceivePayment(order) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (isEditorOpen) {
        val paidAmount = editingOrderId?.let { orderPaidAmounts[it] } ?: BigDecimal.ZERO
        val trimmedTotal = totalText.trim()
        val parsedTotal = trimmedTotal.toBigDecimalOrNull()
        val currentTotal = parsedTotal ?: BigDecimal.ZERO
        val statusText = if (paidAmount >= currentTotal && currentTotal > BigDecimal.ZERO) "Paid" else "Unpaid"
        val remaining = if (currentTotal > paidAmount) currentTotal.subtract(paidAmount) else BigDecimal.ZERO
        val formattedTotal = parsedTotal?.let { formatter.format(it) }
        val isTotalInvalid = trimmedTotal.isNotEmpty() && parsedTotal == null
        val normalizedPickupTime =
            if (pickupTimeText.isBlank()) null else normalizePickupTime(pickupTimeText)
        val isPickupTimeInvalid = pickupTimeText.isNotBlank() && normalizedPickupTime == null
        val hasCustomerMismatch = customerName.isNotBlank() && customerPhone.isBlank()
        val canSave =
            notes.trim().isNotEmpty() &&
                parsedTotal != null &&
                parsedTotal > BigDecimal.ZERO &&
                !hasCustomerMismatch &&
                !isPickupTimeInvalid
        val notesState by rememberUpdatedState(notes)
        val setNotes by rememberUpdatedState<(String) -> Unit>({ notes = it })
        DisposableEffect(Unit) {
            voiceRouter.registerNotesTarget(getNotes = { notesState }, setNotes = setNotes)
            onDispose { voiceRouter.clearNotesTarget() }
        }

        fun submitOrder() {
            val trimmedNotes = notes.trim()
            val finalTotal = trimmedTotal.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)

            when {
                trimmedNotes.isEmpty() -> {
                    notesError = "Notes are required"
                    totalError = null
                    customerError = null
                }
                finalTotal == null || finalTotal <= BigDecimal.ZERO -> {
                    notesError = null
                    totalError = "Enter a valid total"
                    customerError = null
                }
                hasCustomerMismatch -> {
                    notesError = null
                    totalError = null
                    customerError = "Phone is required to attach a customer"
                }
                isPickupTimeInvalid -> {
                    notesError = null
                    totalError = null
                    customerError = null
                }
                else -> {
                    onSaveOrder(
                        trimmedNotes,
                        finalTotal,
                        customerName.trim(),
                        customerPhone.trim(),
                        normalizedPickupTime,
                        editingOrderId
                    )
                    notes = ""
                    totalText = ""
                    customerName = ""
                    customerPhone = ""
                    pickupTimeText = ""
                    editingOrderId = null
                    notesError = null
                    totalError = null
                    customerError = null
                    onDraftChange(null)
                    isEditorOpen = false
                }
            }
        }

        OrderEditorSheet(
            title = if (editingOrderId == null) "New Order" else "Edit Order",
            notes = notes,
            onNotesChange = {
                notes = it
                notesError = null
            },
            notesError = notesError,
            totalText = totalText,
            onTotalTextChange = {
                totalText = sanitizeAmountInput(it)
                totalError = null
            },
            isTotalInvalid = isTotalInvalid,
            totalSupportingText = when {
                isTotalInvalid -> "Enter a valid amount."
                formattedTotal != null -> "Total: KSh $formattedTotal"
                else -> null
            },
            totalError = totalError,
            statusText = "Status: $statusText - Paid ${formatKes(paidAmount)} - Due ${formatKes(remaining)}",
            pickupTimeText = pickupTimeText,
            onPickupTimeChange = { pickupTimeText = sanitizePickupTimeInput(it) },
            isPickupTimeInvalid = isPickupTimeInvalid,
            customerName = customerName,
            onCustomerNameChange = {
                customerName = it
                customerError = null
            },
            customerPhone = customerPhone,
            onCustomerPhoneChange = {
                customerPhone = it
                customerError = null
            },
            suggestions = suggestions,
            onSuggestionSelected = { customer ->
                customerName = customer.name
                customerPhone = customer.phone
                suggestions = emptyList()
            },
            customerError = customerError,
            canSave = canSave,
            onSave = { submitOrder() },
            onClear = {
                notes = ""
                totalText = ""
                customerName = ""
                customerPhone = ""
                pickupTimeText = ""
                editingOrderId = null
                notesError = null
                totalError = null
                customerError = null
                onDraftChange(null)
            },
            onCancel = { isEditorOpen = false },
            onNotesFocused = { voiceRouter.onFocusTarget(VoiceTarget.Notes) },
            onTotalFocused = { setter ->
                amountRegistry.update(setter)
                voiceRouter.onFocusTarget(VoiceTarget.Total)
            },
            voiceHasPermission = voiceCalcAccess.hasPermission,
            onRequestVoicePermission = voiceCalcAccess.onRequestPermission
        )
    }

    if (pendingDeleteOrder != null) {
        val order = pendingDeleteOrder!!
        val hasAllocations = deleteAllocations.isNotEmpty()
        val selectedAllocations =
            deleteAllocations.filter { allocation -> deleteSelection.contains(allocation.allocationId) }
        val targetAllocation =
            if (deleteAction == OrderPaymentAction.MOVE) {
                when (deleteMoveTarget) {
                    DeleteMoveTarget.ORDER ->
                        deleteSelectedOrderId?.let { ReceiptAllocation.Order(it) }
                    DeleteMoveTarget.OLDEST_ORDERS ->
                        order.customerId?.let { ReceiptAllocation.OldestOrders(it) }
                    DeleteMoveTarget.CUSTOMER_CREDIT ->
                        order.customerId?.let { ReceiptAllocation.CustomerCredit(it) }
                }
            } else {
                null
            }
        val canConfirm =
            if (!hasAllocations) {
                true
            } else {
                when (deleteAction) {
                    OrderPaymentAction.MOVE ->
                        selectedAllocations.isNotEmpty() && targetAllocation != null
                    OrderPaymentAction.VOID -> selectedAllocations.isNotEmpty()
                }
            }

        AlertDialog(
            onDismissRequest = { pendingDeleteOrder = null },
            title = { Text("Delete order?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val label =
                        formatOrderLabelWithId(
                            orderId = order.id,
                            date = order.orderDate,
                            customerName = order.customerId?.let { customerNames[it] },
                            notes = order.notes,
                            totalAmount = order.totalAmount
                        )
                    Text("$label will be cancelled and removed from totals.")

                    if (!hasAllocations) {
                        Text(
                            text = "No payments are linked to this order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Select payments to move or delete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    deleteSelection = deleteAllocations.map { it.allocationId }.toSet()
                                }
                            ) { Text("Select all") }
                            TextButton(onClick = { deleteSelection = emptySet() }) { Text("Clear") }
                        }

                        Column(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            deleteAllocations.forEach { allocation ->
                                val selected = deleteSelection.contains(allocation.allocationId)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { checked ->
                                            deleteSelection =
                                                if (checked) {
                                                    deleteSelection + allocation.allocationId
                                                } else {
                                                    deleteSelection - allocation.allocationId
                                                }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        val codeLabel =
                                            allocation.transactionCode?.let { "${allocation.method.name} $it" }
                                                ?: allocation.method.name
                                        Text(
                                            text = "${formatKes(allocation.amount)} - $codeLabel",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = formatDateTime(allocation.receivedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val sender =
                                            allocation.senderName?.takeIf { it.isNotBlank() }
                                                ?: allocation.senderPhone?.takeIf { it.isNotBlank() }
                                        if (sender != null) {
                                            Text(
                                                text = "From $sender",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = deleteAction == OrderPaymentAction.MOVE,
                                onClick = { deleteAction = OrderPaymentAction.MOVE },
                                label = { Text("Move payments") }
                            )
                            FilterChip(
                                selected = deleteAction == OrderPaymentAction.VOID,
                                onClick = { deleteAction = OrderPaymentAction.VOID },
                                label = { Text("Delete payments") }
                            )
                        }

                        if (deleteAction == OrderPaymentAction.MOVE) {
                            if (order.customerId == null) {
                                Text(
                                    text = "No customer on this order. Choose a target order.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = deleteMoveTarget == DeleteMoveTarget.ORDER,
                                    onClick = { deleteMoveTarget = DeleteMoveTarget.ORDER },
                                    label = { Text("Order") }
                                )
                                if (order.customerId != null) {
                                    FilterChip(
                                        selected = deleteMoveTarget == DeleteMoveTarget.OLDEST_ORDERS,
                                        onClick = { deleteMoveTarget = DeleteMoveTarget.OLDEST_ORDERS },
                                        label = { Text("Oldest orders") }
                                    )
                                    FilterChip(
                                        selected = deleteMoveTarget == DeleteMoveTarget.CUSTOMER_CREDIT,
                                        onClick = { deleteMoveTarget = DeleteMoveTarget.CUSTOMER_CREDIT },
                                        label = { Text("Customer credit") }
                                    )
                                }
                            }

                            if (deleteMoveTarget == DeleteMoveTarget.ORDER) {
                                if (deleteMoveOrderOptions.isEmpty()) {
                                    Text(
                                        text = "No other orders available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        deleteMoveOrderOptions.forEach { option ->
                                            FilterChip(
                                                selected = deleteSelectedOrderId == option.orderId,
                                                onClick = { deleteSelectedOrderId = option.orderId },
                                                label = { Text(option.label) }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = deleteMoveFullReceipts,
                                    onCheckedChange = { deleteMoveFullReceipts = it }
                                )
                                Text(
                                    text = "Move full receipts (affects other orders)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val allocationIds = selectedAllocations.map { it.allocationId }
                        scope.launch {
                            if (!hasAllocations) {
                                onDeleteOrder(order.id)
                            } else {
                                onDeleteOrderWithPayments(
                                    order.id,
                                    date,
                                    allocationIds,
                                    deleteAction,
                                    targetAllocation,
                                    deleteMoveFullReceipts
                                )
                            }
                            pendingDeleteOrder = null
                        }
                    },
                    enabled = canConfirm
                ) {
                    Text("Delete order")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteOrder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DaySummaryCard(
    date: LocalDate,
    dayTotal: BigDecimal,
    stats: DaySummaryStats
) {
    val monthLabel = titleCase(date.month.name)
    val dayOfWeekLabel = titleCase(date.dayOfWeek.name)
    val balanceLabel = if (stats.balance.signum() >= 0) "Balance" else "Over"
    val balanceValue = formatKes(stats.balance.abs())

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = monthLabel.take(3).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayOfWeekLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$monthLabel ${date.dayOfMonth}, ${date.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatKes(dayTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(label = "Paid", value = formatKes(stats.totalPaid))
                SummaryMetric(label = balanceLabel, value = balanceValue)
            }

            Spacer(Modifier.height(10.dp))

            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = "Orders",
                    count = stats.orderCount,
                    color = MaterialTheme.colorScheme.primary
                )
                if (stats.paidCount > 0) {
                    SummaryChip(
                        label = "Paid",
                        count = stats.paidCount,
                        color = paymentStateColor(PaymentState.PAID)
                    )
                }
                if (stats.partialCount > 0) {
                    SummaryChip(
                        label = "Partial",
                        count = stats.partialCount,
                        color = paymentStateColor(PaymentState.PARTIAL)
                    )
                }
                if (stats.unpaidCount > 0) {
                    SummaryChip(
                        label = "Unpaid",
                        count = stats.unpaidCount,
                        color = paymentStateColor(PaymentState.UNPAID)
                    )
                }
                if (stats.overpaidCount > 0) {
                    SummaryChip(
                        label = "Overpaid",
                        count = stats.overpaidCount,
                        color = paymentStateColor(PaymentState.OVERPAID)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyDayState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrderListItem(
    order: OrderEntity,
    customerLabel: String,
    paidAmount: BigDecimal,
    paymentState: PaymentState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPaymentHistory: () -> Unit,
    onReceivePayment: () -> Unit
) {
    val stateColor = paymentStateColor(paymentState)
    val statusLabel = paymentStateLabel(paymentState)
    val balance = order.totalAmount.subtract(paidAmount)
    val showReceive = paymentState == PaymentState.UNPAID || paymentState == PaymentState.PARTIAL
    val detailLabel =
        when (paymentState) {
            PaymentState.UNPAID -> "Balance due ${formatKes(order.totalAmount)}"
            PaymentState.PARTIAL -> "Balance due ${formatKes(balance)}"
            PaymentState.OVERPAID -> "Overpaid by ${formatKes(paidAmount.subtract(order.totalAmount))}"
            PaymentState.PAID -> "Paid in full"
        }
    val customerTextColor =
        if (customerLabel == "No customer") {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = stateColor,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .width(6.dp)
                    .heightIn(min = 48.dp)
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = order.notes,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatKes(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(min = 84.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = customerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = customerTextColor
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = statusLabel, color = stateColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = detailLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onPaymentHistory) {
                        Text("History")
                    }
                    if (showReceive) {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onReceivePayment) {
                            Text("Record payment")
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete order")
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun resolvePaymentState(total: BigDecimal, paidAmount: BigDecimal): PaymentState {
    if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
        return PaymentState.UNPAID
    }
    val balance = total.subtract(paidAmount)
    return when {
        balance.compareTo(BigDecimal.ZERO) > 0 -> PaymentState.PARTIAL
        balance.compareTo(BigDecimal.ZERO) == 0 -> PaymentState.PAID
        else -> PaymentState.OVERPAID
    }
}

@Composable
private fun paymentStateColor(state: PaymentState): Color {
    return when (state) {
        PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
        PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
        PaymentState.UNPAID -> MaterialTheme.colorScheme.error
        PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
    }
}

private fun paymentStateLabel(state: PaymentState): String {
    return when (state) {
        PaymentState.PAID -> "Paid"
        PaymentState.PARTIAL -> "Partial"
        PaymentState.UNPAID -> "Unpaid"
        PaymentState.OVERPAID -> "Overpaid"
    }
}

private fun computeDayStats(
    orders: List<OrderEntity>,
    orderPaidAmounts: Map<Long, BigDecimal>,
    dayTotal: BigDecimal
): DaySummaryStats {
    var paidCount = 0
    var partialCount = 0
    var unpaidCount = 0
    var overpaidCount = 0
    var totalPaid = BigDecimal.ZERO

    orders.forEach { order ->
        val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
        totalPaid = totalPaid.add(paidAmount)
        when (resolvePaymentState(order.totalAmount, paidAmount)) {
            PaymentState.PAID -> paidCount += 1
            PaymentState.PARTIAL -> partialCount += 1
            PaymentState.UNPAID -> unpaidCount += 1
            PaymentState.OVERPAID -> overpaidCount += 1
        }
    }

    return DaySummaryStats(
        orderCount = orders.size,
        paidCount = paidCount,
        partialCount = partialCount,
        unpaidCount = unpaidCount,
        overpaidCount = overpaidCount,
        totalPaid = totalPaid,
        balance = dayTotal.subtract(totalPaid)
    )
}

private fun titleCase(value: String): String {
    return value.lowercase().replaceFirstChar { it.uppercase() }
}

data class DaySummaryStats(
    val orderCount: Int,
    val paidCount: Int,
    val partialCount: Int,
    val unpaidCount: Int,
    val overpaidCount: Int,
    val totalPaid: BigDecimal,
    val balance: BigDecimal
)

data class OrderDraft(
    val notes: String,
    val totalText: String,
    val customerName: String,
    val customerPhone: String,
    val pickupTime: String,
    val editingOrderId: Long?
)

private enum class DayOrderFilter(val label: String) {
    All("All"),
    Unpaid("Unpaid"),
    Partial("Partial"),
    Paid("Paid"),
    Overpaid("Overpaid")
}

private enum class DeleteMoveTarget {
    ORDER,
    OLDEST_ORDERS,
    CUSTOMER_CREDIT
}

