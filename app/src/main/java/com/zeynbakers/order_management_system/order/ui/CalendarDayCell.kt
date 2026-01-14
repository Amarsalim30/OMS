package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import kotlinx.datetime.LocalDate

@Composable
fun CalendarDayCell(day: CalendarDayUi, onClick: (LocalDate) -> Unit) {
    val dayTextColor =
        if (day.isInCurrentMonth) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    val hasOrders = day.orderCount > 0
    val statusDotColor =
        when (day.paymentState) {
            PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
            PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
            PaymentState.UNPAID -> MaterialTheme.colorScheme.error
            PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
            null -> MaterialTheme.colorScheme.outline
        }
    val containerColor =
        if (!hasOrders) {
            MaterialTheme.colorScheme.surface
        } else {
            when (day.paymentState) {
                PaymentState.PAID -> MaterialTheme.colorScheme.tertiaryContainer
                PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
                PaymentState.UNPAID -> MaterialTheme.colorScheme.errorContainer
                PaymentState.OVERPAID -> MaterialTheme.colorScheme.primaryContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .alpha(if (day.isInCurrentMonth) 1f else 0.35f)
            .clickable(enabled = day.isInCurrentMonth) { onClick(day.date) },
        tonalElevation =
            if (hasOrders && day.isInCurrentMonth) {
                2.dp
            } else {
                0.dp
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = dayTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (day.isToday || hasOrders) {
                        Surface(
                            color =
                                if (day.isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    statusDotColor
                                },
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (hasOrders) formatKes(day.totalAmount) else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text =
                        if (hasOrders) {
                            if (day.orderCount == 1) "1 order" else "${day.orderCount} orders"
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
