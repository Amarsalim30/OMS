package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.LocalVoiceOverlaySuppressed
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

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
        draft: OrderDraft?,
        onDraftChange: (OrderDraft?) -> Unit
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
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
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
    val amountRegistry = LocalAmountFieldRegistry.current
    val overlaySuppressed = LocalVoiceOverlaySuppressed.current
    val voiceRouter = LocalVoiceInputRouter.current
    var orderFilter by rememberSaveable { mutableStateOf(DayOrderFilter.All) }
    var searchQuery by rememberSaveable(dateKey) { mutableStateOf("") }
    var isSearchExpanded by rememberSaveable(dateKey) { mutableStateOf(false) }
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
    LaunchedEffect(isEditorOpen) { overlaySuppressed.value = isEditorOpen }
    DisposableEffect(Unit) { onDispose { overlaySuppressed.value = false } }
    LaunchedEffect(customerName, customerPhone) {
        val query = customerName.trim()
        val selectedPhone = customerPhone.trim()
        if (query.isBlank() || selectedPhone.isNotBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        suggestions = searchCustomers(query)
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchExpanded = true
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
                        val matchesStatus =
                                when (orderFilter) {
                                    DayOrderFilter.All -> true
                                    DayOrderFilter.Unpaid ->
                                            resolvePaymentState(order.totalAmount, paidAmount) ==
                                                    PaymentState.UNPAID
                                    DayOrderFilter.Partial ->
                                            resolvePaymentState(order.totalAmount, paidAmount) ==
                                                    PaymentState.PARTIAL
                                    DayOrderFilter.Paid ->
                                            resolvePaymentState(order.totalAmount, paidAmount) ==
                                                    PaymentState.PAID
                                    DayOrderFilter.Overpaid ->
                                            resolvePaymentState(order.totalAmount, paidAmount) ==
                                                    PaymentState.OVERPAID
                                }
                        if (!matchesStatus) return@filter false
                        if (normalizedQuery.isBlank()) return@filter true
                        val customerLabel =
                                order.customerId?.let { customerNames[it] }.orEmpty().lowercase()
                        order.notes.lowercase().contains(normalizedQuery) ||
                                customerLabel.contains(normalizedQuery)
                    }
                    sortOrdersForPlanner(filtered)
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
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { DaySummaryCard(dayTotal = dayTotal, stats = dayStats) }
            if (orders.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        AppFilterRow(
                                options = dayOrderFilterOptions(orders.size, dayStats),
                                selectedKey = orderFilter.name,
                                onSelect = { selected ->
                                    orderFilter = DayOrderFilter.valueOf(selected)
                                },
                                showMoreAsIcon = true
                        )
                        Spacer(Modifier.height(6.dp))
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
                                        } else {
                                            isSearchExpanded = true
                                        }
                                    }
                            ) {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
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
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        label = {
                                            Text(stringResource(R.string.day_search_orders))
                                        },
                                        placeholder = {
                                            Text(
                                                    stringResource(
                                                            R.string.day_search_notes_or_customer
                                                    )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                    imageVector = Icons.Filled.Search,
                                                    contentDescription = null
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
                            Spacer(Modifier.height(6.dp))
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
                            onDelete = { pendingDeleteOrder = order },
                            onPaymentHistory = { onOrderPaymentHistory(order.id) },
                            onReceivePayment = { onReceivePayment(order) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    DayOrderEditorDialog(
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
            onSetEditorOpen = { isEditorOpen = it }
    )
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
}
