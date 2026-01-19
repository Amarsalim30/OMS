package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.ui.AmountFieldRegistry
import com.zeynbakers.order_management_system.core.ui.LocalAmountFieldRegistry
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity?,
    ledger: List<AccountEntryEntity>,
    balance: BigDecimal,
    orders: List<CustomerOrderUi>,
    onBack: () -> Unit,
    onRecordPayment: (BigDecimal, PaymentMethod, String, Long?) -> Unit,
    onUpdateOrderStatusOverride: (Long, OrderStatusOverride?) -> Unit,
    onWriteOffOrder: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedOrderId by remember { mutableStateOf<Long?>(null) }
    val amountRegistry = LocalAmountFieldRegistry.current
    var orderFilter by remember { mutableStateOf(OrderFilter.All) }
    var ledgerFilter by remember { mutableStateOf(LedgerFilter.All) }
    var ledgerQuery by remember { mutableStateOf("") }
    var visibleLedgerMonths by remember { mutableStateOf(3) }
    val expandedLedgerMonths = remember { mutableStateMapOf<String, Boolean>() }

    val eligibleOrders = remember(orders) {
        orders.filter { order ->
            order.effectiveStatus == OrderEffectiveStatus.OPEN &&
                order.paidAmount < order.order.totalAmount
        }
    }

    val filteredOrders by remember(orders, orderFilter) {
        derivedStateOf { filterOrders(orders, orderFilter) }
    }

    val filteredLedger by remember(ledger, ledgerFilter, ledgerQuery) {
        derivedStateOf { filterLedger(ledger, ledgerFilter, ledgerQuery) }
    }

    val ledgerSections = remember(filteredLedger) { buildLedgerSections(filteredLedger) }
    val currentMonthKey = remember { currentMonthKey() }

    LaunchedEffect(ledgerSections) {
        ledgerSections.forEach { section ->
            expandedLedgerMonths.putIfAbsent(section.key, section.key == currentMonthKey)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = customer?.name?.ifBlank { "Customer" } ?: "Customer",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BalanceCard(customer = customer, balance = balance)
            }

            item {
                PaymentCard(
                    amountText = amountText,
                    onAmountChange = {
                        amountText = it
                        amountError = null
                    },
                    amountError = amountError,
                    amountRegistry = amountRegistry,
                    selectedMethod = selectedMethod,
                    onMethodChange = { selectedMethod = it },
                    noteText = noteText,
                    onNoteChange = { noteText = it },
                    eligibleOrders = eligibleOrders,
                    selectedOrderId = selectedOrderId,
                    onSelectOrder = { selectedOrderId = it },
                    onSubmit = {
                        val parsedAmount = amountText.trim().takeIf { it.isNotEmpty() }?.let {
                            runCatching { BigDecimal(it) }.getOrNull()
                        }
                        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
                            amountError = "Enter a valid amount"
                        } else {
                            onRecordPayment(parsedAmount, selectedMethod, noteText, selectedOrderId)
                            amountText = ""
                            noteText = ""
                            amountError = null
                        }
                    }
                )
            }

            item {
                OrdersHeader(
                    orderFilter = orderFilter,
                    onFilterChange = { orderFilter = it },
                    orderCount = filteredOrders.size
                )
            }

            if (filteredOrders.isEmpty()) {
                item {
                    Text(text = "No orders yet", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(filteredOrders, key = { it.order.id }) { order ->
                    OrderRow(
                        order = order,
                        onUpdateOverride = { override -> onUpdateOrderStatusOverride(order.order.id, override) },
                        onWriteOff = { onWriteOffOrder(order.order.id) }
                    )
                }
            }

            item {
                LedgerHeader(
                    ledgerFilter = ledgerFilter,
                    onFilterChange = { ledgerFilter = it },
                    ledgerQuery = ledgerQuery,
                    onQueryChange = { ledgerQuery = it }
                )
            }

            if (ledgerSections.isEmpty()) {
                item {
                    Text(text = "No ledger entries", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val visibleSections = ledgerSections.take(visibleLedgerMonths)
                items(visibleSections, key = { it.key }) { section ->
                    LedgerSectionCard(
                        section = section,
                        expanded = expandedLedgerMonths[section.key] ?: false,
                        onToggle = {
                            val current = expandedLedgerMonths[section.key] ?: false
                            expandedLedgerMonths[section.key] = !current
                        }
                    )
                }

                if (visibleLedgerMonths < ledgerSections.size) {
                    item {
                        TextButton(onClick = { visibleLedgerMonths += 3 }) {
                            Text("Load older months")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(customer: CustomerEntity?, balance: BigDecimal) {
    val context = LocalContext.current
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = "Balance",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "Balance", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = formatKes(balance), style = MaterialTheme.typography.titleLarge)
            customer?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { launchDial(context, phone) }) { Text("Call") }
                    TextButton(onClick = { launchSms(context, phone) }) { Text("Message") }
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    amountError: String?,
    amountRegistry: AmountFieldRegistry,
    selectedMethod: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    noteText: String,
    onNoteChange: (String) -> Unit,
    eligibleOrders: List<CustomerOrderUi>,
    selectedOrderId: Long?,
    onSelectOrder: (Long?) -> Unit,
    onSubmit: () -> Unit
) {
    var orderSearch by rememberSaveable { mutableStateOf("") }
    val visibleOrders = remember(eligibleOrders, orderSearch) {
        val lowered = orderSearch.trim().lowercase()
        eligibleOrders
            .filter { order ->
                if (lowered.isBlank()) return@filter true
                val label = orderLabel(order).lowercase()
                label.contains(lowered)
            }
            .take(20)
    }
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Payments,
                    contentDescription = "Record payment",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "Record payment", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))

            val setAmountText by rememberUpdatedState<(String) -> Unit>({ onAmountChange(it) })
            OutlinedTextField(
                value = amountText,
                onValueChange = { onAmountChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
                label = { Text("Amount (KES)") },
                placeholder = { Text("KES 0.00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            amountRegistry.update(setAmountText)
                        }
                    }
            )

            amountError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaymentMethodChip(
                    label = "Cash",
                    selected = selectedMethod == PaymentMethod.CASH,
                    onClick = { onMethodChange(PaymentMethod.CASH) }
                )
                PaymentMethodChip(
                    label = "Mpesa",
                    selected = selectedMethod == PaymentMethod.MPESA,
                    onClick = { onMethodChange(PaymentMethod.MPESA) }
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                label = { Text("Note (optional)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Text(text = "Apply to order (optional)", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = orderSearch,
                onValueChange = { orderSearch = it },
                label = { Text("Search open orders") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { onSelectOrder(null) }) {
                val label = if (selectedOrderId == null) "No order (selected)" else "No order"
                Text(text = label)
            }
            visibleOrders.forEach { order ->
                TextButton(onClick = { onSelectOrder(order.order.id) }) {
                    val label = orderLabel(order)
                    val selectedLabel = "$label (selected)"
                    Text(text = if (selectedOrderId == order.order.id) selectedLabel else label)
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save payment")
            }
        }
    }
}

@Composable
private fun OrdersHeader(
    orderFilter: OrderFilter,
    onFilterChange: (OrderFilter) -> Unit,
    orderCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Orders", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderFilter.values().forEach { filter ->
                FilterChip(
                    selected = orderFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$orderCount orders",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrderRow(
    order: CustomerOrderUi,
    onUpdateOverride: (OrderStatusOverride?) -> Unit,
    onWriteOff: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingOverride by remember { mutableStateOf<OrderStatusOverride?>(null) }
    var confirmWriteOff by remember { mutableStateOf(false) }
    val primaryLine = buildOrderPrimaryLine(order)
    val notes = order.order.notes.ifBlank { "No notes" }.take(60)
    val chipLabel = orderStatusChipLabel(order)
    val chipColor = orderStatusChipColor(order)
    val canWriteOff = canWriteOff(order)

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = primaryLine, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val paymentDetail =
                    when (order.paymentState) {
                        OrderPaymentState.PARTIAL ->
                            "Paid ${formatKes(order.paidAmount)} / ${formatKes(order.order.totalAmount)}"
                        OrderPaymentState.OVERPAID -> {
                            val overpaidAmount = order.paidAmount - order.order.totalAmount
                            "Paid ${formatKes(order.paidAmount)} / ${formatKes(order.order.totalAmount)} (Over ${formatKes(overpaidAmount)})"
                        }
                        else -> null
                    }
                if (paymentDetail != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = paymentDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = chipColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Order actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Close order") },
                        onClick = {
                            pendingOverride = OrderStatusOverride.CLOSED
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reopen order") },
                        onClick = {
                            pendingOverride = OrderStatusOverride.OPEN
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear override") },
                        onClick = {
                            onUpdateOverride(null)
                            menuExpanded = false
                        }
                    )
                    if (canWriteOff) {
                        DropdownMenuItem(
                            text = { Text("Write off balance") },
                            onClick = {
                                confirmWriteOff = true
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    pendingOverride?.let { choice ->
        val label =
            when (choice) {
                OrderStatusOverride.CLOSED -> "close"
                OrderStatusOverride.OPEN -> "reopen"
            }
        AlertDialog(
            onDismissRequest = { pendingOverride = null },
            title = { Text("Apply override?") },
            text = { Text("This will $label the order regardless of payment state.") },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateOverride(choice)
                    pendingOverride = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverride = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmWriteOff) {
        AlertDialog(
            onDismissRequest = { confirmWriteOff = false },
            title = { Text("Write off balance?") },
            text = { Text("This will mark the remaining balance as an adjustment.") },
            confirmButton = {
                TextButton(onClick = {
                    onWriteOff()
                    confirmWriteOff = false
                }) { Text("Write off") }
            },
            dismissButton = {
                TextButton(onClick = { confirmWriteOff = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LedgerHeader(
    ledgerFilter: LedgerFilter,
    onFilterChange: (LedgerFilter) -> Unit,
    ledgerQuery: String,
    onQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = "Ledger",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "Ledger", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ledgerQuery,
            onValueChange = onQueryChange,
            label = { Text("Search ledger") },
            placeholder = { Text("Notes, amount, date") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedgerFilter.values().forEach { filter ->
                FilterChip(
                    selected = ledgerFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}

@Composable
private fun LedgerSectionCard(
    section: LedgerSection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = section.title, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onToggle) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                section.entries.forEach { entry ->
                    LedgerRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(entry: AccountEntryEntity) {
    val amountColor =
        when (entry.type) {
            EntryType.DEBIT -> MaterialTheme.colorScheme.error
            EntryType.CREDIT -> MaterialTheme.colorScheme.primary
            EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
        }
    val typeLabel =
        when (entry.type) {
            EntryType.DEBIT -> "Order"
            EntryType.CREDIT -> "Payment"
            EntryType.WRITE_OFF -> "Adjustment"
        }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = formatDateTime(entry.date), style = MaterialTheme.typography.labelMedium)
        Text(text = entry.description, style = MaterialTheme.typography.bodyMedium)
        val sign = if (entry.type == EntryType.DEBIT) "+" else "-"
        Text(text = typeLabel, style = MaterialTheme.typography.labelSmall, color = amountColor)
        Text(
            text = "$sign ${formatKes(entry.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor
        )
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Money,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun launchDial(context: android.content.Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun launchSms(context: android.content.Context, phone: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun filterOrders(orders: List<CustomerOrderUi>, filter: OrderFilter): List<CustomerOrderUi> {
    return orders.filter { order ->
        when (filter) {
            OrderFilter.All -> true
            OrderFilter.Open -> order.effectiveStatus == OrderEffectiveStatus.OPEN
            OrderFilter.Closed -> order.effectiveStatus == OrderEffectiveStatus.CLOSED
            OrderFilter.Unpaid -> order.paymentState == OrderPaymentState.UNPAID
            OrderFilter.Partial -> order.paymentState == OrderPaymentState.PARTIAL
            OrderFilter.Paid -> order.paymentState == OrderPaymentState.PAID
            OrderFilter.Overpaid -> order.paymentState == OrderPaymentState.OVERPAID
        }
    }
}

private fun filterLedger(
    ledger: List<AccountEntryEntity>,
    filter: LedgerFilter,
    query: String
): List<AccountEntryEntity> {
    val lowered = query.trim().lowercase()
    return ledger.filter { entry ->
        val matchesFilter =
            when (filter) {
                LedgerFilter.All -> true
                LedgerFilter.Orders -> entry.type == EntryType.DEBIT
                LedgerFilter.Payments -> entry.type == EntryType.CREDIT
                LedgerFilter.Adjustments -> entry.type == EntryType.WRITE_OFF
            }
        if (!matchesFilter) return@filter false
        if (lowered.isBlank()) return@filter true
        val amountText = entry.amount.toPlainString()
        val dateText = formatDateTime(entry.date)
        entry.description.lowercase().contains(lowered) ||
            amountText.contains(lowered) ||
            dateText.lowercase().contains(lowered)
    }
}

private fun buildLedgerSections(entries: List<AccountEntryEntity>): List<LedgerSection> {
    val sections = linkedMapOf<String, LedgerSection>()
    entries.forEach { entry ->
        val (key, title) = yearMonthKey(entry.date)
        val existing = sections[key]
        if (existing == null) {
            sections[key] = LedgerSection(key = key, title = title, entries = mutableListOf(entry))
        } else {
            existing.entries.add(entry)
        }
    }
    return sections.values.toList()
}

private fun yearMonthKey(epochMillis: Long): Pair<String, String> {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val key = String.format(Locale.US, "%04d-%02d", year, month + 1)
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return key to formatter.format(calendar.time)
}

private fun currentMonthKey(): String {
    val now = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
}

private fun orderLabel(order: CustomerOrderUi): String {
    val dateLabel = formatShortDate(order.order.orderDate)
    val stateLabel =
        when (order.paymentState) {
            OrderPaymentState.UNPAID -> "Unpaid"
            OrderPaymentState.PARTIAL -> "Partial"
            OrderPaymentState.PAID -> "Paid"
            OrderPaymentState.OVERPAID -> "Overpaid"
        }
    return "$dateLabel - ${formatKes(order.order.totalAmount)} - $stateLabel"
}

private fun buildOrderPrimaryLine(order: CustomerOrderUi): String = orderLabel(order)

private fun orderStatusChipLabel(order: CustomerOrderUi): String {
    return when (order.statusOverride) {
        OrderStatusOverride.CLOSED -> "CLOSED (override)"
        OrderStatusOverride.OPEN -> "OPEN (override)"
        null ->
            when (order.paymentState) {
                OrderPaymentState.UNPAID -> "UNPAID"
                OrderPaymentState.PARTIAL -> "PARTIAL"
                OrderPaymentState.PAID -> "PAID"
                OrderPaymentState.OVERPAID -> "OVERPAID"
            }
    }
}

@Composable
private fun orderStatusChipColor(order: CustomerOrderUi) =
    when {
        order.statusOverride != null -> MaterialTheme.colorScheme.surfaceVariant
        order.paymentState == OrderPaymentState.UNPAID -> MaterialTheme.colorScheme.errorContainer
        order.paymentState == OrderPaymentState.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
        order.paymentState == OrderPaymentState.OVERPAID -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

private fun canWriteOff(order: CustomerOrderUi): Boolean {
    if (order.effectiveStatus != OrderEffectiveStatus.OPEN) return false
    if (order.paidAmount >= order.order.totalAmount) return false
    val cutoff = order.order.orderDate.plus(1, DateTimeUnit.MONTH)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return today >= cutoff
}

private fun formatShortDate(date: kotlinx.datetime.LocalDate): String {
    val month =
        when (date.monthNumber) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Month"
        }
    return "$month ${date.dayOfMonth}"
}

private data class LedgerSection(
    val key: String,
    val title: String,
    val entries: MutableList<AccountEntryEntity>
)

private enum class OrderFilter(val label: String) {
    All("All"),
    Open("Open"),
    Closed("Closed"),
    Unpaid("Unpaid"),
    Partial("Partial"),
    Paid("Paid"),
    Overpaid("Overpaid")
}

private enum class LedgerFilter(val label: String) {
    All("All"),
    Orders("Orders"),
    Payments("Payments"),
    Adjustments("Adjustments")
}
