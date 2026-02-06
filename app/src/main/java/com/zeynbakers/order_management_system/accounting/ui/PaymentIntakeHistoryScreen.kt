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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
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
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val items by viewModel.history.collectAsState()
    val header by viewModel.header.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val screenTitle =
        when (filter) {
            PaymentHistoryFilter.All -> "Payments"
            is PaymentHistoryFilter.Customer -> "Customer payments"
            is PaymentHistoryFilter.Order -> "Order payments"
        }
    var pendingVoid by remember { mutableStateOf<PaymentHistoryItemUi?>(null) }
    var voidReason by remember { mutableStateOf("") }
    var pendingMove by remember { mutableStateOf<PaymentHistoryItemUi?>(null) }
    var moveMode by remember { mutableStateOf(MoveMode.OLDEST_ORDERS) }
    var selectedOrderId by remember { mutableStateOf<Long?>(null) }
    var moveOrders by remember { mutableStateOf<List<MoveOrderOption>>(emptyList()) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            title = { Text("Void payment?") },
            text = {
                Column {
                    Text("This keeps the payment but removes it from totals.")
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason (optional)") },
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
                        snackbarHostState.showSnackbar(result.message)
                        viewModel.load(filter)
                        onRemoved()
                    }
                }) {
                    Text("Void")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingVoid = null
                    voidReason = ""
                }) {
                    Text("Cancel")
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
            title = { Text("Move payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasCustomer) {
                        Text("Missing customer details for this payment.")
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = moveMode == MoveMode.ORDER,
                                onClick = { moveMode = MoveMode.ORDER },
                                label = { Text("Order") }
                            )
                            FilterChip(
                                selected = moveMode == MoveMode.OLDEST_ORDERS,
                                onClick = { moveMode = MoveMode.OLDEST_ORDERS },
                                label = { Text("Oldest orders") }
                            )
                            FilterChip(
                                selected = moveMode == MoveMode.CUSTOMER_CREDIT,
                                onClick = { moveMode = MoveMode.CUSTOMER_CREDIT },
                                label = { Text("Customer credit") }
                            )
                        }

                        if (moveMode == MoveMode.ORDER) {
                            if (moveOrders.isEmpty()) {
                                Text(
                                    text = "No orders found for this customer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    moveOrders.forEach { option ->
                                        FilterChip(
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
                                    PaymentHistoryActionResult(false, "Select a target")
                                } else {
                                    viewModel.onMoveReceipt(target, allocation)
                                }
                            snackbarHostState.showSnackbar(result.message)
                            viewModel.load(filter)
                            onRemoved()
                        }
                    },
                    enabled = allocation != null
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryHeaderCard(header: PaymentHistoryHeader) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
}

@Composable
private fun PaymentHistoryRow(
    item: PaymentHistoryItemUi,
    onMove: () -> Unit,
    onVoid: () -> Unit
) {
    val methodLabel = if (item.method == PaymentMethod.MPESA) "M-PESA" else "Cash"
    val codeLabel = item.transactionCode?.let { "$methodLabel $it" } ?: methodLabel
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
                allocation.orderId?.let { "$label (ID $it)" } ?: label
            }
            else -> "Orders (${orderAllocations.size})"
        }
    val customerLabel =
        item.customerName
            ?: item.customerId?.let { "Customer #$it" }
            ?: "Customer"
    val targetLabel = orderLabel ?: customerLabel
    val statusLabel = item.status.label()
    val statusColor = item.status.color()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatDateTime(item.receivedAt)} - ${formatKes(item.displayAmount)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$codeLabel - $targetLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.secondaryAmount?.let { secondary ->
                    Text(
                        text = "Receipt total ${formatKes(secondary)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusPill(label = statusLabel, color = statusColor)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onMove,
                        enabled = item.customerId != null
                    ) {
                        Text("Move")
                    }
                    TextButton(
                        onClick = onVoid,
                        enabled = item.status != PaymentReceiptStatus.VOIDED
                    ) {
                        Text("Void")
                    }
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
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = "No payments yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        )
    }
}

@Composable
private fun ErrorHistoryState(message: String) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
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

private enum class MoveMode {
    ORDER,
    OLDEST_ORDERS,
    CUSTOMER_CREDIT
}

private fun PaymentReceiptStatus.label(): String {
    return when (this) {
        PaymentReceiptStatus.UNAPPLIED -> "Not used"
        PaymentReceiptStatus.PARTIAL -> "Part used"
        PaymentReceiptStatus.APPLIED -> "Used"
        PaymentReceiptStatus.VOIDED -> "Voided"
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

