@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountBalanceWallet
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity?,
    ledger: List<AccountEntryEntity>,
    balance: BigDecimal,
    financeSummary: CustomerFinanceSummary?,
    orders: List<CustomerOrderUi>,
    orderLabels: Map<Long, String>,
    onBack: () -> Unit,
    onPaymentHistory: (Long) -> Unit,
    onOpenStatement: (Long) -> Unit,
    onReceivePayment: () -> Unit,
    onOrderPaymentHistory: (Long) -> Unit,
    onUpdateOrderStatusOverride: (Long, OrderStatusOverride?) -> Unit,
    onWriteOffOrder: (Long) -> Unit
) {
    var orderFilter by remember { mutableStateOf(OrderFilter.All) }
    var ledgerFilter by remember { mutableStateOf(LedgerFilter.All) }
    var ledgerQuery by remember { mutableStateOf("") }
    var debouncedLedgerQuery by remember { mutableStateOf("") }
    var visibleLedgerMonths by remember { mutableStateOf(3) }
    val expandedLedgerMonths = remember { mutableStateMapOf<String, Boolean>() }

    val filteredOrders by remember(orders, orderFilter) {
        derivedStateOf { filterOrders(orders, orderFilter) }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { ledgerQuery }
            .debounce(300)
            .distinctUntilChanged()
            .collect { debouncedLedgerQuery = it }
    }

    val filteredLedger by remember(ledger, ledgerFilter, debouncedLedgerQuery) {
        derivedStateOf { filterLedger(ledger, ledgerFilter, debouncedLedgerQuery) }
    }

    val ledgerSections = remember(filteredLedger) { buildLedgerSections(filteredLedger) }
    val currentMonthKey = remember { currentMonthKey() }

    LaunchedEffect(ledgerSections) {
        ledgerSections.forEach { section ->
            expandedLedgerMonths.putIfAbsent(section.key, section.key == currentMonthKey)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
                },
                actions = {
                    IconButton(
                        onClick = { customer?.id?.let { onOpenStatement(it) } },
                        enabled = customer != null
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Statement"
                        )
                    }
                    IconButton(
                        onClick = { customer?.id?.let { onPaymentHistory(it) } },
                        enabled = customer != null
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Receipt history")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BalanceCard(
                    customer = customer,
                    balance = balance,
                    financeSummary = financeSummary,
                    canReceive = customer != null,
                    onReceivePayment = onReceivePayment
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
                        onPaymentHistory = { onOrderPaymentHistory(order.order.id) },
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
                        },
                        orderLabels = orderLabels
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
private fun BalanceCard(
    customer: CustomerEntity?,
    balance: BigDecimal,
    financeSummary: CustomerFinanceSummary?,
    canReceive: Boolean,
    onReceivePayment: () -> Unit
) {
    val context = LocalContext.current
    val summary = financeSummary
    val orderTotal = summary?.orderTotal ?: BigDecimal.ZERO
    val paidToOrders = summary?.paidToOrders ?: BigDecimal.ZERO
    val availableCredit = summary?.availableCredit ?: BigDecimal.ZERO
    val netBalance = summary?.netBalance ?: balance
    val netLabel =
        if (summary == null) {
            "Balance"
        } else {
            when {
                netBalance > BigDecimal.ZERO -> "Owes"
                netBalance < BigDecimal.ZERO -> "Credit"
                else -> "Settled"
            }
        }
    val netAmount = netBalance.abs()
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
                Text(text = "Customer summary", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "$netLabel ${formatKes(netAmount)}", style = MaterialTheme.typography.titleLarge)
            if (summary != null) {
                Spacer(Modifier.height(8.dp))
                SummaryRow(label = "Orders total", amount = formatKes(orderTotal))
                SummaryRow(label = "Payments applied", amount = formatKes(paidToOrders))
                SummaryRow(label = "Credit available", amount = formatKes(availableCredit.max(BigDecimal.ZERO)))
                if (availableCredit > BigDecimal.ZERO) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You will be asked before applying credit to new orders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onReceivePayment,
                enabled = canReceive
            ) {
                Text("Record payment")
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall
        )
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
    onPaymentHistory: () -> Unit,
    onUpdateOverride: (OrderStatusOverride?) -> Unit,
    onWriteOff: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingOverride by remember { mutableStateOf<OrderStatusOverride?>(null) }
    var confirmWriteOff by remember { mutableStateOf(false) }
    val today = rememberCurrentDate()
    val primaryLine = buildOrderPrimaryLine(order)
    val notes = order.order.notes.ifBlank { "No notes" }.take(60)
    val chipLabel = orderStatusChipLabel(order)
    val chipColor = orderStatusChipColor(order)
    val canWriteOff = canWriteOff(order, today)

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
                        text = { Text("Payment history") },
                        onClick = {
                            onPaymentHistory()
                            menuExpanded = false
                        }
                    )
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
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = "Statement",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "Statement", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ledgerQuery,
            onValueChange = onQueryChange,
            label = { Text("Search statement") },
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
    onToggle: () -> Unit,
    orderLabels: Map<Long, String>
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
                    LedgerRow(entry, orderLabels)
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(entry: AccountEntryEntity, orderLabels: Map<Long, String>) {
    val amountColor =
        when (entry.type) {
            EntryType.DEBIT -> MaterialTheme.colorScheme.error
            EntryType.CREDIT -> MaterialTheme.colorScheme.primary
            EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
            EntryType.REVERSAL -> MaterialTheme.colorScheme.error
        }
    val typeLabel =
        when (entry.type) {
            EntryType.DEBIT -> "Order charge"
            EntryType.CREDIT ->
                if (entry.orderId == null) "Extra credit" else "Payment"
            EntryType.WRITE_OFF -> "Write-off"
            EntryType.REVERSAL ->
                if (entry.orderId == null) "Credit reversal" else "Reversal"
        }
    val orderLabel =
        entry.orderId?.let { id -> orderLabels[id] ?: "Order ID $id" }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = formatDateTime(entry.date), style = MaterialTheme.typography.labelMedium)
        if (orderLabel != null) {
            Text(
                text = orderLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = entry.description, style = MaterialTheme.typography.bodyMedium)
        val sign =
            when (entry.type) {
                EntryType.DEBIT, EntryType.REVERSAL -> "+"
                EntryType.CREDIT, EntryType.WRITE_OFF -> "-"
            }
        Text(text = typeLabel, style = MaterialTheme.typography.labelSmall, color = amountColor)
        Text(
            text = "$sign ${formatKes(entry.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor
        )
    }
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
            LedgerFilter.Payments -> entry.type == EntryType.CREDIT || entry.type == EntryType.REVERSAL
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

private fun canWriteOff(order: CustomerOrderUi, today: LocalDate): Boolean {
    if (order.effectiveStatus != OrderEffectiveStatus.OPEN) return false
    if (order.paidAmount >= order.order.totalAmount) return false
    val cutoff = order.order.orderDate.plus(1, DateTimeUnit.MONTH)
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

