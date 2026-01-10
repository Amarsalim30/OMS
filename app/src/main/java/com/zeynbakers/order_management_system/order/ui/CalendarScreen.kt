package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@Composable
fun CalendarScreen(
    days: List<CalendarDayUi>,
    onDateClick: (LocalDate) -> Unit
) {
    Column {
        Text(
            text = "Orders",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

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
