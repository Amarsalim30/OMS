package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
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
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppSpacing
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes

@Composable
fun MpesaTransactionRow(
    item: MpesaTransactionUi,
    onToggleSelected: (Boolean) -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDuplicate = item.duplicateState != DuplicateState.NONE
    val canApply = item.canApply()
    val canSelect = !isDuplicate && canApply
    val timeLabel = item.receivedAt?.let { formatDateTime(it) }
    val codeLabel = item.transactionCode?.let { stringResource(R.string.money_code_value, it) }
        ?: stringResource(R.string.money_no_code)
    val amountLabel = formatKes(item.amount)
    val sender = item.senderName?.takeIf { it.isNotBlank() }
        ?: item.senderPhone?.takeIf { it.isNotBlank() }
    val senderLabel = sender?.let { stringResource(R.string.money_sender_from, it) }

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
            else -> ""
        }
        !canApply -> stringResource(R.string.money_status_needs_match)
        item.selected -> stringResource(R.string.money_status_selected)
        else -> stringResource(R.string.money_status_ready)
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

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.money_existing_receipt)
            ) { onOpenDetails() }
            .semantics { contentDescription = rowCd }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // UX Fix: Moved selection Checkbox to the left (start) of the row
            Checkbox(
                checked = item.selected,
                onCheckedChange = { checked -> onToggleSelected(checked) },
                enabled = canSelect,
                modifier = Modifier.semantics { contentDescription = checkboxCd }
            )

            Spacer(modifier = Modifier.width(AppSpacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = amountLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

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
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (senderLabel != null) {
                    Text(
                        text = senderLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val metadataParts = listOfNotNull(codeLabel, timeLabel)
                if (metadataParts.isNotEmpty()) {
                    Text(
                        text = metadataParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
