package com.zeynbakers.order_management_system.customer.ui

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
internal fun BalanceCard(
    customer: CustomerEntity?,
    balance: BigDecimal,
    financeSummary: CustomerFinanceSummary?,
    canReceive: Boolean,
    onReceivePayment: () -> Unit,
    onViewStatement: () -> Unit,
    onViewPaymentHistory: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager =
        remember(context) {
            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    val summary = financeSummary
    val orderTotal = summary?.orderTotal ?: BigDecimal.ZERO
    val paidToOrders = summary?.paidToOrders ?: BigDecimal.ZERO
    val availableCredit = summary?.availableCredit ?: BigDecimal.ZERO
    val netBalance = balance
    val netLabel =
        if (summary == null) {
            stringResource(R.string.customer_detail_balance)
        } else {
            when {
                netBalance > BigDecimal.ZERO -> stringResource(R.string.customer_filter_owes)
                netBalance < BigDecimal.ZERO -> stringResource(R.string.customer_filter_credit)
                else -> stringResource(R.string.customer_filter_settled)
            }
        }
    val netAmount = netBalance.abs()
    val fontScale = LocalDensity.current.fontScale
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = stringResource(R.string.customer_detail_balance),
                    tint = MaterialTheme.colorScheme.primary
                )
            Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.customer_detail_summary), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.customer_detail_balance_line, netLabel, formatKes(netAmount)),
                style = MaterialTheme.typography.titleLarge
            )
            if (summary != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.customer_detail_orders_total),
                        amount = formatKes(orderTotal)
                    )
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.customer_detail_payments_applied),
                        amount = formatKes(paidToOrders)
                    )
                }
                Spacer(Modifier.height(8.dp))
                SummaryRow(
                    label = stringResource(R.string.customer_detail_credit_available),
                    amount = formatKes(availableCredit.max(BigDecimal.ZERO))
                )
                if (availableCredit > BigDecimal.ZERO) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.customer_detail_credit_prompt_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            customer?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("customer_phone", phone))
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = stringResource(R.string.customer_detail_phone),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { launchCustomerDial(context, phone) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = stringResource(R.string.customer_detail_call)
                            )
                        }
                        IconButton(
                            onClick = { launchCustomerMessage(context, phone) }
                        ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.customer_action_message)
                        )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onViewStatement,
                enabled = canReceive,
                modifier = Modifier
                    .testTag("customer-view-statement-button")
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.customer_action_view_statement))
            }
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackActions = fontScale > 1.3f || maxWidth < 360.dp
                if (stackActions) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReceivePayment,
                            enabled = canReceive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.customer_action_record_payment))
                        }
                        TextButton(
                            onClick = onViewPaymentHistory,
                            enabled = canReceive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.customer_detail_receipt_history))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReceivePayment,
                            enabled = canReceive,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.customer_action_record_payment))
                        }
                        TextButton(
                            onClick = onViewPaymentHistory,
                            enabled = canReceive,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.customer_detail_receipt_history))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SummaryRow(
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
internal fun OrdersHeader(
    orders: List<CustomerOrderUi>,
    orderFilter: OrderFilter,
    onFilterChange: (OrderFilter) -> Unit,
    orderCount: Int
) {
    val totalOwed =
        remember(orders) {
            orders.fold(BigDecimal.ZERO) { acc, order ->
                val due = order.order.totalAmount - order.paidAmount
                if (due > BigDecimal.ZERO) acc + due else acc
            }
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.summary_orders), style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(R.plurals.day_orders_count_plural, orderCount, orderCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text =
                if (totalOwed > BigDecimal.ZERO) {
                    stringResource(R.string.customer_orders_owes_summary, formatKes(totalOwed))
                } else {
                    stringResource(R.string.customer_orders_clear_summary)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
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
                    label = { Text(stringResource(filter.labelRes)) }
                )
            }
        }
    }
}

