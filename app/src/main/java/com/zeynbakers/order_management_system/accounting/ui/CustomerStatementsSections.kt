package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.core.ui.components.AppFilterRow
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import java.text.DateFormatSymbols
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun StatementHeader(
        customerName: String,
        phone: String,
        rangeLabel: String,
        hideAmounts: Boolean,
        showReversals: Boolean,
        onToggleHideAmounts: () -> Unit,
        onToggleReversals: () -> Unit,
        onChangeCustomer: () -> Unit,
        onOpenAllLedger: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = stringResource(R.string.money_statement_title),
                        style = MaterialTheme.typography.titleLarge
                )
                Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleHideAmounts) {
                Icon(
                        imageVector =
                                if (hideAmounts) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                        contentDescription =
                                if (hideAmounts) {
                                    stringResource(R.string.action_show_balances)
                                } else {
                                    stringResource(R.string.action_hide_balances)
                                }
                )
            }
            TextButton(onClick = onChangeCustomer) { Text(stringResource(R.string.action_change)) }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                        text = {
                            Text(
                                    if (showReversals) {
                                        stringResource(R.string.action_hide_reversals)
                                    } else {
                                        stringResource(R.string.action_show_reversals)
                                    }
                            )
                        },
                        onClick = {
                            onToggleReversals()
                            showMenu = false
                        }
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.money_advanced_ledger_title)) },
                        onClick = {
                            onOpenAllLedger()
                            showMenu = false
                        }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = customerName, style = MaterialTheme.typography.titleMedium)
        if (phone.isNotBlank()) {
            Text(
                    text = phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun RangeSelector(
        rangeType: StatementRangeType,
        onRangeTypeChange: (StatementRangeType) -> Unit,
        monthOptions: List<MonthOption>,
        selectedMonth: Int,
        selectedYear: Int,
        onMonthSelected: (Int, Int) -> Unit,
        customStart: String,
        customEnd: String,
        onCustomStartChange: (String) -> Unit,
        onCustomEndChange: (String) -> Unit,
        dateInputPlaceholder: String
) {
    var isMonthMenuOpen by remember { mutableStateOf(false) }
    val selectedLabel =
            monthOptions.firstOrNull { it.year == selectedYear && it.month == selectedMonth }?.label
                    ?: "${monthName(selectedMonth)} $selectedYear"

    Column(modifier = Modifier.fillMaxWidth()) {
        AppFilterRow(
                options = rangeTypeOptions(),
                selectedKey = rangeType.name,
                onSelect = { onRangeTypeChange(StatementRangeType.valueOf(it)) }
        )

        if (rangeType == StatementRangeType.Month) {
            Spacer(Modifier.height(8.dp))
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { isMonthMenuOpen = true }
                                    .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = stringResource(R.string.money_month),
                            style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.money_select_month)
                )
            }
            DropdownMenu(
                    expanded = isMonthMenuOpen,
                    onDismissRequest = { isMonthMenuOpen = false }
            ) {
                monthOptions.forEach { option ->
                    DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onMonthSelected(option.year, option.month)
                                isMonthMenuOpen = false
                            }
                    )
                }
            }
        }

        if (rangeType == StatementRangeType.Custom) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                        value = customStart,
                        onValueChange = onCustomStartChange,
                        label = { Text(stringResource(R.string.money_start_date)) },
                        placeholder = { Text(dateInputPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                        value = customEnd,
                        onValueChange = onCustomEndChange,
                        label = { Text(stringResource(R.string.money_end_date)) },
                        placeholder = { Text(dateInputPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun StatementSummaryCard(
        openingBalance: BigDecimal,
        charges: BigDecimal,
        payments: BigDecimal,
        writeOffs: BigDecimal,
        reversals: BigDecimal,
        hideAmounts: Boolean,
        showReversals: Boolean,
        closingBalance: BigDecimal
) {
    AppCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                    text = stringResource(R.string.money_statement_summary_title),
                    style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(6.dp))
            SummaryRow(
                    label = stringResource(R.string.money_statement_opening_balance),
                    amount = formatKes(openingBalance),
                    hideAmounts = hideAmounts
            )
            SummaryRow(
                    label = stringResource(R.string.money_statement_charges),
                    amount = formatKes(charges),
                    hideAmounts = hideAmounts
            )
            SummaryRow(
                    label = stringResource(R.string.money_statement_payments),
                    amount = formatKes(payments),
                    hideAmounts = hideAmounts
            )
            SummaryRow(
                    label = stringResource(R.string.money_statement_write_offs),
                    amount = formatKes(writeOffs),
                    hideAmounts = hideAmounts
            )
            if (showReversals) {
                SummaryRow(
                        label = stringResource(R.string.money_statement_reversals),
                        amount = formatKes(reversals),
                        hideAmounts = hideAmounts
                )
            }
            Spacer(Modifier.height(4.dp))
            SummaryRow(
                    label = stringResource(R.string.money_statement_closing_balance),
                    amount = formatKes(closingBalance),
                    hideAmounts = hideAmounts
            )
        }
    }
}

@Composable
internal fun SummaryRow(label: String, amount: String, hideAmounts: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text =
                        if (hideAmounts) {
                            stringResource(R.string.money_amount_hidden)
                        } else {
                            amount
                        },
                style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun rangeTypeOptions(): List<AppFilterOption> {
    return listOf(
            AppFilterOption(StatementRangeType.All.name, stringResource(R.string.money_all_time)),
            AppFilterOption(StatementRangeType.Month.name, stringResource(R.string.money_month)),
            AppFilterOption(StatementRangeType.Custom.name, stringResource(R.string.money_custom))
    )
}

@Composable
internal fun StatementEntryRow(
        modifier: Modifier = Modifier,
        entry: StatementEntryUi,
        hideAmounts: Boolean,
        orderLabel: String?
) {
    val typeLabel =
            when (entry.entry.type) {
                EntryType.DEBIT -> stringResource(R.string.money_entry_order)
                EntryType.CREDIT ->
                        if (entry.entry.orderId == null) {
                            stringResource(R.string.money_entry_extra_credit)
                        } else {
                            stringResource(R.string.money_entry_payment)
                        }
                EntryType.WRITE_OFF -> stringResource(R.string.money_entry_bad_debt_writeoff)
                EntryType.REVERSAL ->
                        if (entry.entry.orderId == null) {
                            stringResource(R.string.money_entry_credit_reversal)
                        } else {
                            stringResource(R.string.money_entry_payment_reversal)
                        }
            }
    val amountColor = entryTypeColor(entry.entry.type)
    val sign =
            if (entry.entry.type == EntryType.DEBIT || entry.entry.type == EntryType.REVERSAL) "+"
            else "-"
    val balanceSign = if (entry.runningBalance < BigDecimal.ZERO) "-" else ""
    val balanceColor =
            when {
                entry.runningBalance > BigDecimal.ZERO -> MaterialTheme.colorScheme.error
                entry.runningBalance < BigDecimal.ZERO -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

    val icon =
            when (entry.entry.type) {
                EntryType.DEBIT -> Icons.Filled.ShoppingBag
                EntryType.CREDIT -> Icons.Outlined.Payments
                EntryType.WRITE_OFF -> Icons.Filled.MoneyOff
                EntryType.REVERSAL -> Icons.Filled.Warning
            }

    AppCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text =
                                    if (hideAmounts) {
                                        stringResource(R.string.money_amount_hidden)
                                    } else {
                                        "$sign ${formatKes(entry.entry.amount)}"
                                    },
                            style = MaterialTheme.typography.titleSmall,
                            color = amountColor,
                            fontWeight = FontWeight.Bold
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = formatDateTime(entry.entry.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        orderLabel?.let {
                            Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.entry.description.isNotBlank()) {
                            Text(
                                    text = entry.entry.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                            text =
                                    if (hideAmounts) {
                                        stringResource(R.string.money_amount_hidden)
                                    } else {
                                        stringResource(
                                                R.string.money_balance_label,
                                                balanceSign,
                                                formatKes(entry.runningBalance.abs())
                                        )
                                    },
                            style = MaterialTheme.typography.labelSmall,
                            color = balanceColor,
                            modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatementCustomerRow(
        modifier: Modifier = Modifier,
        customer: CustomerAccountSummary,
        hideAmounts: Boolean,
        onClick: () -> Unit
) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = customer.name.ifBlank { stringResource(R.string.customer_unknown) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                if (customer.phone.isNotBlank()) {
                    Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                        text =
                                if (hideAmounts) {
                                    stringResource(
                                            R.string.money_billed_paid,
                                            stringResource(R.string.money_amount_hidden),
                                            stringResource(R.string.money_amount_hidden)
                                    )
                                } else {
                                    stringResource(
                                            R.string.money_billed_paid,
                                            formatKes(customer.billed),
                                            formatKes(customer.paid)
                                    )
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            BalanceChip(balance = customer.balance, hideAmounts = hideAmounts)
        }
    }
}

@Composable
internal fun BalanceChip(balance: BigDecimal, hideAmounts: Boolean) {
    val (label, color) =
            when {
                balance > BigDecimal.ZERO ->
                        stringResource(R.string.money_balance_due) to
                                MaterialTheme.colorScheme.errorContainer
                balance < BigDecimal.ZERO ->
                        stringResource(R.string.money_balance_extra) to
                                MaterialTheme.colorScheme.tertiaryContainer
                else ->
                        stringResource(R.string.money_balance_clear) to
                                MaterialTheme.colorScheme.secondaryContainer
            }
    Surface(
            color = color,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.heightIn(min = 32.dp)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text(
                    text =
                            if (hideAmounts) {
                                stringResource(R.string.money_amount_hidden)
                            } else {
                                formatKes(balance.abs())
                            },
                    style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
internal fun InfoCard(text: String) {
    AppCard {
        Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun EmptyState(text: String) {
    AppEmptyState(title = stringResource(R.string.money_statement_title), body = text)
}

internal fun buildStatementEntries(entries: List<AccountEntryEntity>): List<StatementEntryUi> {
    val sorted = entries.sortedWith(compareBy<AccountEntryEntity> { it.date }.thenBy { it.id })
    var running = BigDecimal.ZERO
    return sorted.map { entry ->
        running += signedAmount(entry)
        StatementEntryUi(
                entry = entry,
                date =
                        Instant.fromEpochMilliseconds(entry.date)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date,
                runningBalance = running
        )
    }
}

internal fun buildStatementRange(
        rangeType: StatementRangeType,
        selectedYear: Int,
        selectedMonth: Int,
        customStart: String,
        customEnd: String,
        customRangeLabel: String,
        allTimeLabel: String,
        rangeToSeparator: String
): StatementRange {
    return when (rangeType) {
        StatementRangeType.All -> StatementRange(allTimeLabel, null, null, true)
        StatementRangeType.Month -> {
            val days = daysInMonth(selectedYear, selectedMonth)
            val start = LocalDate(selectedYear, selectedMonth, 1)
            val end = LocalDate(selectedYear, selectedMonth, days)
            StatementRange("${monthName(selectedMonth)} $selectedYear", start, end, true)
        }
        StatementRangeType.Custom -> {
            val start = parseDate(customStart)
            val end = parseDate(customEnd)
            val isValid = start != null && end != null && start <= end
            val label = if (isValid) "$start $rangeToSeparator $end" else customRangeLabel
            StatementRange(label, start, end, isValid)
        }
    }
}

internal fun buildTotals(entries: List<StatementEntryUi>): StatementTotals {
    var charges = BigDecimal.ZERO
    var payments = BigDecimal.ZERO
    var writeOffs = BigDecimal.ZERO
    var reversals = BigDecimal.ZERO
    entries.forEach { item ->
        when (item.entry.type) {
            EntryType.DEBIT -> charges += item.entry.amount
            EntryType.CREDIT -> payments += item.entry.amount
            EntryType.WRITE_OFF -> writeOffs += item.entry.amount
            EntryType.REVERSAL -> reversals += item.entry.amount
        }
    }
    val net = charges - payments - writeOffs + reversals
    return StatementTotals(charges, payments, writeOffs, reversals, net)
}

internal fun signedAmount(entry: AccountEntryEntity): BigDecimal {
    return when (entry.type) {
        EntryType.DEBIT -> entry.amount
        EntryType.CREDIT -> entry.amount.negate()
        EntryType.WRITE_OFF -> entry.amount.negate()
        EntryType.REVERSAL -> entry.amount
    }
}

@Composable
internal fun entryTypeColor(type: EntryType) =
        when (type) {
            EntryType.DEBIT -> MaterialTheme.colorScheme.error
            EntryType.CREDIT -> MaterialTheme.colorScheme.primary
            EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
            EntryType.REVERSAL -> MaterialTheme.colorScheme.tertiary
        }

internal fun buildMonthOptions(anchorDate: LocalDate, count: Int = 12): List<MonthOption> {
    val results = mutableListOf<MonthOption>()
    var year = anchorDate.year
    var month = anchorDate.monthNumber
    repeat(count) {
        results.add(MonthOption(year, month, "${monthName(month)} $year"))
        val (newYear, newMonth) = shiftMonth(year, month, -1)
        year = newYear
        month = newMonth
    }
    return results
}

internal fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val total = (year * 12) + (month - 1) + delta
    val newYear = total.floorDiv(12)
    val newMonth = (total % 12) + 1
    return newYear to newMonth
}

internal fun parseDate(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}

internal fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

internal fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

internal fun monthName(month: Int): String {
    val index = month - 1
    return if (index in 0..11) {
        DateFormatSymbols.getInstance(Locale.getDefault()).months[index]
    } else {
        DateFormatSymbols.getInstance(Locale.getDefault()).months[0]
    }
}
