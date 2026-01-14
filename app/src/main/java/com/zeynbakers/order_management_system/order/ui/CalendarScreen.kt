package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(
    days: List<CalendarDayUi>,
    monthLabel: String,
    monthTotal: java.math.BigDecimal,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSummary: () -> Unit,
    onCustomers: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Text("<")
            }

            Text(
                text = monthLabel,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onNextMonth) {
                Text(">")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Month total: ${formatKes(monthTotal)}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSummary) {
                Text(text = "Summary")
            }
            TextButton(onClick = onCustomers) {
                Text(text = "Customers")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.padding(8.dp)
        ) {
            items(days) { day ->
                CalendarDayCell(day, onDateClick)
            }
        }
    }
}
