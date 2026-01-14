package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.core.util.formatKes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search

@Composable
fun CustomerListScreen(
    query: String,
    summaries: List<CustomerAccountSummary>,
    onQueryChange: (String) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(text = "Customers", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Search and manage balances",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search customers") },
            placeholder = { Text("Name or phone") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (query.isBlank()) "All customers" else "Search results",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "${summaries.size} total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        if (summaries.isEmpty()) {
            Text(
                text = if (query.isBlank()) "No customers yet." else "No customers match your search.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
            ) {
                items(summaries, key = { it.customerId }) { customer ->
                    CustomerSummaryRow(
                        customer = customer,
                        onClick = { onCustomerClick(customer.customerId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerSummaryRow(customer: CustomerAccountSummary, onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = customer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = customer.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Billed", style = MaterialTheme.typography.labelSmall)
                    Text(text = formatKes(customer.billed), style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text(text = "Paid", style = MaterialTheme.typography.labelSmall)
                    Text(text = formatKes(customer.paid), style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text(text = "Balance", style = MaterialTheme.typography.labelSmall)
                    val balanceColor =
                        if (customer.balance > java.math.BigDecimal.ZERO) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    Text(
                        text = formatKes(customer.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = balanceColor
                    )
                }
            }
        }
    }
}
