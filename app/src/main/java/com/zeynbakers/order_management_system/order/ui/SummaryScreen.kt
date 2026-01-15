package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.domain.aggregateLineItems
import com.zeynbakers.order_management_system.order.domain.formatQuantity
import com.zeynbakers.order_management_system.order.domain.OrderLineItem
import com.zeynbakers.order_management_system.order.domain.parseOrderNotes
import java.math.BigDecimal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    monthLabel: String,
    monthTotal: BigDecimal,
    date: LocalDate,
    orders: List<OrderEntity>,
    dayTotal: BigDecimal,
    customerNames: Map<Long, String>,
    onLoadDate: (LocalDate) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isDatePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        onLoadDate(date)
    }

    val aggregatedItems = remember(orders) { aggregateLineItems(orders.map { it.notes }) }
    val unparsedLines =
        remember(orders) { orders.flatMap { parseOrderNotes(it.notes).unparsed }.distinct() }
    val chefMessage = remember(date, aggregatedItems, unparsedLines) {
        buildChefMessage(date = date, items = aggregatedItems, unparsedLines = unparsedLines)
    }

    if (isDatePickerOpen) {
        val initialMillis =
            remember(date) {
                date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate =
                                Instant.fromEpochMilliseconds(selectedMillis)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                            onDateChange(selectedDate)
                        }
                        isDatePickerOpen = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            TextButton(
                onClick = { clipboardManager.setText(AnnotatedString(chefMessage)) },
                enabled = aggregatedItems.isNotEmpty() || unparsedLines.isNotEmpty()
            ) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy chef list")
            }
        }

        Text(text = "Summary - $monthLabel", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(text = "Month total: ${formatKes(monthTotal)}", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Chef prep", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { isDatePickerOpen = true }) {
                        Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = "Pick date")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onDateChange(date.plus(-1, DateTimeUnit.DAY)) }) {
                        Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    Text(text = date.toString(), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onDateChange(date.plus(1, DateTimeUnit.DAY)) }) {
                        Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Orders: ${orders.size} | Total: ${formatKes(dayTotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))
                if (aggregatedItems.isEmpty()) {
                    Text(
                        text = "No product quantities found in notes for this day.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    aggregatedItems.forEachIndexed { idx, item ->
                        if (idx > 0) Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatQuantity(item.quantity),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                if (unparsedLines.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(text = "Unparsed lines", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    unparsedLines.forEach { line ->
                        Text(text = "- $line", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(text = "Orders", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (orders.isEmpty()) {
            Text(text = "No orders for this day.", style = MaterialTheme.typography.bodyMedium)
        } else {
            orders.forEach { order ->
                val customerLabel =
                    order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                        ?: "Walk-in"
                OrderSummaryCard(
                    customerLabel = customerLabel,
                    notes = order.notes,
                    total = order.totalAmount,
                    onCopyNotes = {
                        clipboardManager.setText(AnnotatedString(order.notes))
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    customerLabel: String,
    notes: String,
    total: BigDecimal,
    onCopyNotes: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = notes, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customerLabel, style = MaterialTheme.typography.bodyMedium)
                    Text(text = formatKes(total), style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onCopyNotes) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        }
    }
}

private fun buildChefMessage(
    date: LocalDate,
    items: List<OrderLineItem>,
    unparsedLines: List<String>
): String {
    val lines = mutableListOf<String>()
    lines += "Chef prep - $date"
    if (items.isEmpty()) {
        lines += "No product quantities found in order notes."
    } else {
        lines += ""
        lines += items.map { "${it.name}: ${formatQuantity(it.quantity)}" }
    }
    if (unparsedLines.isNotEmpty()) {
        lines += ""
        lines += "Unparsed lines:"
        lines += unparsedLines.map { "- $it" }
    }
    return lines.joinToString("\n")
}
