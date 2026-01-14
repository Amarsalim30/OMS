package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity?,
    ledger: List<AccountEntryEntity>,
    balance: BigDecimal,
    orders: List<OrderEntity>,
    onBack: () -> Unit,
    onRecordPayment: (BigDecimal, PaymentMethod, String, Long?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedOrderId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = customer?.name?.ifBlank { "Customer" } ?: "Customer",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Balance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Balance", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = formatKes(balance), style = MaterialTheme.typography.titleLarge)
                    customer?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Phone",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = "Record payment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Record payment", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                            amountError = null
                        },
                        label = { Text("Amount (KES)") },
                        placeholder = { Text("KES 0.00") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    amountError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PaymentMethodToggle(
                            label = "Cash",
                            selected = selectedMethod == PaymentMethod.CASH,
                            onClick = { selectedMethod = PaymentMethod.CASH }
                        )
                        PaymentMethodToggle(
                            label = "Mpesa",
                            selected = selectedMethod == PaymentMethod.MPESA,
                            onClick = { selectedMethod = PaymentMethod.MPESA }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (optional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(text = "Apply to order (optional)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { selectedOrderId = null }) {
                        val label = if (selectedOrderId == null) "No order (selected)" else "No order"
                        Text(text = label)
                    }
                    orders.take(10).forEach { order ->
                        TextButton(onClick = { selectedOrderId = order.id }) {
                            val label = "Order #${order.id} - ${formatKes(order.totalAmount)}"
                            val selectedLabel = "$label (selected)"
                            Text(text = if (selectedOrderId == order.id) selectedLabel else label)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val parsedAmount = amountText.trim().takeIf { it.isNotEmpty() }?.let {
                                runCatching { BigDecimal(it) }.getOrNull()
                            }
                            if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
                                amountError = "Enter a valid amount"
                            } else {
                                onRecordPayment(parsedAmount, selectedMethod, noteText, selectedOrderId)
                                amountText = ""
                                noteText = ""
                                amountError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save payment")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = "Ledger",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Ledger", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))

                    if (ledger.isEmpty()) {
                        Text(text = "No ledger entries", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        ledger.forEach { entry ->
                            LedgerRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(entry: AccountEntryEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = formatDateTime(entry.date), style = MaterialTheme.typography.labelMedium)
        Text(text = entry.description, style = MaterialTheme.typography.bodyMedium)
        val sign = if (entry.type == EntryType.DEBIT) "+" else "-"
        Text(text = "$sign ${formatKes(entry.amount)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PaymentMethodToggle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val text = if (selected) "$label (selected)" else label
    TextButton(onClick = onClick, modifier = Modifier.height(48.dp)) {
        Text(text = text)
    }
}
