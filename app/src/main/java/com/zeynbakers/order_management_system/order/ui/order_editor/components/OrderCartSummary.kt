package com.zeynbakers.order_management_system.order.ui.order_editor.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.ui.common.OrderItemDraft
import java.math.BigDecimal

@Composable
internal fun OrderCartSummary(
    cartItems: List<OrderItemDraft>,
    onCartItemsChange: (List<OrderItemDraft>) -> Unit,
    onAddProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (cartItems.isNotEmpty()) {
            Text(
                text = stringResource(R.string.order_editor_cart_section_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            cartItems.forEachIndexed { index, item ->
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
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = buildString {
                                if (item.emoji.isNotBlank()) append("${item.emoji} ")
                                append(item.name)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.unitPrice > BigDecimal.ZERO) {
                            Text(
                                text = "@ ${formatKes(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "x${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatKes(item.lineTotal),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                val updated = cartItems.toMutableList().apply { removeAt(index) }
                                onCartItemsChange(updated)
                            },
                            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
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
