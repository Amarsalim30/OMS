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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes

private enum class LedgerFilter(val labelRes: Int) {
    All(R.string.ledger_filter_all),
    Charges(R.string.ledger_filter_charges),
    Payments(R.string.ledger_filter_payments),
    Adjustments(R.string.ledger_filter_adjustments)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
    externalPadding: PaddingValues = PaddingValues(0.dp)
) {
    val entries by viewModel.entries.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var filter by remember { mutableStateOf(LedgerFilter.All) }
    var hideAmounts by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val filtered =
        remember(entries, filter) {
            entries.filter { entry ->
                when (filter) {
                    LedgerFilter.All -> true
                    LedgerFilter.Charges -> entry.type == EntryType.DEBIT
                    LedgerFilter.Payments -> entry.type == EntryType.CREDIT || entry.type == EntryType.REVERSAL
                    LedgerFilter.Adjustments -> entry.type == EntryType.WRITE_OFF
                }
            }
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.money_ledger_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { hideAmounts = !hideAmounts }) {
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
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(externalPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LedgerSummaryCard(summary = summary, hideAmounts = hideAmounts)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedgerFilter.values().forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(stringResource(option.labelRes)) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyLedgerState() }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry, hideAmounts = hideAmounts)
                }
            }
        }
    }
}

@Composable
private fun LedgerSummaryCard(summary: LedgerSummaryUi?, hideAmounts: Boolean) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = stringResource(R.string.ledger_snapshot), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (summary == null) {
                Text(
                    text = stringResource(R.string.ledger_loading_totals),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LedgerSummaryRow(label = stringResource(R.string.ledger_filter_charges), amount = formatKes(summary.totalDebits), hideAmounts = hideAmounts)
                LedgerSummaryRow(label = stringResource(R.string.ledger_filter_payments), amount = formatKes(summary.totalCredits), hideAmounts = hideAmounts)
                LedgerSummaryRow(label = stringResource(R.string.ledger_write_offs), amount = formatKes(summary.totalWriteOffs), hideAmounts = hideAmounts)
                LedgerSummaryRow(label = stringResource(R.string.ledger_reversals), amount = formatKes(summary.totalReversals), hideAmounts = hideAmounts)
                Spacer(Modifier.height(4.dp))
                LedgerSummaryRow(label = stringResource(R.string.ledger_net_balance), amount = formatKes(summary.netBalance), hideAmounts = hideAmounts)
            }
        }
    }
}

@Composable
private fun LedgerSummaryRow(label: String, amount: String, hideAmounts: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun LedgerEntryCard(entry: LedgerEntryUi, hideAmounts: Boolean) {
    val typeLabel = entryTypeLabel(entry.type)
    val amountColor = ledgerEntryTypeColor(entry.type)
    val sign = if (entry.type == EntryType.DEBIT || entry.type == EntryType.REVERSAL) "+" else "-"
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = typeLabel, style = MaterialTheme.typography.labelLarge, color = amountColor)
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text =
                        if (hideAmounts) {
                            stringResource(R.string.money_amount_hidden)
                        } else {
                            "$sign ${formatKes(entry.amount)}"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
            }
            entry.orderLabel?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entry.customerLabel?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = formatDateTime(entry.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyLedgerState() {
    AppEmptyState(
        title = stringResource(R.string.money_ledger_title),
        body = stringResource(R.string.ledger_no_entries)
    )
}

@Composable
private fun entryTypeLabel(type: EntryType): String {
    return when (type) {
        EntryType.DEBIT -> stringResource(R.string.ledger_entry_order_charge)
        EntryType.CREDIT -> stringResource(R.string.ledger_entry_payment)
        EntryType.WRITE_OFF -> stringResource(R.string.ledger_entry_write_off)
        EntryType.REVERSAL -> stringResource(R.string.ledger_entry_payment_reversal)
    }
}

@Composable
private fun ledgerEntryTypeColor(type: EntryType) =
    when (type) {
        EntryType.DEBIT -> MaterialTheme.colorScheme.error
        EntryType.CREDIT -> MaterialTheme.colorScheme.primary
        EntryType.WRITE_OFF -> MaterialTheme.colorScheme.secondary
        EntryType.REVERSAL -> MaterialTheme.colorScheme.tertiary
    }
