package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.util.formatKes

@Composable
fun CustomerListScreen(
    query: String,
    summaries: List<CustomerAccountSummary>,
    onQueryChange: (String) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(text = "Customers", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search by name or phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (summaries.isEmpty()) {
            Text(text = "No customers found", style = MaterialTheme.typography.bodyMedium)
        } else {
            summaries.forEach { customer ->
                CustomerSummaryRow(customer = customer, onClick = {
                    onCustomerClick(customer.customerId)
                })
            }
        }
    }
}

@Composable
private fun CustomerSummaryRow(customer: CustomerAccountSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(text = customer.name, style = MaterialTheme.typography.bodyLarge)
        Text(text = customer.phone, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(text = "Billed: ${formatKes(customer.billed)}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Paid: ${formatKes(customer.paid)}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Balance: ${formatKes(customer.balance)}", style = MaterialTheme.typography.bodyMedium)
    }
}