@Composable
internal fun OrderRow(
    order: CustomerOrderUi,
    onPaymentHistory: () -> Unit,
    onUpdateOverride: (OrderStatusOverride?) -> Unit,
    onWriteOff: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingOverride by remember { mutableStateOf<OrderStatusOverride?>(null) }
    var confirmWriteOff by remember { mutableStateOf(false) }
    val today = rememberCurrentDate()
    val notes = order.order.notes.trim().take(80)
    val dateLabel = formatShortDate(order.order.orderDate)
    val dueAmount = (order.order.totalAmount - order.paidAmount).max(BigDecimal.ZERO)
    val creditAmount = (order.paidAmount - order.order.totalAmount).max(BigDecimal.ZERO)
    val canWriteOff = canWriteOff(order, today)
    val accountStateLabel =
        when {
            dueAmount > BigDecimal.ZERO ->
                stringResource(R.string.customer_order_owes_value, formatKes(dueAmount))
            creditAmount > BigDecimal.ZERO ->
                stringResource(R.string.customer_order_credit_value, formatKes(creditAmount))
            else -> stringResource(R.string.customer_order_settled)
        }
    val accountStateColor =
        when {
            dueAmount > BigDecimal.ZERO -> MaterialTheme.colorScheme.error
            creditAmount > BigDecimal.ZERO -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.tertiary
        }
    val lifecycleLabel =
        if (order.effectiveStatus == OrderEffectiveStatus.CLOSED) {
            stringResource(R.string.customer_detail_filter_closed)
        } else {
            stringResource(R.string.customer_detail_filter_open)
        }
    val lifecycleContainer =
        if (order.effectiveStatus == OrderEffectiveStatus.CLOSED) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val lifecycleContent =
        if (order.effectiveStatus == OrderEffectiveStatus.CLOSED) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

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
                Text(
                    text = stringResource(R.string.customer_order_title_with_date, dateLabel),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatKes(order.order.totalAmount),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = lifecycleContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = lifecycleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = lifecycleContent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = accountStateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accountStateColor
                )
            }

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    stringResource(
                        R.string.customer_order_paid_of_total,
                        formatKes(order.paidAmount.max(BigDecimal.ZERO)),
                        formatKes(order.order.totalAmount)
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPaymentHistory) {
                    Text(stringResource(R.string.customer_order_action_payments))
                }
                if (canWriteOff) {
                    TextButton(onClick = { confirmWriteOff = true }) {
                        Text(stringResource(R.string.customer_detail_write_off_action))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(R.string.customer_actions))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.customer_detail_close_order)) },
                        onClick = {
                            pendingOverride = OrderStatusOverride.CLOSED
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.customer_detail_reopen_order)) },
                        onClick = {
                            pendingOverride = OrderStatusOverride.OPEN
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.customer_detail_clear_override)) },
                        onClick = {
                            onUpdateOverride(null)
                            menuExpanded = false
                        }
                    )
                    if (canWriteOff) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.customer_detail_write_off_balance)) },
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
                OrderStatusOverride.CLOSED -> stringResource(R.string.customer_detail_close_order_verb)
                OrderStatusOverride.OPEN -> stringResource(R.string.customer_detail_reopen_order_verb)
            }
        AlertDialog(
            onDismissRequest = { pendingOverride = null },
            title = { Text(stringResource(R.string.customer_detail_apply_override_title)) },
            text = { Text(stringResource(R.string.customer_detail_apply_override_message, label)) },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateOverride(choice)
                    pendingOverride = null
                }) { Text(stringResource(R.string.customer_detail_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverride = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (confirmWriteOff) {
        AlertDialog(
            onDismissRequest = { confirmWriteOff = false },
            title = { Text(stringResource(R.string.customer_detail_write_off_title)) },
            text = { Text(stringResource(R.string.customer_detail_write_off_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onWriteOff()
                    confirmWriteOff = false
                }) { Text(stringResource(R.string.customer_detail_write_off_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWriteOff = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
internal fun LedgerHeader(
    ledgerFilter: LedgerFilter,
    onFilterChange: (LedgerFilter) -> Unit,
    ledgerQuery: String,
    onQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = stringResource(R.string.customer_detail_statement),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.customer_detail_statement), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ledgerQuery,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.customer_detail_search_statement)) },
            placeholder = { Text(stringResource(R.string.customer_detail_search_statement_placeholder)) },
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
                    label = { Text(stringResource(filter.labelRes)) }
                )
            }
        }
    }
}

@Composable
internal fun LedgerSectionCard(
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
                    Text(
                        if (expanded) {
                            stringResource(R.string.customer_detail_hide)
                        } else {
                            stringResource(R.string.customer_detail_show)
                        }
                    )
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
internal fun LedgerRow(entry: AccountEntryEntity, orderLabels: Map<Long, String>) {
    val amountColor =
        when (entry.type) {
            EntryType.DEBIT -> MaterialTheme.colorScheme.error
            EntryType.CREDIT -> MaterialTheme.colorScheme.primary
            EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
            EntryType.REVERSAL -> MaterialTheme.colorScheme.error
        }
    val typeLabel =
        when (entry.type) {
            EntryType.DEBIT -> stringResource(R.string.ledger_entry_order_charge)
            EntryType.CREDIT ->
                if (entry.orderId == null) stringResource(R.string.money_entry_extra_credit) else stringResource(R.string.ledger_entry_payment)
            EntryType.WRITE_OFF -> stringResource(R.string.ledger_entry_write_off)
            EntryType.REVERSAL ->
                if (entry.orderId == null) stringResource(R.string.money_entry_credit_reversal) else stringResource(R.string.money_entry_payment_reversal)
        }
    val orderLabel =
        entry.orderId?.let { id -> orderLabels[id] ?: stringResource(R.string.customer_detail_order_id, id) }
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

internal fun launchCustomerDial(context: android.content.Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

internal fun filterOrders(orders: List<CustomerOrderUi>, filter: OrderFilter): List<CustomerOrderUi> {
    return orders.filter { order ->
        when (filter) {
            OrderFilter.All -> true
            OrderFilter.Owes -> order.order.totalAmount > order.paidAmount
            OrderFilter.Paid -> order.paidAmount >= order.order.totalAmount
            OrderFilter.Closed -> order.effectiveStatus == OrderEffectiveStatus.CLOSED
        }
    }
}

internal fun filterLedger(
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

internal fun buildLedgerSections(entries: List<AccountEntryEntity>): List<LedgerSection> {
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

internal fun yearMonthKey(epochMillis: Long): Pair<String, String> {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val key = String.format(Locale.US, "%04d-%02d", year, month + 1)
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return key to formatter.format(calendar.time)
}

internal fun currentMonthKey(): String {
    val now = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
}

@Composable
internal fun orderLabel(order: CustomerOrderUi): String {
    val dateLabel = formatShortDate(order.order.orderDate)
    val stateLabel =
        when (order.paymentState) {
            OrderPaymentState.UNPAID -> stringResource(R.string.day_status_unpaid)
            OrderPaymentState.PARTIAL -> stringResource(R.string.day_status_partial)
            OrderPaymentState.PAID -> stringResource(R.string.day_status_paid)
            OrderPaymentState.OVERPAID -> stringResource(R.string.day_status_overpaid)
        }
    return stringResource(
        R.string.customer_detail_order_primary_line,
        dateLabel,
        formatKes(order.order.totalAmount),
        stateLabel
    )
}

@Composable
internal fun buildOrderPrimaryLine(order: CustomerOrderUi): String = orderLabel(order)

@Composable
internal fun orderStatusChipLabel(order: CustomerOrderUi): String {
    return when (order.statusOverride) {
        OrderStatusOverride.CLOSED -> stringResource(R.string.customer_detail_status_closed_override)
        OrderStatusOverride.OPEN -> stringResource(R.string.customer_detail_status_open_override)
        null ->
            when (order.paymentState) {
                OrderPaymentState.UNPAID -> stringResource(R.string.customer_detail_status_unpaid)
                OrderPaymentState.PARTIAL -> stringResource(R.string.customer_detail_status_partial)
                OrderPaymentState.PAID -> stringResource(R.string.customer_detail_status_paid)
                OrderPaymentState.OVERPAID -> stringResource(R.string.customer_detail_status_overpaid)
            }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String
) {
    Surface(
        modifier = modifier,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
internal fun orderStatusChipColor(order: CustomerOrderUi) =
    when {
        order.statusOverride != null -> MaterialTheme.colorScheme.surfaceVariant
        order.paymentState == OrderPaymentState.UNPAID -> MaterialTheme.colorScheme.errorContainer
        order.paymentState == OrderPaymentState.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
        order.paymentState == OrderPaymentState.OVERPAID -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

internal fun canWriteOff(order: CustomerOrderUi, today: LocalDate): Boolean {
    if (order.effectiveStatus != OrderEffectiveStatus.OPEN) return false
    if (order.paidAmount >= order.order.totalAmount) return false
    val cutoff = order.order.orderDate.plus(1, DateTimeUnit.MONTH)
    return today >= cutoff
}

internal fun formatShortDate(date: kotlinx.datetime.LocalDate): String {
    val index = (date.monthNumber - 1).coerceIn(0, 11)
    val month = DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths[index]
    return "$month ${date.dayOfMonth}"
}

internal data class LedgerSection(
    val key: String,
    val title: String,
    val entries: MutableList<AccountEntryEntity>
)

internal enum class OrderFilter(val labelRes: Int) {
    All(R.string.customer_filter_all),
    Owes(R.string.customer_filter_owes),
    Paid(R.string.day_filter_paid),
    Closed(R.string.customer_detail_filter_closed)
}

internal enum class LedgerFilter(val labelRes: Int) {
    All(R.string.ledger_filter_all),
    Orders(R.string.customer_detail_filter_orders),
    Payments(R.string.ledger_filter_payments),
    Adjustments(R.string.ledger_filter_adjustments)
}


