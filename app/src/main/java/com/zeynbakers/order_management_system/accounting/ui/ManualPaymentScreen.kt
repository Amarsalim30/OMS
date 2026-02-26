package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.formatOrderLabel
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerOrderUi
import com.zeynbakers.order_management_system.customer.ui.OrderEffectiveStatus
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPaymentScreen(
    customerViewModel: CustomerAccountsViewModel,
    initialCustomerId: Long?,
    initialOrderId: Long?,
    initialAmount: BigDecimal?,
    onContextConsumed: () -> Unit,
    onPaymentRecorded: () -> Unit,
    showTopBar: Boolean = true,
    externalPadding: PaddingValues = PaddingValues(0.dp)
) {
    val amountRegistry = LocalAmountFieldRegistry.current
    val voiceRouter = LocalVoiceInputRouter.current
    val uiEvents = LocalUiEventDispatcher.current
    val paymentSavedMessage = stringResource(R.string.payment_saved)
    val enterValidAmount = stringResource(R.string.money_enter_valid_amount)
    val customer by customerViewModel.customer.collectAsState()
    val orders by customerViewModel.orders.collectAsState()
    val orderLabels by customerViewModel.orderLabels.collectAsState()
    val summaries by customerViewModel.summaries.collectAsState()

    var selectedCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedOrderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var customerQuery by rememberSaveable { mutableStateOf("") }
    var showOrderSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialCustomerId, initialOrderId, initialAmount) {
        if (initialCustomerId != null) {
            selectedCustomerId = initialCustomerId
            selectedOrderId = initialOrderId
            amountText = initialAmount?.toPlainString().orEmpty()
            customerQuery = ""
            onContextConsumed()
        }
    }

    LaunchedEffect(selectedCustomerId) {
        val customerId = selectedCustomerId ?: return@LaunchedEffect
        customerViewModel.loadCustomer(customerId)
    }

    LaunchedEffect(customerQuery) {
        val query = customerQuery.trim()
        if (query.isNotBlank()) {
            customerViewModel.searchCustomers(query)
        }
    }

    val eligibleOrders = remember(orders) {
        orders
            .filter { order ->
                order.effectiveStatus == OrderEffectiveStatus.OPEN &&
                    order.paidAmount < order.order.totalAmount
            }
            .sortedWith(
                compareBy<CustomerOrderUi> { it.order.orderDate }
                    .thenBy { it.order.createdAt }
                    .thenBy { it.order.id }
            )
    }

    val suggestions by remember(customerQuery, summaries) {
        derivedStateOf {
            if (customerQuery.isBlank()) emptyList() else summaries
        }
    }

    val parsedAmount = amountText.trim().takeIf { it.isNotEmpty() }?.let {
        runCatching { BigDecimal(it) }.getOrNull()
    }
    val canSave =
        selectedCustomerId != null && parsedAmount != null && parsedAmount > BigDecimal.ZERO

    val layoutDirection = LocalLayoutDirection.current
    val contentPadding =
        PaddingValues(
            start = 16.dp + externalPadding.calculateStartPadding(layoutDirection),
            end = 16.dp + externalPadding.calculateEndPadding(layoutDirection),
            top = 12.dp + externalPadding.calculateTopPadding(),
            bottom = 12.dp + externalPadding.calculateBottomPadding()
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.money_record_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val customerId = selectedCustomerId ?: return@Button
                            val amount = parsedAmount
                            if (amount == null || amount <= BigDecimal.ZERO) {
                                amountError = enterValidAmount
                                return@Button
                            }
                            customerViewModel.recordPayment(
                                customerId = customerId,
                                amount = amount,
                                method = selectedMethod,
                                note = noteText,
                                orderId = selectedOrderId
                            )
                            onPaymentRecorded()
                            scope.launch {
                                uiEvents.showSnackbar(paymentSavedMessage)
                            }
                            amountText = ""
                            noteText = ""
                            amountError = null
                        },
                        enabled = canSave
                    ) {
                        Text(stringResource(R.string.money_save_payment))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedCustomerId == null) {
                        Text(
                            text = stringResource(R.string.money_select_customer),
                            style = MaterialTheme.typography.titleSmall
                        )
                        OutlinedTextField(
                            value = customerQuery,
                            onValueChange = { customerQuery = it },
                            label = { Text(stringResource(R.string.money_search_name_or_phone)) },
                            placeholder = { Text(stringResource(R.string.money_customer_name_or_number)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (suggestions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                suggestions.take(8).forEach { summary ->
                                    TextButton(
                                        onClick = {
                                            selectedCustomerId = summary.customerId
                                            selectedOrderId = null
                                            customerQuery = ""
                                        }
                                    ) {
                                        val balanceLabel =
                                            when {
                                                summary.balance > BigDecimal.ZERO ->
                                                    stringResource(
                                                        R.string.money_due_value,
                                                        formatKes(summary.balance)
                                                    )
                                                summary.balance < BigDecimal.ZERO ->
                                                    stringResource(
                                                        R.string.money_credit_value,
                                                        formatKes(summary.balance.abs())
                                                    )
                                                else -> stringResource(R.string.money_balance_clear)
                                            }
                                        val primaryLabel =
                                            if (summary.phone.isBlank()) {
                                                summary.name
                                            } else {
                                                "${summary.name} - ${summary.phone}"
                                            }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = primaryLabel,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = balanceLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text =
                                        customer?.name?.ifBlank {
                                            stringResource(R.string.money_customer)
                                        } ?: stringResource(R.string.money_customer),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                customer?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        selectedCustomerId = null
                                        selectedOrderId = null
                                        amountText = ""
                                        customerQuery = ""
                                    }
                                ) {
                                    Text(stringResource(R.string.money_change_customer))
                                }
                            }
                        }
                    }

                    Text(text = stringResource(R.string.money_amount), style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                            if (filtered.count { ch -> ch == '.' } <= 1) {
                                amountText = filtered
                            }
                            amountError = null
                        },
                        label = { Text(stringResource(R.string.money_amount_kes)) },
                        placeholder = { Text(stringResource(R.string.money_kes_zero)) },
                        enabled = selectedCustomerId != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    amountRegistry.update { amountText = it }
                                    voiceRouter.onFocusTarget(VoiceTarget.Total)
                                }
                            }
                    )
                    amountError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    Text(text = stringResource(R.string.money_method), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MethodChip(
                            label = stringResource(R.string.money_method_cash),
                            selected = selectedMethod == PaymentMethod.CASH,
                            onClick = { selectedMethod = PaymentMethod.CASH },
                            enabled = selectedCustomerId != null
                        )
                        MethodChip(
                            label = stringResource(R.string.money_method_mpesa),
                            selected = selectedMethod == PaymentMethod.MPESA,
                            onClick = { selectedMethod = PaymentMethod.MPESA },
                            enabled = selectedCustomerId != null
                        )
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.money_note_optional)) },
                        enabled = selectedCustomerId != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = stringResource(R.string.money_allocation), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedOrderId == null,
                            onClick = { selectedOrderId = null },
                            label = { Text(stringResource(R.string.money_oldest_orders)) },
                            enabled = selectedCustomerId != null
                        )
                        FilterChip(
                            selected = selectedOrderId != null,
                            onClick = { showOrderSheet = true },
                            label = { Text(stringResource(R.string.money_pick_order)) },
                            enabled = selectedCustomerId != null
                        )
                    }
                    if (selectedOrderId != null) {
                        val label =
                            orderLabels[selectedOrderId]
                                ?: eligibleOrders.firstOrNull { it.order.id == selectedOrderId }?.let { order ->
                                    formatOrderLabel(
                                        date = order.order.orderDate,
                                        customerName = customer?.name,
                                        notes = order.order.notes,
                                        totalAmount = order.order.totalAmount
                                    )
                                }
                        if (!label.isNullOrBlank()) {
                            Text(
                                text = stringResource(R.string.money_selected_label, label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOrderSheet = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.money_pick_order), style = MaterialTheme.typography.titleSmall)
                if (eligibleOrders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.money_no_open_orders),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(eligibleOrders, key = { it.order.id }) { order ->
                            val label =
                                formatOrderLabel(
                                    date = order.order.orderDate,
                                    customerName = customer?.name,
                                    notes = order.order.notes,
                                    totalAmount = order.order.totalAmount
                                )
                            val outstanding = order.order.totalAmount - order.paidAmount
                            TextButton(
                                onClick = {
                                    selectedOrderId = order.order.id
                                    amountText = outstanding.max(BigDecimal.ZERO).toPlainString()
                                    showOrderSheet = false
                                }
                            ) {
                                Text(
                                    stringResource(
                                        R.string.money_due_amount,
                                        label,
                                        formatKes(outstanding)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled,
        modifier = Modifier.sizeIn(minHeight = 48.dp)
    )
}
