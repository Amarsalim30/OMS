package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import kotlinx.datetime.LocalDate

@Composable
fun CalendarDayCell(day: CalendarDayUi, onClick: (LocalDate) -> Unit) {
    Box(
            modifier =
                    Modifier.aspectRatio(1f)
                            .padding(4.dp)
                            .clickable { onClick(day.date) }
                            .background(
                                    color =
                                            if (day.orderCount > 0)
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.15f
                                                    )
                                            else Color.Transparent,
                                    shape = CircleShape
                            ),
            contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = day.date.dayOfMonth.toString())

            if (day.isToday) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }

            if (day.orderCount > 0) {
                Text(
                        text = "${day.orderCount} | ${formatKes(day.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
