@file:OptIn(
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class,
        kotlinx.coroutines.FlowPreview::class
)

package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import java.math.BigDecimal
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate

internal enum class StatementRangeType {
    All,
    Month,
    Custom
}

internal data class MonthOption(val year: Int, val month: Int, val label: String)

internal data class StatementEntryUi(
        val entry: AccountEntryEntity,
        val date: LocalDate,
        val runningBalance: BigDecimal
)

internal data class StatementTotals(
        val charges: BigDecimal,
        val payments: BigDecimal,
        val writeOffs: BigDecimal,
        val reversals: BigDecimal,
        val net: BigDecimal
)

internal data class StatementRange(
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
    var hideAmounts by rememberSaveable { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

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
        snapshotFlow { queryText }.debounce(300).distinctUntilChanged().collect {
            customerViewModel.searchCustomers(it)
        }
    }

    if (showAllLedger) {
        Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(externalPadding)) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(R.string.money_advanced_ledger_title),
                                style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                                text = stringResource(R.string.money_advanced_ledger_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showAllLedger = false }) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                        )
                    }
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
    val customRangeLabel = stringResource(R.string.money_custom_range)
    val allTimeLabel = stringResource(R.string.money_all_time)
    val rangeToSeparator = stringResource(R.string.money_to_separator)
    val dateInputPlaceholder = stringResource(R.string.money_date_placeholder)
    val range =
            remember(
                    rangeType,
                    selectedMonth,
                    selectedYear,
                    customStart,
                    customEnd,
                    customRangeLabel,
                    allTimeLabel,
                    rangeToSeparator
            ) {
                buildStatementRange(
                        rangeType = rangeType,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        customStart = customStart,
                        customEnd = customEnd,
                        customRangeLabel = customRangeLabel,
                        allTimeLabel = allTimeLabel,
                        rangeToSeparator = rangeToSeparator
                )
            }

    val filteredEntries by
            remember(statementEntries, range, showReversals) {
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

    val openingBalance by
            remember(statementEntries, range) {
                derivedStateOf {
                    val start = range.start ?: return@derivedStateOf BigDecimal.ZERO
                    statementEntries.lastOrNull { it.date < start }?.runningBalance
                            ?: BigDecimal.ZERO
                }
            }

    val totals by remember(filteredEntries) { derivedStateOf { buildTotals(filteredEntries) } }

    val closingBalance by
            remember(filteredEntries, openingBalance, range.isValid) {
                derivedStateOf {
                    if (!range.isValid) {
                        openingBalance
                    } else {
                        filteredEntries.lastOrNull()?.runningBalance ?: openingBalance
                    }
                }
            }

    Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (activeCustomerId == null) {
                    // Customer List Mode Top Bar
                    if (isSearchActive) {
                        Surface(
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp
                        ) {
                            TextField(
                                    value = queryText,
                                    onValueChange = { queryText = it },
                                    placeholder = {
                                        Text(stringResource(R.string.money_name_or_phone))
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                            TextFieldDefaults.colors(
                                                    focusedContainerColor =
                                                            MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor =
                                                            MaterialTheme.colorScheme.surface,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                            ),
                                    leadingIcon = {
                                        IconButton(
                                                onClick = {
                                                    isSearchActive = false
                                                    queryText = ""
                                                }
                                        ) {
                                            Icon(
                                                    imageVector =
                                                            Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription =
                                                            stringResource(R.string.action_back)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (queryText.isNotEmpty()) {
                                            IconButton(onClick = { queryText = "" }) {
                                                Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string.action_clear
                                                                )
                                                )
                                            }
                                        }
                                    }
                            )
                        }
                    } else {
                        CenterAlignedTopAppBar(
                                title = { Text(stringResource(R.string.money_statement_title)) },
                                actions = {
                                    IconButton(onClick = { hideAmounts = !hideAmounts }) {
                                        Icon(
                                                imageVector =
                                                        if (hideAmounts) Icons.Filled.VisibilityOff
                                                        else Icons.Filled.Visibility,
                                                contentDescription =
                                                        if (hideAmounts)
                                                                stringResource(
                                                                        R.string
                                                                                .action_show_balances
                                                                )
                                                        else
                                                                stringResource(
                                                                        R.string
                                                                                .action_hide_balances
                                                                )
                                        )
                                    }
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(
                                                imageVector = Icons.Filled.Search,
                                                contentDescription =
                                                        stringResource(R.string.action_search)
                                        )
                                    }
                                    IconButton(onClick = { isMenuOpen = true }) {
                                        Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription =
                                                        stringResource(R.string.action_more)
                                        )
                                    }
                                    DropdownMenu(
                                            expanded = isMenuOpen,
                                            onDismissRequest = { isMenuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                                text = {
                                                    Text(
                                                            stringResource(
                                                                    R.string
                                                                            .money_advanced_ledger_title
                                                            )
                                                    )
                                                },
                                                onClick = {
                                                    showAllLedger = true
                                                    isMenuOpen = false
                                                }
                                        )
                                    }
                                }
                        )
                    }
                } else {
                    // Statement Detail Mode Top Bar
                    CenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.money_statement_title)) },
                            navigationIcon = {
                                IconButton(onClick = { activeCustomerId = null }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { hideAmounts = !hideAmounts }) {
                                    Icon(
                                            imageVector =
                                                    if (hideAmounts) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                            contentDescription =
                                                    if (hideAmounts)
                                                            stringResource(
                                                                    R.string.action_show_balances
                                                            )
                                                    else
                                                            stringResource(
                                                                    R.string.action_hide_balances
                                                            )
                                    )
                                }
                                IconButton(onClick = { isMenuOpen = true }) {
                                    Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription =
                                                    stringResource(R.string.action_more)
                                    )
                                }
                                DropdownMenu(
                                        expanded = isMenuOpen,
                                        onDismissRequest = { isMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                            text = {
                                                Text(
                                                        if (showReversals)
                                                                stringResource(
                                                                        R.string
                                                                                .action_hide_reversals
                                                                )
                                                        else
                                                                stringResource(
                                                                        R.string
                                                                                .action_show_reversals
                                                                )
                                                )
                                            },
                                            onClick = {
                                                showReversals = !showReversals
                                                isMenuOpen = false
                                            }
                                    )
                                    DropdownMenuItem(
                                            text = {
                                                Text(
                                                        stringResource(
                                                                R.string.money_advanced_ledger_title
                                                        )
                                                )
                                            },
                                            onClick = {
                                                showAllLedger = true
                                                isMenuOpen = false
                                            }
                                    )
                                }
                            }
                    )
                }
            }
    ) { padding ->
        if (activeCustomerId == null) {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(padding)
                                    .padding(externalPadding)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text =
                                    if (queryText.isBlank())
                                            stringResource(R.string.money_all_customers)
                                    else stringResource(R.string.money_search_results),
                            style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                            text =
                                    pluralStringResource(
                                            id = R.plurals.money_result_count,
                                            count = summaries.size,
                                            summaries.size
                                    ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (summaries.isEmpty()) {
                    EmptyState(
                            text =
                                    if (queryText.isBlank()) {
                                        stringResource(R.string.money_no_customers_yet)
                                    } else {
                                        stringResource(R.string.money_no_customers_found)
                                    }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(summaries, key = { it.customerId }) { summary ->
                            StatementCustomerRow(
                                    modifier = Modifier.animateItem(),
                                    customer = summary,
                                    hideAmounts = hideAmounts,
                                    onClick = { activeCustomerId = summary.customerId }
                            )
                        }
                    }
                }
            }
        } else {
            val customerName =
                    customer?.name?.ifBlank { stringResource(R.string.money_customer) }
                            ?: stringResource(R.string.money_customer)
            val phone = customer?.phone?.trim().orEmpty()
            val displayEntries = remember(filteredEntries) { filteredEntries.asReversed() }

            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(externalPadding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                                text = customerName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                        )
                        if (phone.isNotBlank()) {
                            Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                            onCustomEndChange = { customEnd = it },
                            dateInputPlaceholder = dateInputPlaceholder
                    )
                }

                item {
                    StatementSummaryCard(
                            openingBalance = openingBalance,
                            charges = totals.charges,
                            payments = totals.payments,
                            writeOffs = totals.writeOffs,
                            reversals = totals.reversals,
                            hideAmounts = hideAmounts,
                            showReversals = showReversals,
                            closingBalance = closingBalance
                    )
                }

                if (!range.isValid) {
                    item { InfoCard(text = stringResource(R.string.money_invalid_custom_range)) }
                } else if (displayEntries.isEmpty()) {
                    item { EmptyState(text = stringResource(R.string.money_no_entries_in_range)) }
                } else {
                    items(displayEntries, key = { it.entry.id }) { entry ->
                        val orderLabel = entry.entry.orderId?.let { orderLabels[it] }
                        StatementEntryRow(
                                modifier = Modifier.animateItem(),
                                entry = entry,
                                hideAmounts = hideAmounts,
                                orderLabel = orderLabel
                        )
                    }
                }
            }
        }
    }
}
