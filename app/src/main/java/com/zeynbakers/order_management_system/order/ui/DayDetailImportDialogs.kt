package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.order.data.OrderEntity
import kotlinx.datetime.LocalDate

@Composable
internal fun DayImportPreviewDialog(
    isOpen: Boolean,
    importData: com.zeynbakers.order_management_system.order.data.OrderExportData?,
    existingOrders: List<OrderEntity>,
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirmImport: (List<OrderImportAction>) -> Unit
) {
    if (!isOpen || importData == null) return

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val importDate = runCatching { kotlinx.datetime.LocalDate.parse(importData.orderDate) }.getOrNull()
    val isCrossDayImport = importDate != null && importDate != currentDate
    
    val importActions = remember(importData.orders, existingOrders) {
        importData.orders.map { importItem ->
            val duplicate = existingOrders.find { existing ->
                existing.notes == importItem.notes &&
                existing.totalAmount.toString() == importItem.totalAmount &&
                (importItem.customerPhone == null || 
                 (existing.customerId != null && importItem.customerPhone == existing.customerId.toString()))
            }
            OrderImportAction(
                importItem = importItem,
                duplicateOrderId = duplicate?.id,
                action = if (duplicate != null) ImportAction.MERGE else ImportAction.CREATE,
                customerAction = if (importItem.customerPhone != null) CustomerImportAction.MATCH else CustomerImportAction.CREATE
            )
        }
    }
    
    var selectedActions by remember { mutableStateOf(importActions) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (isCrossDayImport) {
                    stringResource(R.string.day_import_preview_cross_day)
                } else {
                    stringResource(R.string.day_import_preview_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCrossDayImport) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.day_import_date_adjustment_required),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.day_import_date_adjustment_message, importDate, currentDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Text(
                    text = stringResource(R.string.day_import_orders_count, importData.orders.size),
                    style = MaterialTheme.typography.labelLarge
                )
                
                selectedActions.forEachIndexed { index, action ->
                    ImportOrderItem(
                        action = action,
                        onActionChange = { newAction ->
                            selectedActions = selectedActions.toMutableList().apply {
                                set(index, action.copy(action = newAction))
                            }
                        },
                        onCustomerActionChange = { newCustomerAction ->
                            selectedActions = selectedActions.toMutableList().apply {
                                set(index, action.copy(customerAction = newCustomerAction))
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmImport(selectedActions)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.day_import_confirm, selectedActions.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ImportOrderItem(
    action: OrderImportAction,
    onActionChange: (ImportAction) -> Unit,
    onCustomerActionChange: (CustomerImportAction) -> Unit
) {
    val item = action.importItem
    val isDuplicate = action.duplicateOrderId != null
    
    Surface(
        color = if (isDuplicate) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.totalAmount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            if (item.customerName != null || item.customerPhone != null) {
                Text(
                    text = "${item.customerName ?: ""} ${item.customerPhone ?: ""}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.pickupTime != null) {
                Text(
                    text = stringResource(R.string.day_import_pickup_label, item.pickupTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.cartItems.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = stringResource(R.string.day_import_items_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.cartItems.take(3).forEach { cartItem ->
                        Text(
                            text = "• ${cartItem.name} x${cartItem.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.cartItems.size > 3) {
                        Text(
                            text = stringResource(R.string.day_import_more_items, item.cartItems.size - 3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (isDuplicate) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = action.action == ImportAction.MERGE,
                        onClick = { onActionChange(ImportAction.MERGE) },
                        label = { Text(stringResource(R.string.day_import_merge)) }
                    )
                    FilterChip(
                        selected = action.action == ImportAction.CREATE,
                        onClick = { onActionChange(ImportAction.CREATE) },
                        label = { Text(stringResource(R.string.day_import_keep_separate)) }
                    )
                }
            }
            
            if (item.customerPhone != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = action.customerAction == CustomerImportAction.MATCH,
                        onClick = { onCustomerActionChange(CustomerImportAction.MATCH) },
                        label = { Text(stringResource(R.string.day_import_match_customer)) }
                    )
                    FilterChip(
                        selected = action.customerAction == CustomerImportAction.CREATE,
                        onClick = { onCustomerActionChange(CustomerImportAction.CREATE) },
                        label = { Text(stringResource(R.string.day_import_create_customer)) }
                    )
                }
            }
        }
    }
}

data class OrderImportAction(
    val importItem: com.zeynbakers.order_management_system.order.data.OrderExportItem,
    val duplicateOrderId: Long?,
    val action: ImportAction,
    val customerAction: CustomerImportAction
)

enum class ImportAction {
    MERGE,
    CREATE
}

enum class CustomerImportAction {
    MATCH,
    CREATE
}
