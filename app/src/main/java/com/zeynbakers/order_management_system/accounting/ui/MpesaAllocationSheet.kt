package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaAllocationSheet(
    item: MpesaTransactionUi,
    searchCustomers: suspend (String) -> List<CustomerSuggestionUi>,
    onSelectCustomer: (Long?) -> Unit,
    onSelectOrder: (Long?) -> Unit,
    onSelectAllocationMode: (AllocationMode) -> Unit,
    onViewExisting: () -> Unit,
    onMoveExisting: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customerQuery by remember(item.key) { mutableStateOf("") }
    var customerSuggestions by remember(item.key) { mutableStateOf(emptyList<CustomerSuggestionUi>()) }
    var editingCustomer by remember(item.key) { mutableStateOf(item.selectedCustomerId == null) }

    LaunchedEffect(item.key, item.selectedCustomerId) {
        if (item.selectedCustomerId == null) {
            editingCustomer = true
        }
    }

    LaunchedEffect(customerQuery) {
        val query = customerQuery.trim()
        customerSuggestions =
            if (query.isBlank()) {
                emptyList()
            } else {
                searchCustomers(query)
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatKes(item.amount),
                    style = MaterialTheme.typography.titleLarge
                )
                val codeLabel = item.transactionCode?.let { stringResource(R.string.money_code_value, it) }
                    ?: stringResource(R.string.money_no_code)
                val timeLabel = item.receivedAt?.let { formatDateTime(it) }
                val meta = listOfNotNull(codeLabel, timeLabel).joinToString(" • ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.money_customer), style = MaterialTheme.typography.titleSmall)
                if (item.selectedCustomerId != null && !editingCustomer) {
                    val name = item.suggestedCustomerName ?: stringResource(R.string.money_customer)
                    TextButton(onClick = { editingCustomer = true }) {
                        Text(text = name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onSelectCustomer(null)
                                editingCustomer = true
                                customerQuery = ""
                            }
                        ) { Text(stringResource(R.string.action_clear)) }
                    }
                } else {
                    OutlinedTextField(
                        value = customerQuery,
                        onValueChange = { customerQuery = it },
                        label = { Text(stringResource(R.string.money_search_name_or_phone)) },
                        placeholder = { Text(stringResource(R.string.money_customer_name_or_number)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (customerSuggestions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            customerSuggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        onSelectCustomer(suggestion.id)
                                        customerQuery = ""
                                        customerSuggestions = emptyList()
                                        editingCustomer = false
                                    }
                                ) {
                                    val label = "${suggestion.name} - ${suggestion.phone}"
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.money_allocation), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = item.allocationMode == AllocationMode.OLDEST_ORDERS,
                        onClick = { onSelectAllocationMode(AllocationMode.OLDEST_ORDERS) },
                        label = { Text(stringResource(R.string.money_oldest_orders)) },
                        enabled = item.selectedCustomerId != null
                    )
                    FilterChip(
                        selected = item.allocationMode == AllocationMode.CUSTOMER_CREDIT,
                        onClick = { onSelectAllocationMode(AllocationMode.CUSTOMER_CREDIT) },
                        label = { Text(stringResource(R.string.money_customer_credit)) },
                        enabled = item.selectedCustomerId != null
                    )
                }

                if (item.orderSuggestions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.money_pick_order),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val selectedOrder =
                        item.orderSuggestions.firstOrNull { it.orderId == item.selectedOrderId }
                    if (selectedOrder != null) {
                        Text(
                            text = stringResource(
                                R.string.money_selected_label,
                                suggestionLabel(selectedOrder, 22)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(item.orderSuggestions, key = { it.orderId }) { suggestion ->
                            val label = suggestionLabel(suggestion, 22)
                            val outstanding = formatKes(suggestion.outstanding)
                            TextButton(
                                onClick = { onSelectOrder(suggestion.orderId) }
                            ) {
                                val rowLabel = stringResource(R.string.money_due_amount, label, outstanding)
                                Text(rowLabel)
                            }
                        }
                    }
                } else if (item.selectedCustomerId != null) {
                    Text(
                        text = stringResource(R.string.money_no_open_orders),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.canApply()) {
                    Text(
                        text = stringResource(R.string.money_needs_customer_match),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (item.duplicateState == DuplicateState.EXISTING) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stringResource(R.string.money_existing_receipt), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onViewExisting) {
                            Text(stringResource(R.string.money_view_receipt))
                        }
                        Button(
                            onClick = onMoveExisting,
                            enabled = item.canApply()
                        ) {
                            Text(stringResource(R.string.action_move))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
