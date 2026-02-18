package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard
import com.zeynbakers.order_management_system.core.util.formatOrderLabel
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentIntakeHistoryScreen(
    viewModel: PaymentIntakeHistoryViewModel,
    filter: PaymentHistoryFilter,
    focusReceiptId: Long?,
    onBack: () -> Unit,
    onRemoved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uiEvents = LocalUiEventDispatcher.current
    val listState = rememberLazyListState()
    val items by viewModel.history.collectAsState()
    val header by viewModel.header.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val screenTitle = stringResource(MoneyCopy.paymentHistoryTitle(filter))
    var pendingVoid by remember { mutableStateOf<PaymentHistoryItemUi?>(null) }
    var voidReason by remember { mutableStateOf("") }
    var pendingMove by remember { mutableStateOf<PaymentHistoryItemUi?>(null) }
    var moveMode by remember { mutableStateOf(MoveMode.OLDEST_ORDERS) }
    var selectedOrderId by remember { mutableStateOf<Long?>(null) }
    var moveOrders by remember { mutableStateOf<List<MoveOrderOption>>(emptyList()) }
    val selectTargetMessage = stringResource(R.string.money_select_target)

    LaunchedEffect(filter) {
        viewModel.load(filter)
    }

    LaunchedEffect(focusReceiptId, items) {
        val target = focusReceiptId ?: return@LaunchedEffect
        val index = items.indexOfFirst { it.receiptId == target }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    LaunchedEffect(pendingMove?.receiptId) {
        val item = pendingMove
        if (item?.customerId != null) {
            moveOrders = viewModel.loadMoveOrderOptions(item.customerId)
        } else {
            moveOrders = emptyList()
        }
        moveMode = MoveMode.OLDEST_ORDERS
        selectedOrderId = null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            AppScreenHeaderCard(
                title = stringResource(R.string.money_payment_history_all),
                subtitle = stringResource(R.string.money_history_owner_subtitle),
                highlight = stringResource(R.string.money_history_owner_highlight_count, items.size)
            )

            Spacer(modifier = Modifier.height(8.dp))

            header?.let {
                HistoryHeaderCard(header = it)
                Spacer(modifier = Modifier.height(6.dp))
            }

            error?.let { message ->
                ErrorHistoryState(message = message)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (!isLoading && items.isEmpty() && error == null) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.receiptId }) { item ->
                        PaymentHistoryRow(
                            item = item,
                            onMove = { pendingMove = item },
                            onVoid = { pendingVoid = item }
                        )
                    }
                }
            }
        }
    }

    pendingVoid?.let { item ->
        AlertDialog(
            onDismissRequest = {
                pendingVoid = null
                voidReason = ""
            },
            title = { Text(stringResource(R.string.money_void_payment_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.money_void_payment_hint))
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(stringResource(R.string.money_reason_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = item.receiptId
                    val reason = voidReason.trim().ifBlank { null }
                    pendingVoid = null
                    voidReason = ""
                    scope.launch {
                        val result = viewModel.onVoidReceipt(target, reason)
                        uiEvents.showSnackbar(result.message)
                        viewModel.load(filter)
                        onRemoved()
                    }
                }) {
                    Text(stringResource(R.string.action_void))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingVoid = null
                    voidReason = ""
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    pendingMove?.let { item ->
        val hasCustomer = item.customerId != null
        val allocation =
            when (moveMode) {
                MoveMode.ORDER ->
                    selectedOrderId?.let { ReceiptAllocation.Order(it) }
                MoveMode.OLDEST_ORDERS ->
                    item.customerId?.let { ReceiptAllocation.OldestOrders(it) }
                MoveMode.CUSTOMER_CREDIT ->
                    item.customerId?.let { ReceiptAllocation.CustomerCredit(it) }
            }
        AlertDialog(
            onDismissRequest = { pendingMove = null },
            title = { Text(stringResource(R.string.money_move_payment_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasCustomer) {
                        Text(stringResource(R.string.money_move_payment_missing_customer))
                    } else {
                        AppFilterRow(
                            options = moveModeOptions(),
                            selectedKey = moveMode.name,
                            onSelect = { selected -> moveMode = MoveMode.valueOf(selected) }
                        )

                        if (moveMode == MoveMode.ORDER) {
                            if (moveOrders.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.money_no_orders_found_for_customer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    moveOrders.forEach { option ->
                                        androidx.compose.material3.FilterChip(
                                            selected = selectedOrderId == option.orderId,
                                            onClick = { selectedOrderId = option.orderId },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = item.receiptId
                        pendingMove = null
                        scope.launch {
                            val result =
                                if (allocation == null) {
                                    PaymentHistoryActionResult(false, selectTargetMessage)
                                } else {
                                    viewModel.onMoveReceipt(target, allocation)
                                }
                            uiEvents.showSnackbar(result.message)
                            viewModel.load(filter)
                            onRemoved()
                        }
                    },
                    enabled = allocation != null
                ) {
                    Text(stringResource(R.string.action_move))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMove = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryHeaderCard(header: PaymentHistoryHeader) {
    AppCard {
            Text(text = header.title, style = MaterialTheme.typography.titleMedium)
            header.subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun PaymentHistoryRow(
    item: PaymentHistoryItemUi,
    onMove: () -> Unit,
    onVoid: () -> Unit
) {
    val methodLabel =
        if (item.method == PaymentMethod.MPESA) {
            stringResource(R.string.money_method_mpesa)
        } else {
            stringResource(R.string.money_method_cash)
        }
    val codeLabel =
        item.transactionCode?.let { stringResource(R.string.money_method_code, methodLabel, it) }
            ?: methodLabel
    val orderAllocations =
        item.allocations.filter { it.orderId != null && it.status == PaymentAllocationStatus.APPLIED }
    val orderLabel =
        when {
            orderAllocations.isEmpty() -> null
            orderAllocations.size == 1 -> {
                val allocation = orderAllocations.first()
                val label =
                    formatOrderLabel(
                    date = allocation.orderDate,
                    customerName = item.customerName,
                    notes = allocation.orderNotes,
                    totalAmount = null
                )
                allocation.orderId?.let {
                    stringResource(R.string.money_order_with_id, label, it)
                } ?: label
            }
            else -> stringResource(R.string.money_orders_count, orderAllocations.size)
        }
    val customerLabel =
        item.customerName
            ?: item.customerId?.let { stringResource(R.string.money_customer_id_fallback, it) }
            ?: stringResource(R.string.money_customer)
    val targetLabel = orderLabel ?: customerLabel
    val statusLabel = item.status.label()
    val statusColor = item.status.color()

    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        stringResource(
                            R.string.money_row_datetime_amount,
                            formatDateTime(item.receivedAt),
                            formatKes(item.displayAmount)
                        ),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.money_row_code_target, codeLabel, targetLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.secondaryAmount?.let { secondary ->
                    Text(
                        text = stringResource(R.string.money_receipt_total, formatKes(secondary)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusPill(label = statusLabel, color = statusColor)
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onMove,
                    enabled = item.customerId != null
                ) {
                    Text(stringResource(R.string.action_move))
                }
                TextButton(
                    onClick = onVoid,
                    enabled = item.status != PaymentReceiptStatus.VOIDED
                ) {
                    Text(stringResource(R.string.action_void))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    AppEmptyState(
        title = stringResource(R.string.money_payment_history_all),
        body = stringResource(R.string.money_no_payments_yet)
    )
}

@Composable
private fun ErrorHistoryState(message: String) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        )
    }
}

@Composable
private fun moveModeOptions(): List<AppFilterOption> {
    return listOf(
        AppFilterOption(MoveMode.ORDER.name, stringResource(R.string.money_move_mode_order)),
        AppFilterOption(MoveMode.OLDEST_ORDERS.name, stringResource(R.string.money_oldest_orders)),
        AppFilterOption(
            MoveMode.CUSTOMER_CREDIT.name,
            stringResource(R.string.money_move_mode_customer_credit)
        )
    )
}

private enum class MoveMode {
    ORDER,
    OLDEST_ORDERS,
    CUSTOMER_CREDIT
}

@Composable
private fun PaymentReceiptStatus.label(): String {
    return when (this) {
        PaymentReceiptStatus.UNAPPLIED -> stringResource(R.string.money_receipt_status_not_used)
        PaymentReceiptStatus.PARTIAL -> stringResource(R.string.money_receipt_status_part_used)
        PaymentReceiptStatus.APPLIED -> stringResource(R.string.money_receipt_status_used)
        PaymentReceiptStatus.VOIDED -> stringResource(R.string.money_receipt_status_voided)
    }
}

@Composable
private fun PaymentReceiptStatus.color(): androidx.compose.ui.graphics.Color {
    return when (this) {
        PaymentReceiptStatus.UNAPPLIED -> MaterialTheme.colorScheme.secondary
        PaymentReceiptStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        PaymentReceiptStatus.APPLIED -> MaterialTheme.colorScheme.primary
        PaymentReceiptStatus.VOIDED -> MaterialTheme.colorScheme.error
    }
}

