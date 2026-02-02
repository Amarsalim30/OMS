@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.order.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.domain.OrderLineItem
import com.zeynbakers.order_management_system.order.domain.aggregateLineItems
import com.zeynbakers.order_management_system.order.domain.aggregateOrderLineItems
import com.zeynbakers.order_management_system.order.domain.formatQuantity
import com.zeynbakers.order_management_system.order.domain.parseOrderNotes
import java.math.BigDecimal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock

private enum class SummaryRangeMode(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

private data class DateRange(val startInclusive: LocalDate, val endExclusive: LocalDate)

private data class OrderNoteAnalysis(
    val order: OrderEntity,
    val items: List<OrderLineItem>,
    val unparsedLines: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    monthLabel: String,
    monthTotal: BigDecimal,
    initialDate: LocalDate,
    orders: List<OrderEntity>,
    rangeTotal: BigDecimal,
    customerNames: Map<Long, String>,
    onAnchorDateChange: (LocalDate) -> Unit,
    onLoadRange: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(SummaryRangeMode.DAY) }
    var anchorDate by remember { mutableStateOf(initialDate) }
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    LaunchedEffect(initialDate) {
        anchorDate = initialDate
    }

    LaunchedEffect(anchorDate) {
        onAnchorDateChange(anchorDate)
    }

    val range = remember(mode, anchorDate) { rangeFor(mode = mode, anchorDate = anchorDate) }
    val rangeLabel = remember(mode, range, anchorDate) { formatRangeLabel(mode, range, anchorDate) }

    LaunchedEffect(range) {
        onLoadRange(range.startInclusive, range.endExclusive)
    }

    val analyses =
        remember(orders) {
            orders.map { order ->
                val parsed = parseOrderNotes(order.notes)
                OrderNoteAnalysis(order = order, items = parsed.items, unparsedLines = parsed.unparsed)
            }
        }
    val aggregatedItems = remember(analyses) { aggregateOrderLineItems(analyses.flatMap { it.items }) }
    val unparsedLines = remember(analyses) { analyses.flatMap { it.unparsedLines }.distinct() }
    val chefMessage = remember(rangeLabel, aggregatedItems, unparsedLines) {
        buildChefMessage(rangeLabel = rangeLabel, items = aggregatedItems, unparsedLines = unparsedLines)
    }

    if (isDatePickerOpen) {
        val initialMillis =
            remember(anchorDate) {
                anchorDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            anchorDate =
                                Instant.fromEpochMilliseconds(selectedMillis)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(text = "Summary", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(chefMessage))
                            Toast
                                .makeText(context, "Chef list copied", Toast.LENGTH_SHORT)
                                .show()
                        },
                        enabled = aggregatedItems.isNotEmpty() || unparsedLines.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy chef list")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                MonthTotalCard(monthTotal = monthTotal, label = monthLabel)
            }

            item {
                ChefPrepCard(
                    mode = mode,
                    rangeLabel = rangeLabel,
                    anchorLabel = "Anchor: $anchorDate",
                    orderCount = orders.size,
                    rangeTotal = rangeTotal,
                    hasChefList = aggregatedItems.isNotEmpty() || unparsedLines.isNotEmpty(),
                    onPickDate = { isDatePickerOpen = true },
                    onPrev = { anchorDate = shiftAnchorDate(mode, anchorDate, -1) },
                    onNext = { anchorDate = shiftAnchorDate(mode, anchorDate, 1) },
                    onToday = { anchorDate = today },
                    onModeChange = { mode = it }
                )
            }

            if (aggregatedItems.isNotEmpty()) {
                item {
                    SectionHeader(
                        title =
                            when (mode) {
                                SummaryRangeMode.DAY -> "Products"
                                SummaryRangeMode.WEEK -> "Products (week total)"
                                SummaryRangeMode.MONTH -> "Products (month total)"
                            }
                    )
                }
                items(aggregatedItems, key = { it.name }) { item ->
                    ProductRow(name = item.name, quantity = formatQuantity(item.quantity))
                }
            }

            if (unparsedLines.isNotEmpty()) {
                item {
                    UnparsedLinesCard(unparsedLines = unparsedLines)
                }
            }

            if (mode != SummaryRangeMode.DAY) {
                val analysesByDate = analyses.groupBy { it.order.orderDate }
                val datesAsc = analysesByDate.keys.sorted()

                item { SectionHeader(title = "Daily view") }
                if (datesAsc.isEmpty()) {
                    item { EmptyState(text = "No orders in this range.") }
                } else {
                    items(datesAsc, key = { it.toString() }) { date ->
                        val dayAnalyses = analysesByDate[date].orEmpty()
                        val dayTotal =
                            dayAnalyses.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.order.totalAmount }
                        val dayProducts = aggregateOrderLineItems(dayAnalyses.flatMap { it.items })
                        val dayUnparsed = dayAnalyses.flatMap { it.unparsedLines }.distinct()
                        val dayMessage =
                            buildChefMessage(
                                rangeLabel = date.toString(),
                                items = dayProducts,
                                unparsedLines = dayUnparsed
                            )
                        DailySummaryCard(
                            date = date.toString(),
                            orderCount = dayAnalyses.size,
                            total = dayTotal,
                            onOpenDay = {
                                mode = SummaryRangeMode.DAY
                                anchorDate = date
                            },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(dayMessage))
                            }
                        )
                    }
                }
            }

            item { SectionHeader(title = "Orders") }

            if (orders.isEmpty()) {
                item { EmptyState(text = "No orders in this range.") }
            } else {
                when (mode) {
                    SummaryRangeMode.DAY -> {
                        items(orders, key = { it.id }) { order ->
                            val customerLabel =
                                order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                                    ?: "Walk-in"
                            OrderSummaryCard(
                                customerLabel = customerLabel,
                                notes = order.notes,
                                total = order.totalAmount,
                                onCopyNotes = { clipboardManager.setText(AnnotatedString(order.notes)) }
                            )
                        }
                    }
                    SummaryRangeMode.WEEK, SummaryRangeMode.MONTH -> {
                        val ordersByDate = orders.groupBy { it.orderDate }
                        val datesDesc = ordersByDate.keys.sortedDescending()
                        datesDesc.forEach { date ->
                            item {
                                Text(
                                    text = date.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(ordersByDate[date].orEmpty(), key = { it.id }) { order ->
                                val customerLabel =
                                    order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                                        ?: "Walk-in"
                                OrderSummaryCard(
                                    customerLabel = customerLabel,
                                    notes = order.notes,
                                    total = order.totalAmount,
                                    onCopyNotes = { clipboardManager.setText(AnnotatedString(order.notes)) }
                                )
                            }
                        }
                    }
                }
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
    Card(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun MonthTotalCard(monthTotal: BigDecimal, label: String) {
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
                Text(text = "Current month total", style = MaterialTheme.typography.titleSmall)
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
private fun ChefPrepCard(
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
                    Text(text = "Chef prep", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (hasChefList) "Ready to copy/share" else "No product quantities found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPickDate) {
                    Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = "Pick date")
                }
            }

            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = SummaryRangeMode.entries.indexOf(mode)) {
                SummaryRangeMode.entries.forEach { entry ->
                    Tab(
                        selected = mode == entry,
                        onClick = { onModeChange(entry) },
                        text = { Text(entry.label) }
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onNext) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
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
                TextButton(onClick = onToday) { Text("Today") }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(label = "Orders", value = orderCount.toString())
                StatPill(label = "Total", value = formatKes(rangeTotal))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ProductRow(name: String, quantity: String) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(12.dp))
            Text(text = quantity, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun UnparsedLinesCard(unparsedLines: List<String>) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = "Unparsed lines (${unparsedLines.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            unparsedLines.forEach { line ->
                Text(text = "- $line", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    date: String,
    orderCount: Int,
    total: BigDecimal,
    onOpenDay: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenDay)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = date, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Orders: $orderCount - Total: ${formatKes(total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopy) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy day list")
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        )
    }
}

private fun buildChefMessage(
    rangeLabel: String,
    items: List<OrderLineItem>,
    unparsedLines: List<String>
): String {
    val lines = mutableListOf<String>()
    lines += "Chef prep - $rangeLabel"
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

private fun rangeFor(mode: SummaryRangeMode, anchorDate: LocalDate): DateRange {
    return when (mode) {
        SummaryRangeMode.DAY -> DateRange(anchorDate, anchorDate.plus(1, DateTimeUnit.DAY))
        SummaryRangeMode.WEEK -> {
            val offset = anchorDate.dayOfWeek.ordinal
            val start = anchorDate.plus(-offset, DateTimeUnit.DAY)
            DateRange(start, start.plus(7, DateTimeUnit.DAY))
        }
        SummaryRangeMode.MONTH -> {
            val start = LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
            DateRange(start, start.plus(1, DateTimeUnit.MONTH))
        }
    }
}

private fun shiftAnchorDate(mode: SummaryRangeMode, anchorDate: LocalDate, delta: Int): LocalDate {
    return when (mode) {
        SummaryRangeMode.DAY -> anchorDate.plus(delta, DateTimeUnit.DAY)
        SummaryRangeMode.WEEK -> anchorDate.plus(delta * 7, DateTimeUnit.DAY)
        SummaryRangeMode.MONTH -> anchorDate.plus(delta, DateTimeUnit.MONTH)
    }
}

private fun formatRangeLabel(mode: SummaryRangeMode, range: DateRange, anchorDate: LocalDate): String {
    return when (mode) {
        SummaryRangeMode.DAY -> anchorDate.toString()
        SummaryRangeMode.WEEK -> {
            val endInclusive = range.endExclusive.plus(-1, DateTimeUnit.DAY)
            "${range.startInclusive} to $endInclusive"
        }
        SummaryRangeMode.MONTH -> "${monthName(anchorDate.monthNumber)} ${anchorDate.year}"
    }
}

private fun monthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }
}

