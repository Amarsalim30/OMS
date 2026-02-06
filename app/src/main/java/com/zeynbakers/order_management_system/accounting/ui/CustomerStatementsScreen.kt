@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    kotlinx.coroutines.FlowPreview::class
)

package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import java.math.BigDecimal
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class StatementRangeType(val label: String) {
    All("All time"),
    Month("Month"),
    Custom("Custom")
}

private data class MonthOption(
    val year: Int,
    val month: Int,
    val label: String
)

private data class StatementEntryUi(
    val entry: AccountEntryEntity,
    val date: LocalDate,
    val runningBalance: BigDecimal
)

private data class StatementTotals(
    val charges: BigDecimal,
    val payments: BigDecimal,
    val writeOffs: BigDecimal,
    val reversals: BigDecimal,
    val net: BigDecimal
)

private data class StatementRange(
    val label: String,
    val start: LocalDate?,
    val end: LocalDate?,
    val isValid: Boolean
)

@Composable
fun CustomerStatementsScreen(
    customerViewModel: CustomerAccountsViewModel,
    ledgerViewModel: LedgerViewModel,
    initialCustomerId: Long?,
    onContextConsumed: () -> Unit,
    externalPadding: PaddingValues = PaddingValues(0.dp)
) {
    val summaries by customerViewModel.summaries.collectAsState()
    val customer by customerViewModel.customer.collectAsState()
    val ledger by customerViewModel.ledger.collectAsState()
    val orderLabels by customerViewModel.orderLabels.collectAsState()

    val today = rememberCurrentDate()
    val monthOptions = remember(today) { buildMonthOptions(today) }

    var queryText by rememberSaveable { mutableStateOf("") }
    var activeCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var rangeType by rememberSaveable { mutableStateOf(StatementRangeType.Month) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthNumber) }
    var selectedYear by rememberSaveable { mutableStateOf(today.year) }
    var customStart by rememberSaveable { mutableStateOf("") }
    var customEnd by rememberSaveable { mutableStateOf("") }
    var showAllLedger by rememberSaveable { mutableStateOf(false) }
    var showReversals by rememberSaveable { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialCustomerId) {
        if (initialCustomerId != null) {
            activeCustomerId = initialCustomerId
            onContextConsumed()
        }
    }

    LaunchedEffect(activeCustomerId) {
        val id = activeCustomerId ?: return@LaunchedEffect
        customerViewModel.loadCustomer(id)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { queryText }
            .debounce(300)
            .distinctUntilChanged()
            .collect { customerViewModel.searchCustomers(it) }
    }

    if (showAllLedger) {
        Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(externalPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "All customers ledger", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Advanced view",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showAllLedger = false }) { Text("Back") }
                }
                LedgerScreen(
                    viewModel = ledgerViewModel,
                    onBack = { showAllLedger = false },
                    showTopBar = false,
                    externalPadding = PaddingValues(0.dp)
                )
            }
        }
        return
    }

    val statementEntries = remember(ledger) { buildStatementEntries(ledger) }
    val range = remember(
        rangeType,
        selectedMonth,
        selectedYear,
        customStart,
        customEnd
    ) {
        buildStatementRange(
            rangeType = rangeType,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            customStart = customStart,
            customEnd = customEnd
        )
    }

    val filteredEntries by remember(statementEntries, range, showReversals) {
        derivedStateOf {
            if (!range.isValid) {
                emptyList()
            } else {
                statementEntries.filter { entry ->
                    val afterStart = range.start?.let { entry.date >= it } ?: true
                    val beforeEnd = range.end?.let { entry.date <= it } ?: true
                    val typeMatch = showReversals || entry.entry.type != EntryType.REVERSAL
                    afterStart && beforeEnd && typeMatch
                }
            }
        }
    }

    val openingBalance by remember(statementEntries, range) {
        derivedStateOf {
            val start = range.start ?: return@derivedStateOf BigDecimal.ZERO
            statementEntries.lastOrNull { it.date < start }?.runningBalance ?: BigDecimal.ZERO
        }
    }

    val totals by remember(filteredEntries) {
        derivedStateOf { buildTotals(filteredEntries) }
    }

    val closingBalance by remember(filteredEntries, openingBalance, range.isValid) {
        derivedStateOf {
            if (!range.isValid) {
                openingBalance
            } else {
                filteredEntries.lastOrNull()?.runningBalance ?: openingBalance
            }
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        if (activeCustomerId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(externalPadding)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Customer statements", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Pick a customer to view statement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("All customers ledger") },
                            onClick = {
                                showAllLedger = true
                                isMenuOpen = false
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Search customers") },
                    placeholder = { Text("Name or phone") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (queryText.isNotBlank()) {
                            IconButton(onClick = { queryText = "" }) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (queryText.isBlank()) "All customers" else "Search results",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${summaries.size} results",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (summaries.isEmpty()) {
                    EmptyState(text = if (queryText.isBlank()) "No customers yet" else "No customers found")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(summaries, key = { it.customerId }) { summary ->
                            StatementCustomerRow(
                                customer = summary,
                                onClick = { activeCustomerId = summary.customerId }
                            )
                        }
                    }
                }
            }
        } else {
            val customerName = customer?.name?.ifBlank { "Customer" } ?: "Customer"
            val phone = customer?.phone?.trim().orEmpty()
            val displayEntries = remember(filteredEntries) { filteredEntries.asReversed() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(externalPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    StatementHeader(
                        customerName = customerName,
                        phone = phone,
                        rangeLabel = range.label,
                        showReversals = showReversals,
                        onToggleReversals = { showReversals = !showReversals },
                        onChangeCustomer = { activeCustomerId = null },
                        onOpenAllLedger = { showAllLedger = true }
                    )
                }

                item {
                    RangeSelector(
                        rangeType = rangeType,
                        onRangeTypeChange = { rangeType = it },
                        monthOptions = monthOptions,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        onMonthSelected = { year, month ->
                            selectedYear = year
                            selectedMonth = month
                        },
                        customStart = customStart,
                        customEnd = customEnd,
                        onCustomStartChange = { customStart = it },
                        onCustomEndChange = { customEnd = it }
                    )
                }

                item {
                    StatementSummaryCard(
                        openingBalance = openingBalance,
                        charges = totals.charges,
                        payments = totals.payments,
                        writeOffs = totals.writeOffs,
                        reversals = totals.reversals,
                        showReversals = showReversals,
                        closingBalance = closingBalance
                    )
                }

                if (!range.isValid) {
                    item {
                        InfoCard(text = "Enter a valid custom range (YYYY-MM-DD).")
                    }
                } else if (displayEntries.isEmpty()) {
                    item { EmptyState(text = "No ledger entries in this range.") }
                } else {
                    items(displayEntries, key = { it.entry.id }) { entry ->
                        val orderLabel = entry.entry.orderId?.let { orderLabels[it] }
                        StatementEntryRow(entry = entry, orderLabel = orderLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementHeader(
    customerName: String,
    phone: String,
    rangeLabel: String,
    showReversals: Boolean,
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
                Text(text = "Statement", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onChangeCustomer) { Text("Change") }
            IconButton(onClick = { showMenu = true }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (showReversals) "Hide reversals" else "Show reversals") },
                    onClick = {
                        onToggleReversals()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("All customers ledger") },
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
private fun RangeSelector(
    rangeType: StatementRangeType,
    onRangeTypeChange: (StatementRangeType) -> Unit,
    monthOptions: List<MonthOption>,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int, Int) -> Unit,
    customStart: String,
    customEnd: String,
    onCustomStartChange: (String) -> Unit,
    onCustomEndChange: (String) -> Unit
) {
    var isMonthMenuOpen by remember { mutableStateOf(false) }
    val selectedLabel = monthOptions
        .firstOrNull { it.year == selectedYear && it.month == selectedMonth }
        ?.label ?: "${monthName(selectedMonth)} $selectedYear"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatementRangeType.entries.forEach { option ->
                FilterChip(
                    selected = rangeType == option,
                    onClick = { onRangeTypeChange(option) },
                    label = { Text(option.label) }
                )
            }
        }

        if (rangeType == StatementRangeType.Month) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMonthMenuOpen = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Month", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Select month")
            }
            DropdownMenu(expanded = isMonthMenuOpen, onDismissRequest = { isMonthMenuOpen = false }) {
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
                    label = { Text("Start date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customEnd,
                    onValueChange = onCustomEndChange,
                    label = { Text("End date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatementSummaryCard(
    openingBalance: BigDecimal,
    charges: BigDecimal,
    payments: BigDecimal,
    writeOffs: BigDecimal,
    reversals: BigDecimal,
    showReversals: Boolean,
    closingBalance: BigDecimal
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = "Statement summary", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            SummaryRow(label = "Opening balance", amount = formatKes(openingBalance))
            SummaryRow(label = "Charges (+)", amount = formatKes(charges))
            SummaryRow(label = "Payments (-)", amount = formatKes(payments))
            SummaryRow(label = "Write-offs (-)", amount = formatKes(writeOffs))
            if (showReversals) {
                SummaryRow(label = "Reversals", amount = formatKes(reversals))
            }
            Spacer(Modifier.height(4.dp))
            SummaryRow(label = "Closing balance", amount = formatKes(closingBalance))
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatementEntryRow(entry: StatementEntryUi, orderLabel: String?) {
    val typeLabel = entryTypeLabel(entry.entry.type, entry.entry.orderId)
    val amountColor = entryTypeColor(entry.entry.type)
    val sign = if (entry.entry.type == EntryType.DEBIT || entry.entry.type == EntryType.REVERSAL) "+" else "-"
    val balanceSign = if (entry.runningBalance < BigDecimal.ZERO) "-" else ""
    val balanceColor =
        when {
            entry.runningBalance > BigDecimal.ZERO -> MaterialTheme.colorScheme.error
            entry.runningBalance < BigDecimal.ZERO -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    
    val icon = when (entry.entry.type) {
        EntryType.DEBIT -> Icons.Filled.ShoppingBag
        EntryType.CREDIT -> Icons.Outlined.Payments
        EntryType.WRITE_OFF -> Icons.Filled.MoneyOff
        EntryType.REVERSAL -> Icons.Filled.Warning
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
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
                        text = "$sign ${formatKes(entry.entry.amount)}",
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
                         Text(text = formatDateTime(entry.entry.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         
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
                        text = "Bal $balanceSign${formatKes(entry.runningBalance.abs())}",
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
private fun StatementCustomerRow(
    customer: CustomerAccountSummary,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name.ifBlank { "Unknown customer" },
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
                    text = "Billed ${formatKes(customer.billed)} - Paid ${formatKes(customer.paid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            BalanceChip(balance = customer.balance)
        }
    }
}

@Composable
private fun BalanceChip(balance: BigDecimal) {
    val (label, color) =
        when {
            balance > BigDecimal.ZERO -> "Due" to MaterialTheme.colorScheme.errorContainer
            balance < BigDecimal.ZERO -> "Extra" to MaterialTheme.colorScheme.tertiaryContainer
            else -> "Clear" to MaterialTheme.colorScheme.secondaryContainer
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
            Text(text = formatKes(balance.abs()), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
    }
}

private fun buildStatementEntries(entries: List<AccountEntryEntity>): List<StatementEntryUi> {
    val sorted = entries.sortedWith(compareBy<AccountEntryEntity> { it.date }.thenBy { it.id })
    var running = BigDecimal.ZERO
    return sorted.map { entry ->
        running += signedAmount(entry)
        StatementEntryUi(
            entry = entry,
            date = Instant.fromEpochMilliseconds(entry.date)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date,
            runningBalance = running
        )
    }
}

private fun buildStatementRange(
    rangeType: StatementRangeType,
    selectedYear: Int,
    selectedMonth: Int,
    customStart: String,
    customEnd: String
): StatementRange {
    return when (rangeType) {
        StatementRangeType.All -> StatementRange("All time", null, null, true)
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
            val label = if (isValid) "$start to $end" else "Custom range"
            StatementRange(label, start, end, isValid)
        }
    }
}

private fun buildTotals(entries: List<StatementEntryUi>): StatementTotals {
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

private fun signedAmount(entry: AccountEntryEntity): BigDecimal {
    return when (entry.type) {
        EntryType.DEBIT -> entry.amount
        EntryType.CREDIT -> entry.amount.negate()
        EntryType.WRITE_OFF -> entry.amount.negate()
        EntryType.REVERSAL -> entry.amount
    }
}

private fun entryTypeLabel(type: EntryType, orderId: Long?): String {
    return when (type) {
        EntryType.DEBIT -> "Order" // Simplified from "Order charge"
        EntryType.CREDIT -> if (orderId == null) "Extra credit" else "Payment"
        EntryType.WRITE_OFF -> "Bad Debt / Write-off" // Clearer label
        EntryType.REVERSAL -> if (orderId == null) "Credit reversal" else "Payment reversal"
    }
}

@Composable
private fun entryTypeColor(type: EntryType) =
    when (type) {
        EntryType.DEBIT -> MaterialTheme.colorScheme.error
        EntryType.CREDIT -> MaterialTheme.colorScheme.primary
        EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
        EntryType.REVERSAL -> MaterialTheme.colorScheme.tertiary
    }

private fun buildMonthOptions(anchorDate: LocalDate, count: Int = 12): List<MonthOption> {
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

private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val total = (year * 12) + (month - 1) + delta
    val newYear = total.floorDiv(12)
    val newMonth = (total % 12) + 1
    return newYear to newMonth
}

private fun parseDate(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun monthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }
}
