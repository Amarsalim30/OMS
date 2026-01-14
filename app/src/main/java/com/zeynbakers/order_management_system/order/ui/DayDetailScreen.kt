package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    orders: List<OrderEntity>,
    dayTotal: BigDecimal,
    customerNames: Map<Long, String>,
    orderPaidAmounts: Map<Long, BigDecimal>,
    onBack: () -> Unit,
    onSaveOrder: (String, BigDecimal, String, String, Long?) -> Unit,
    loadCustomerById: suspend (Long) -> CustomerEntity?,
    searchCustomers: suspend (String) -> List<CustomerEntity>
) {
    var notes by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var editingOrderId by remember { mutableStateOf<Long?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }

    LaunchedEffect(editingOrderId) {
        if (editingOrderId == null) {
            customerName = ""
            customerPhone = ""
            return@LaunchedEffect
        }
        val order = orders.firstOrNull { it.id == editingOrderId } ?: return@LaunchedEffect
        val customerId = order.customerId ?: return@LaunchedEffect
        val customer = loadCustomerById(customerId) ?: return@LaunchedEffect
        customerName = customer.name
        customerPhone = customer.phone
    }

    LaunchedEffect(customerName, customerPhone) {
        val query = when {
            customerName.isNotBlank() -> customerName
            customerPhone.isNotBlank() -> customerPhone
            else -> ""
        }
        suggestions = if (query.isBlank()) emptyList() else searchCustomers(query)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingOrderId = null
                notes = ""
                totalText = ""
                customerName = ""
                customerPhone = ""
                notesError = null
                totalError = null
                customerError = null
                isEditorOpen = true
            }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            DaySummaryHeader(date = date, dayTotal = dayTotal, onBack = onBack)
            LazyColumn(modifier = Modifier.padding(top = 88.dp)) {
                item {
                    Spacer(Modifier.height(8.dp))
                }

                if (orders.isEmpty()) {
                    item {
                        Text(
                            text = "No orders yet",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(orders) { order ->
                        val customerLabel =
                            order.customerId?.let { customerNames[it] } ?: "No customer"
                        val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
                        val isPaid = paidAmount >= order.totalAmount
                        OrderListItem(
                            order = order,
                            customerLabel = customerLabel,
                            isPaid = isPaid,
                            onEdit = {
                                notes = order.notes
                                totalText = order.totalAmount.toPlainString()
                                editingOrderId = order.id
                                notesError = null
                                totalError = null
                                customerError = null
                                isEditorOpen = true
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    if (isEditorOpen) {
        val paidAmount = editingOrderId?.let { orderPaidAmounts[it] } ?: BigDecimal.ZERO
        val currentTotal = totalText.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val statusText = if (paidAmount >= currentTotal && currentTotal > BigDecimal.ZERO) "Paid" else "Unpaid"

        ModalBottomSheet(onDismissRequest = { isEditorOpen = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (editingOrderId == null) "New Order" else "Edit Order",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        notesError = null
                    },
                    label = { Text("Notes (required)") },
                    modifier = Modifier.fillMaxWidth()
                )

                notesError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalText,
                    onValueChange = {
                        totalText = it
                        totalError = null
                    },
                    label = { Text("Total amount (KES)") },
                    modifier = Modifier.fillMaxWidth()
                )

                totalError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(4.dp))
                Text(text = "Status: $statusText", style = MaterialTheme.typography.labelLarge)

                Spacer(Modifier.height(8.dp))
                Text(text = "Customer (optional)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        customerError = null
                    },
                    label = { Text("Customer name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                        customerError = null
                    },
                    label = { Text("Customer phone") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = "Suggestions", style = MaterialTheme.typography.labelLarge)
                    suggestions.forEach { customer ->
                        TextButton(
                            onClick = {
                                customerName = customer.name
                                customerPhone = customer.phone
                                suggestions = emptyList()
                            }
                        ) {
                            Text("${customer.name} - ${customer.phone}")
                        }
                    }
                }

                customerError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = {
                            notes = ""
                            totalText = ""
                            customerName = ""
                            customerPhone = ""
                            editingOrderId = null
                            notesError = null
                            totalError = null
                            customerError = null
                        }
                    ) {
                        Text("Clear")
                    }

                    Button(
                        onClick = {
                            val trimmedNotes = notes.trim()
                            val parsedTotal = totalText.trim().takeIf { it.isNotEmpty() }?.let {
                                runCatching { BigDecimal(it) }.getOrNull()
                            }

                            when {
                                trimmedNotes.isEmpty() -> {
                                    notesError = "Notes are required"
                                    totalError = null
                                    customerError = null
                                }
                                parsedTotal == null || parsedTotal <= BigDecimal.ZERO -> {
                                    notesError = null
                                    totalError = "Enter a valid total"
                                    customerError = null
                                }
                                (customerName.isBlank() xor customerPhone.isBlank()) -> {
                                    notesError = null
                                    totalError = null
                                    customerError = "Enter both name and phone, or leave both blank"
                                }
                                else -> {
                                    onSaveOrder(
                                        trimmedNotes,
                                        parsedTotal,
                                        customerName.trim(),
                                        customerPhone.trim(),
                                        editingOrderId
                                    )
                                    notes = ""
                                    totalText = ""
                                    customerName = ""
                                    customerPhone = ""
                                    editingOrderId = null
                                    notesError = null
                                    totalError = null
                                    customerError = null
                                    isEditorOpen = false
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun OrderListItem(
    order: OrderEntity,
    customerLabel: String,
    isPaid: Boolean,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = order.notes, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(text = customerLabel, style = MaterialTheme.typography.bodyMedium)
            Text(text = formatKes(order.totalAmount), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (isPaid) "Paid" else "Unpaid",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DaySummaryHeader(
    date: LocalDate,
    dayTotal: BigDecimal,
    onBack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Column {
                Text(text = "Date: $date", style = MaterialTheme.typography.titleMedium)
                Text(text = "Total: ${formatKes(dayTotal)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
