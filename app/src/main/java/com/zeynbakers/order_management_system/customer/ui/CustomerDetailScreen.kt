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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.Intent

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
                        text = customer?.name?.ifBlank { stringResource(R.string.money_customer) }
                            ?: stringResource(R.string.money_customer),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { customer?.id?.let { onOpenStatement(it) } },
                        enabled = customer != null
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.customer_detail_statement)
                        )
                    }
                    IconButton(
                        onClick = { customer?.id?.let { onPaymentHistory(it) } },
                        enabled = customer != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = stringResource(R.string.customer_detail_receipt_history)
                        )
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
                    Text(
                        text = stringResource(R.string.customer_detail_no_orders),
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    Text(
                        text = stringResource(R.string.ledger_no_entries),
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                            Text(stringResource(R.string.customer_detail_load_older_months))
                        }
                    }
                }
            }
        }
    }
}

