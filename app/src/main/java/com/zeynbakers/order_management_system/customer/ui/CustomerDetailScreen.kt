package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.util.formatDateTime
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal

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

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("Back") }

        Text(text = customer?.name ?: "Customer", style = MaterialTheme.typography.titleLarge)
        customer?.phone?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(8.dp))
        Text(text = "Balance: ${formatKes(balance)}", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))
        Text(text = "Record payment", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                amountError = null
            },
            label = { Text("Amount (KES)") },
            modifier = Modifier.fillMaxWidth()
        )

        amountError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { selectedMethod = PaymentMethod.CASH }) {
                Text(text = if (selectedMethod == PaymentMethod.CASH) "CASH ✓" else "CASH")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { selectedMethod = PaymentMethod.MPESA }) {
                Text(text = if (selectedMethod == PaymentMethod.MPESA) "MPESA ✓" else "MPESA")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Text(text = "Apply to order (optional)", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { selectedOrderId = null }) {
            Text(text = if (selectedOrderId == null) "No order ✓" else "No order")
        }
        orders.take(10).forEach { order ->
            TextButton(onClick = { selectedOrderId = order.id }) {
                val label = "Order #${order.id} • ${formatKes(order.totalAmount)}"
                Text(text = if (selectedOrderId == order.id) "$label ✓" else label)
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
            }
        ) {
            Text("Save payment")
        }

        Spacer(Modifier.height(16.dp))
        Text(text = "Ledger", style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun LedgerRow(entry: AccountEntryEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = formatDateTime(entry.date), style = MaterialTheme.typography.labelMedium)
        Text(text = entry.description, style = MaterialTheme.typography.bodyMedium)
        val sign = if (entry.type == EntryType.DEBIT) "+" else "-"
        Text(text = "$sign ${formatKes(entry.amount)}", style = MaterialTheme.typography.bodyMedium)
    }
}
