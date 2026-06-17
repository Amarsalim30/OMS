package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.LocalVoiceInputRouter
import com.zeynbakers.order_management_system.core.ui.VoiceTarget
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard
import com.zeynbakers.order_management_system.core.ui.components.AppSection
import com.zeynbakers.order_management_system.core.ui.components.AppSpacing
import com.zeynbakers.order_management_system.core.ui.components.SuccessOverlay
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.formatOrderLabel
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerOrderUi
import com.zeynbakers.order_management_system.customer.ui.OrderEffectiveStatus
import java.math.BigDecimal

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
    val customerBalance by customerViewModel.balance.collectAsState()

    var selectedCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedOrderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var customerQuery by rememberSaveable { mutableStateOf("") }
    var showOrderSheet by remember { mutableStateOf(false) }
    var showSuccessOverlay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialCustomerId, initialOrderId, initialAmount) {
        val hasContext = initialCustomerId != null || initialOrderId != null || initialAmount != null
        if (!hasContext) return@LaunchedEffect
        selectedCustomerId = initialCustomerId
        selectedOrderId = initialOrderId
        amountText = initialAmount?.toPlainString().orEmpty()
        customerQuery = ""
        onContextConsumed()
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
    val hasAllocationTarget = selectedCustomerId != null || selectedOrderId != null
    val canSave = hasAllocationTarget && parsedAmount != null && parsedAmount > BigDecimal.ZERO

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
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        val amount = parsedAmount
                        if (amount == null || amount <= BigDecimal.ZERO) {
                            amountError = enterValidAmount
                            return@Button
                        }
                        if (selectedCustomerId == null && selectedOrderId == null) {
                            return@Button
                        }
                        customerViewModel.recordPayment(
                            customerId = selectedCustomerId,
                            amount = amount,
                            method = selectedMethod,
                            note = noteText,
                            orderId = selectedOrderId
                        )
                        showSuccessOverlay = true
                        amountText = ""
                        noteText = ""
                        amountError = null
                    },
                    enabled = canSave,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small)
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.money_save_payment),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                item {
                    if (selectedCustomerId == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                            selectedOrderId?.let { contextOrderId ->
                                AppCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)) {
                                        Text(
                                            text = stringResource(
                                                R.string.payment_history_header_order_id,
                                                contextOrderId
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.money_anonymous_payment_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(AppSpacing.small))
                            }
                            AppSection(
                                title = stringResource(R.string.money_select_customer),
                                subtitle = stringResource(R.string.money_customer_name_or_number)
                            ) {
                                OutlinedTextField(
                                    value = customerQuery,
                                    onValueChange = { customerQuery = it },
                                    label = { Text(stringResource(R.string.action_search)) },
                                    placeholder = { Text(stringResource(R.string.money_search_name_or_phone)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                )
                                if (suggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(AppSpacing.small))
                                    AppCard {
                                        suggestions.take(8).forEachIndexed { index, summary ->
                                            if (index > 0) HorizontalDivider(
                                                modifier = Modifier.padding(vertical = AppSpacing.xSmall),
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            )
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
                                            val phoneLabel =
                                                summary.phone.takeIf { it.isNotBlank() }
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = summary.name,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                supportingContent = {
                                                    val subLine =
                                                        listOfNotNull(phoneLabel, balanceLabel)
                                                            .joinToString(" · ")
                                                    if (subLine.isNotBlank()) Text(subLine)
                                                },
                                                leadingContent = {
                                                    Surface(
                                                        shape = MaterialTheme.shapes.small,
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(
                                                            alpha = 0.4f
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.AccountCircle,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(6.dp)
                                                        )
                                                    }
                                                },
                                                trailingContent = {
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.5f
                                                        )
                                                    )
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCustomerId = summary.customerId
                                                        selectedOrderId = null
                                                        customerQuery = ""
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val balance = customerBalance
                        val highlightText = when {
                            balance > BigDecimal.ZERO -> stringResource(
                                R.string.money_due_value,
                                formatKes(balance)
                            )

                            balance < BigDecimal.ZERO -> stringResource(
                                R.string.money_credit_value,
                                formatKes(balance.abs())
                            )

                            else -> null
                        }

                        AppScreenHeaderCard(
                            title = customer?.name?.ifBlank {
                                stringResource(R.string.money_customer)
                            } ?: stringResource(R.string.money_customer),
                            subtitle = customer?.phone?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.customer_unknown),
                            leadingIcon = Icons.Default.AccountCircle,
                            highlight = highlightText
                        )
                        TextButton(
                            onClick = {
                                selectedCustomerId = null
                                selectedOrderId = null
                                amountText = ""
                                customerQuery = ""
                            },
                            modifier = Modifier.padding(top = AppSpacing.xSmall)
                        ) {
                            Text(stringResource(R.string.action_change))
                        }
                    }
                }

                item {
                    AppCard {
                        AppSection(
                            title = stringResource(R.string.customer_accounts_payment_title),
                        ) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    if (filtered.count { ch -> ch == '.' } <= 1) {
                                        amountText = filtered
                                    }
                                    amountError = null
                                },
                                label = { Text(stringResource(R.string.money_amount)) },
                                placeholder = { Text(stringResource(R.string.money_kes_zero)) },
                                prefix = {
                                    Text(
                                        text = stringResource(R.string.order_editor_currency_prefix),
                                        modifier = Modifier.padding(end = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                enabled = hasAllocationTarget,
                                isError = amountError != null,
                                supportingText = amountError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            amountRegistry.update { amountText = it }
                                            voiceRouter.onFocusTarget(VoiceTarget.Total)
                                        }
                                    }
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.medium))

                            Text(
                                text = stringResource(R.string.money_method),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xSmall))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                                MethodChip(
                                    label = stringResource(R.string.money_method_cash),
                                    icon = Icons.Default.AttachMoney,
                                    selected = selectedMethod == PaymentMethod.CASH,
                                    onClick = { selectedMethod = PaymentMethod.CASH },
                                    enabled = hasAllocationTarget
                                )
                                MethodChip(
                                    label = stringResource(R.string.money_method_mpesa),
                                    icon = Icons.Default.CreditCard,
                                    selected = selectedMethod == PaymentMethod.MPESA,
                                    onClick = { selectedMethod = PaymentMethod.MPESA },
                                    enabled = hasAllocationTarget
                                )
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.medium))

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                label = { Text(stringResource(R.string.money_note_optional)) },
                                placeholder = { Text(stringResource(R.string.order_editor_notes_placeholder)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null
                                    )
                                },
                                enabled = hasAllocationTarget,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (selectedCustomerId != null) {
                    item {
                        AppCard {
                            AppSection(
                                title = stringResource(R.string.money_allocation),
                                subtitle = stringResource(R.string.money_pick_order)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                                    FilterChip(
                                        selected = selectedOrderId == null,
                                        onClick = { selectedOrderId = null },
                                        label = { Text(stringResource(R.string.money_oldest_orders)) },
                                        leadingIcon = if (selectedOrderId == null) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.height(48.dp)
                                    )
                                    FilterChip(
                                        selected = selectedOrderId != null,
                                        onClick = { showOrderSheet = true },
                                        label = { Text(stringResource(R.string.money_pick_order)) },
                                        leadingIcon = if (selectedOrderId != null) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.height(48.dp)
                                    )
                                }

                                selectedOrderId?.let { contextOrderId ->
                                    val order =
                                        eligibleOrders.firstOrNull { it.order.id == contextOrderId }
                                    val label =
                                        orderLabels[contextOrderId]
                                            ?: order?.let { o ->
                                                formatOrderLabel(
                                                    date = o.order.orderDate,
                                                    customerName = customer?.name,
                                                    notes = "",
                                                    totalAmount = o.order.totalAmount
                                                )
                                            }
                                            ?: stringResource(
                                                R.string.payment_history_header_order_id,
                                                contextOrderId
                                            )

                                    if (label.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(AppSpacing.medium))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                                alpha = 0.3f
                                            ),
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                supportingContent = {
                                                    order?.let { o ->
                                                        Text(
                                                            text = stringResource(
                                                                R.string.day_balance_due_amount,
                                                                formatKes(o.order.totalAmount - o.paidAmount)
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                },
                                                leadingContent = {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                },
                                                trailingContent = {
                                                    TextButton(onClick = {
                                                        showOrderSheet = true
                                                    }) {
                                                        Text(stringResource(R.string.action_change))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SuccessOverlay(
                visible = showSuccessOverlay,
                text = paymentSavedMessage,
                onDismiss = {
                    showSuccessOverlay = false
                    onPaymentRecorded()
                }
            )
        }
    }

    if (showOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOrderSheet = false },
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape
                ) {
                    Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.money_pick_order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.large,
                        vertical = AppSpacing.medium
                    )
                )
                if (eligibleOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.money_no_open_orders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(eligibleOrders, key = { it.order.id }) { order ->
                            val label =
                                formatOrderLabel(
                                    date = order.order.orderDate,
                                    customerName = customer?.name,
                                    notes = "",
                                    totalAmount = order.order.totalAmount
                                )
                            val outstanding = order.order.totalAmount - order.paidAmount
                            ListItem(
                                headlineContent = { Text(label) },
                                supportingContent = {
                                    Text(
                                        stringResource(
                                            R.string.day_balance_due_amount,
                                            formatKes(outstanding)
                                        )
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null
                                    )
                                },
                                trailingContent = {
                                    if (selectedOrderId == order.order.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    selectedOrderId = order.order.id
                                    amountText = outstanding.max(BigDecimal.ZERO).toPlainString()
                                    showOrderSheet = false
                                }
                            )
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
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        enabled = enabled,
        modifier = Modifier.height(48.dp)
    )
}
