package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKesCompact
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@Composable
fun CalendarDayCell(day: CalendarDayUi, onClick: (LocalDate) -> Unit) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable { onClick(day.date) },
        tonalElevation =
            if (day.orderCount > 0 && day.isInCurrentMonth) {
                2.dp
            } else {
                0.dp
            },
        shape = MaterialTheme.shapes.medium,
        color =
            if (day.isInCurrentMonth) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (day.isInCurrentMonth) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                modifier = Modifier.align(Alignment.TopStart)
            )

            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (day.orderCount > 0) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${day.orderCount} orders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatKes(day.totalAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
