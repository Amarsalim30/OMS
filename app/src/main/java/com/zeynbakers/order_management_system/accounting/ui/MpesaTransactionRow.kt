package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes

@Composable
fun MpesaTransactionRow(
    item: MpesaTransactionUi,
    onToggleSelected: (Boolean) -> Unit,
    onOpenDetails: () -> Unit
) {
    val isDuplicate = item.duplicateState != DuplicateState.NONE
    val canApply = item.canApply()
    val canSelect = !isDuplicate && canApply
    val timeLabel = item.receivedAt?.let { formatDateTime(it) }
    val codeLabel = item.transactionCode?.let { "Code $it" } ?: "No code"
    val lineOne = listOfNotNull(formatKes(item.amount), codeLabel).joinToString(" • ")
    val sender =
        item.senderName?.takeIf { it.isNotBlank() } ?: item.senderPhone?.takeIf { it.isNotBlank() }
    val lineTwo = listOfNotNull(sender?.let { "From $it" }, timeLabel).joinToString(" • ")
    val statusLabel =
        when {
            isDuplicate ->
                when (item.duplicateState) {
                    DuplicateState.EXISTING -> {
                        when (item.existingReceiptStatus) {
                            PaymentReceiptStatus.VOIDED -> "Already recorded (voided)"
                            PaymentReceiptStatus.UNAPPLIED -> "Already recorded (not used)"
                            PaymentReceiptStatus.PARTIAL -> "Already recorded (part used)"
                            PaymentReceiptStatus.APPLIED -> "Already recorded (used)"
                            null -> "Already recorded"
                        }
                    }
                    DuplicateState.INTAKE -> "Duplicate"
                    DuplicateState.NONE -> ""
                }
            !canApply -> "Needs match"
            item.selected -> "Selected"
            else -> "Ready"
        }
    val statusColors =
        when {
            isDuplicate -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            !canApply -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            item.selected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lineOne,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lineTwo.isNotBlank()) {
                    Text(
                        text = lineTwo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (statusLabel.isNotBlank()) {
                Surface(
                    color = statusColors.first,
                    contentColor = statusColors.second,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            Checkbox(
                checked = item.selected,
                onCheckedChange = { checked -> onToggleSelected(checked) },
                enabled = canSelect
            )
        }
    }
}
