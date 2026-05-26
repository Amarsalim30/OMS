package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R

@Composable
internal fun OrderCartSummary(
    notes: String,
    onNotesChange: (String) -> Unit,
    onAddProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartItems = remember(notes) { OrderCartParser.parseNotesToCart(notes) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cartItems.isNotEmpty()) {
            Text(
                text = stringResource(R.string.order_editor_cart_section_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            cartItems.forEachIndexed { index, item ->
                val label =
                    buildString {
                        if (item.emoji.isNotBlank()) append("${item.emoji} ")
                        append("${item.name} x ${item.quantity}")
                    }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = OrderEditorFieldShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val updated = cartItems.toMutableList().apply { removeAt(index) }
                                onNotesChange(OrderCartParser.serializeCartToNotes(updated))
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.day_delete_order)
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAddProductClick,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            shape = OrderEditorFieldShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(R.string.order_editor_add_product_trigger))
        }
    }
}
