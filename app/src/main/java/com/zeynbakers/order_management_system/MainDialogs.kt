package com.zeynbakers.order_management_system

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.ui.OrderCreditPrompt

@Composable
internal fun CreditPromptDialog(
    prompt: OrderCreditPrompt,
    onDismiss: () -> Unit,
    onApplyCredit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.credit_prompt_title)) },
        text = {
            Text(
                stringResource(
                    R.string.credit_prompt_body,
                    formatKes(prompt.availableCredit),
                    prompt.orderLabel
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onApplyCredit) {
                Text(stringResource(R.string.credit_prompt_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.credit_prompt_skip))
            }
        }
    )
}

@Composable
internal fun WhatsNewDialog(
    notes: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title)) },
        text = {
            Column {
                notes.forEach { note ->
                    Text(text = "- $note")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.whats_new_action))
            }
        }
    )
}
