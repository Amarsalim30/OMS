@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import kotlinx.datetime.toJavaLocalDate
import java.util.Locale
import com.zeynbakers.order_management_system.order.data.OrderEntity

@Composable
internal fun OrderSummaryCard(
    customerLabel: String?,
    notes: String,
    total: BigDecimal,
    pickupTime: String?,
    onCopyNotes: () -> Unit
) {
    val cartItems = remember(notes) { OrderCartParser.parseNotesToCart(notes) }
    
    AppCard {
        Column {
            // Display structured cart items with clear formatting
            cartItems.forEach { item ->
                val priceText = if (item.unitPrice > BigDecimal.ZERO) {
                    " (@ ${formatKes(item.unitPrice)})"
                } else {
                    ""
                }
                
                Text(
                    text = buildString {
                        if (item.emoji.isNotBlank()) {
                            append(item.emoji).append(" ")
                        }
                        append(item.name)
                        append(" × ")
                        append(item.quantity)
                        append(priceText)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (cartItems.isEmpty()) {
                // Fallback to raw notes if parsing fails
                Text(text = notes, style = MaterialTheme.typography.bodyLarge)
            }
        }
        
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                customerLabel?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                pickupTime?.let {
                    Text(
                        text = stringResource(R.string.day_pickup_time_value, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = formatKes(total), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onCopyNotes) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_copy))
            }
        }
    }
}

@Composable
internal fun MonthTotalCard(monthTotal: BigDecimal, label: String) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.summary_current_month_total),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatKes(monthTotal),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
internal fun ChefPrepCard(
    mode: SummaryRangeMode,
    rangeLabel: String,
    anchorLabel: String,
    orderCount: Int,
    rangeTotal: BigDecimal,
    hasChefList: Boolean,
    onPickDate: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onModeChange: (SummaryRangeMode) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.summary_chef_prep),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text =
                            if (hasChefList) {
                                stringResource(R.string.summary_ready_to_copy)
                            } else {
                                stringResource(R.string.summary_no_products_found)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPickDate) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = stringResource(R.string.summary_pick_date)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = SummaryRangeMode.entries.indexOf(mode)) {
                SummaryRangeMode.entries.forEach { entry ->
                    Tab(
                        selected = mode == entry,
                        onClick = { onModeChange(entry) },
                        text = { Text(stringResource(entry.labelRes)) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrev) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.summary_previous)
                        )
                    }
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.summary_next)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anchorLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onToday) { Text(stringResource(R.string.action_today)) }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
internal fun ProductRow(name: String, quantity: String) {
    Text(
        text = "$name × $quantity",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
internal fun UnparsedLinesCard(lines: List<String>) {
    AppCard {
        Column {
            Text(
                text = stringResource(R.string.summary_message_unparsed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lines.forEach { line ->
                Text(text = "- $line", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SummaryEmptyState() {
    AppEmptyState(
        title = "No Orders",
        body = "There are no orders in the selected range"
    )
}

@Composable
internal fun DailySummaryCard(
    date: kotlinx.datetime.LocalDate,
    orders: List<OrderEntity>,
    customerNames: Map<Long, String>,
    onCopyNotes: (String) -> Unit
) {
    val dateLabel = remember(date) {
        date.toJavaLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
    
    AppCard {
        Column {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            orders.forEach { order ->
                val customerLabel = order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                val pickupDisplay = com.zeynbakers.order_management_system.core.util.formatPickupTimeForDisplay(order.pickupTime)
                OrderSummaryCard(
                    customerLabel = customerLabel,
                    notes = order.notes ?: "",
                    total = order.totalAmount,
                    pickupTime = pickupDisplay,
                    onCopyNotes = { onCopyNotes(order.notes ?: "") }
                )
            }
        }
    }
}