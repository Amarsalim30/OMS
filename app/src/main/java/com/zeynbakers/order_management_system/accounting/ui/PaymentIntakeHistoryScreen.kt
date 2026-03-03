package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget
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
                title = {
                    Text(
                        text = screenTitle,
                        modifier = Modifier.tutorialCoachTarget(TutorialCoachTargets.PaymentHistoryTitle)
                    )
                },
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
                if (items.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.money_history_swipe_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
    val canMove = item.customerId != null && !item.isBadDebt
    val canVoid = item.status != PaymentReceiptStatus.VOIDED
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { target ->
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (canMove) onMove()
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (canVoid) onVoid()
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )
    val accentColor =
        when (item.status) {
            PaymentReceiptStatus.UNAPPLIED -> MaterialTheme.colorScheme.outline
            PaymentReceiptStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
            PaymentReceiptStatus.APPLIED -> MaterialTheme.colorScheme.primary
            PaymentReceiptStatus.VOIDED -> MaterialTheme.colorScheme.error
        }
    val containerColor =
        if (item.status == PaymentReceiptStatus.VOIDED) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    val amountDecoration =
        if (item.status == PaymentReceiptStatus.VOIDED) {
            TextDecoration.LineThrough
        } else {
            TextDecoration.None
        }
    val contentAlpha =
        if (item.status == PaymentReceiptStatus.VOIDED) 0.74f else 1f
    val methodLabel =
        if (item.isBadDebt) {
            stringResource(R.string.money_entry_bad_debt_writeoff)
        } else if (item.method == PaymentMethod.MPESA) {
            stringResource(R.string.money_method_mpesa)
        } else {
            stringResource(R.string.money_method_cash)
        }
    val supportingLabel =
        if (item.isBadDebt) {
            item.note?.trim()?.takeIf { it.isNotBlank() }
        } else {
            null
        }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = canMove,
        enableDismissFromEndToStart = canVoid,
        backgroundContent = { PaymentHistorySwipeBackground(dismissState) }
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = methodLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Text(
                        text = formatDateTime(item.receivedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    supportingLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatKes(item.displayAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = amountDecoration,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentHistorySwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_move),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        SwipeToDismissBoxValue.EndToStart -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_void),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        SwipeToDismissBoxValue.Settled -> Unit
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

