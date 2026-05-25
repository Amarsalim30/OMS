package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
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
    val codeLabel = item.transactionCode?.let { stringResource(R.string.money_code_value, it) }
        ?: stringResource(R.string.money_no_code)
    val amountLabel = formatKes(item.amount)
    val lineOne = listOfNotNull(amountLabel, codeLabel).joinToString(" • ")
    val sender = item.senderName?.takeIf { it.isNotBlank() }
        ?: item.senderPhone?.takeIf { it.isNotBlank() }
    val senderLabel = sender?.let { stringResource(R.string.money_sender_from, it) }
    val lineTwo = listOfNotNull(senderLabel, timeLabel).joinToString(" • ")

    val statusLabel = when {
        isDuplicate -> when (item.duplicateState) {
            DuplicateState.EXISTING -> when (item.existingReceiptStatus) {
                PaymentReceiptStatus.VOIDED -> stringResource(R.string.money_status_already_recorded_voided)
                PaymentReceiptStatus.UNAPPLIED -> stringResource(R.string.money_status_already_recorded_unused)
                PaymentReceiptStatus.PARTIAL -> stringResource(R.string.money_status_already_recorded_partial)
                PaymentReceiptStatus.APPLIED -> stringResource(R.string.money_status_already_recorded_used)
                null -> stringResource(R.string.money_status_already_recorded)
            }
            DuplicateState.INTAKE -> stringResource(R.string.money_status_duplicate)
            DuplicateState.NONE -> ""
        }
        !canApply -> stringResource(R.string.money_status_needs_match)
        item.selected -> stringResource(R.string.money_status_selected)
        else -> stringResource(R.string.money_status_ready)
    }

    val statusIcon = when {
        isDuplicate -> Icons.Filled.ContentCopy
        !canApply -> Icons.Filled.Warning
        item.selected -> Icons.Filled.CheckCircle
        else -> Icons.Filled.RadioButtonUnchecked
    }

    val statusColors = when {
        isDuplicate -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        !canApply -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        item.selected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    // Compose a meaningful content description for TalkBack
    val rowCd = stringResource(
        R.string.money_transaction_row_cd,
        amountLabel,
        sender ?: stringResource(R.string.money_sender_unknown),
        statusLabel
    )
    val checkboxCd = stringResource(R.string.money_transaction_checkbox_cd, amountLabel)

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.money_existing_receipt)
            ) { onOpenDetails() }
            .semantics { contentDescription = rowCd }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null, // label already announces the status
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            Checkbox(
                checked = item.selected,
                onCheckedChange = { checked -> onToggleSelected(checked) },
                enabled = canSelect,
                modifier = Modifier.semantics { contentDescription = checkboxCd }
            )
        }
    }
}
