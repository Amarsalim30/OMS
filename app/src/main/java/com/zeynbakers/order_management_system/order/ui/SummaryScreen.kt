package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal

@Composable
fun SummaryScreen(
    monthLabel: String,
    monthTotal: BigDecimal,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("Back") }

        Text(text = "Summary - $monthLabel", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(text = "Month total: ${formatKes(monthTotal)}", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))
        Text(text = "Totals by customer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = "Open the Customers tab for balances", style = MaterialTheme.typography.bodyMedium)
    }
}
