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
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes

private enum class LedgerFilter(val label: String) {
    All("All"),
    Charges("Charges"),
    Payments("Payments"),
    Adjustments("Adjustments")
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
                    title = { Text("Ledger") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                LedgerSummaryCard(summary = summary)
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
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyLedgerState() }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LedgerSummaryCard(summary: LedgerSummaryUi?) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = "Ledger snapshot", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (summary == null) {
                Text(
                    text = "Loading totals...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SummaryRow(label = "Charges", amount = formatKes(summary.totalDebits))
                SummaryRow(label = "Payments", amount = formatKes(summary.totalCredits))
                SummaryRow(label = "Write-offs", amount = formatKes(summary.totalWriteOffs))
                SummaryRow(label = "Reversals", amount = formatKes(summary.totalReversals))
                Spacer(Modifier.height(4.dp))
                SummaryRow(label = "Net balance", amount = formatKes(summary.netBalance))
            }
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
private fun LedgerEntryCard(entry: LedgerEntryUi) {
    val typeLabel = entryTypeLabel(entry.type)
    val amountColor = entryTypeColor(entry.type)
    val sign = if (entry.type == EntryType.DEBIT || entry.type == EntryType.REVERSAL) "+" else "-"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    text = "$sign ${formatKes(entry.amount)}",
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
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "No ledger entries yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        )
    }
}

private fun entryTypeLabel(type: EntryType): String {
    return when (type) {
        EntryType.DEBIT -> "Order charge"
        EntryType.CREDIT -> "Payment"
        EntryType.WRITE_OFF -> "Write-off"
        EntryType.REVERSAL -> "Payment reversal"
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
