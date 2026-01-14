package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
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
    var notes by rememberSaveable { mutableStateOf("") }
    var totalText by rememberSaveable { mutableStateOf("") }
    var customerName by rememberSaveable { mutableStateOf("") }
    var customerPhone by rememberSaveable { mutableStateOf("") }
    var editingOrderId by remember { mutableStateOf<Long?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var notesError by remember { mutableStateOf<String?>(null) }
    var totalError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("en-KE")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

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
        val trimmedTotal = totalText.trim()
        val parsedTotal = trimmedTotal.toBigDecimalOrNull()
        val currentTotal = parsedTotal ?: BigDecimal.ZERO
        val statusText = if (paidAmount >= currentTotal && currentTotal > BigDecimal.ZERO) "Paid" else "Unpaid"
        val formattedTotal = parsedTotal?.let { formatter.format(it) }
        val isTotalInvalid = trimmedTotal.isNotEmpty() && parsedTotal == null
        val hasCustomerMismatch = (customerName.isBlank() xor customerPhone.isBlank())
        val canSave =
            notes.trim().isNotEmpty() && parsedTotal != null && parsedTotal > BigDecimal.ZERO && !hasCustomerMismatch

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isEditorOpen = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
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
                    placeholder = { Text("Customer details, delivery time, etc.") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                notesError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (filtered.count { ch -> ch == '.' } <= 1) {
                            totalText = filtered
                            totalError = null
                        }
                    },
                    label = { Text("Total amount") },
                    placeholder = { Text("KSh 0.00") },
                    leadingIcon = { Text("KSh") },
                    isError = isTotalInvalid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = {
                        when {
                            isTotalInvalid -> Text("Enter a valid amount.")
                            formattedTotal != null -> Text("Total: KSh $formattedTotal")
                        }
                    },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
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
                            val finalTotal =
                                trimmedTotal.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)

                            when {
                                trimmedNotes.isEmpty() -> {
                                    notesError = "Notes are required"
                                    totalError = null
                                    customerError = null
                                }
                                finalTotal == null || finalTotal <= BigDecimal.ZERO -> {
                                    notesError = null
                                    totalError = "Enter a valid total"
                                    customerError = null
                                }
                                hasCustomerMismatch -> {
                                    notesError = null
                                    totalError = null
                                    customerError = "Enter both name and phone, or leave both blank"
                                }
                                else -> {
                                    onSaveOrder(
                                        trimmedNotes,
                                        finalTotal,
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
                        ,
                        enabled = canSave
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
