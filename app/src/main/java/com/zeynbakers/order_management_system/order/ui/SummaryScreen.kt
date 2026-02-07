@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.order.ui

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.domain.OrderLineItem
import com.zeynbakers.order_management_system.order.domain.aggregateLineItems
import com.zeynbakers.order_management_system.order.domain.aggregateOrderLineItems
import com.zeynbakers.order_management_system.order.domain.formatQuantity
import com.zeynbakers.order_management_system.order.domain.parseOrderNotes
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch

internal enum class SummaryRangeMode(val labelRes: Int) {
    DAY(R.string.summary_range_day),
    WEEK(R.string.summary_range_week),
    MONTH(R.string.summary_range_month)
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
    val uiEvents = LocalUiEventDispatcher.current
    val scope = rememberCoroutineScope()
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(SummaryRangeMode.DAY) }
    var anchorDate by remember { mutableStateOf(initialDate) }
    val today = rememberCurrentDate()

    LaunchedEffect(initialDate) {
        anchorDate = initialDate
    }

    LaunchedEffect(anchorDate) {
        onAnchorDateChange(anchorDate)
    }

    val range = remember(mode, anchorDate) { rangeFor(mode = mode, anchorDate = anchorDate) }
    val weekRangeFormat = stringResource(R.string.summary_range_week_label)
    val rangeLabel =
        remember(mode, range, anchorDate, weekRangeFormat) {
            formatRangeLabel(mode, range, anchorDate, weekRangeFormat)
        }
    val uiDateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val messageHeaderPrefix = stringResource(R.string.summary_message_header_prefix)
    val messageNoProducts = stringResource(R.string.summary_message_no_products)
    val messageUnparsed = stringResource(R.string.summary_message_unparsed)
    val chefListCopiedMessage = stringResource(R.string.summary_chef_list_copied)

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
    val chefMessage = remember(
        rangeLabel,
        aggregatedItems,
        unparsedLines,
        messageHeaderPrefix,
        messageNoProducts,
        messageUnparsed
    ) {
        buildChefMessage(
            rangeLabel = rangeLabel,
            items = aggregatedItems,
            unparsedLines = unparsedLines,
            headerPrefix = messageHeaderPrefix,
            noProductsFoundLabel = messageNoProducts,
            unparsedHeader = messageUnparsed
        )
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
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) { Text(stringResource(R.string.action_cancel)) }
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
                        Text(text = stringResource(R.string.summary_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(chefMessage))
                            scope.launch { uiEvents.showSnackbar(chefListCopiedMessage) }
                        },
                        enabled = aggregatedItems.isNotEmpty() || unparsedLines.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.summary_copy_chef_list))
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
                    anchorLabel = stringResource(R.string.summary_anchor_label, anchorDate.toJavaLocalDate().format(uiDateFormatter)),
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
                                SummaryRangeMode.DAY -> stringResource(R.string.summary_products)
                                SummaryRangeMode.WEEK -> stringResource(R.string.summary_products_week)
                                SummaryRangeMode.MONTH -> stringResource(R.string.summary_products_month)
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

                item { SectionHeader(title = stringResource(R.string.summary_daily_view)) }
                if (datesAsc.isEmpty()) {
                    item { SummaryEmptyState(text = stringResource(R.string.summary_no_orders_in_range)) }
                } else {
                    items(datesAsc, key = { it.toString() }) { date ->
                        val dayAnalyses = analysesByDate[date].orEmpty()
                        val dayTotal =
                            dayAnalyses.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.order.totalAmount }
                        val dayProducts = aggregateOrderLineItems(dayAnalyses.flatMap { it.items })
                        val dayUnparsed = dayAnalyses.flatMap { it.unparsedLines }.distinct()
                        val dayMessage =
                            buildChefMessage(
                                rangeLabel = date.toJavaLocalDate().format(uiDateFormatter),
                                items = dayProducts,
                                unparsedLines = dayUnparsed,
                                headerPrefix = messageHeaderPrefix,
                                noProductsFoundLabel = messageNoProducts,
                                unparsedHeader = messageUnparsed
                            )
                        DailySummaryCard(
                            date = date.toJavaLocalDate().format(uiDateFormatter),
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

            item { SectionHeader(title = stringResource(R.string.summary_orders)) }

            if (orders.isEmpty()) {
                item { SummaryEmptyState(text = stringResource(R.string.summary_no_orders_in_range)) }
            } else {
                when (mode) {
                    SummaryRangeMode.DAY -> {
                        items(orders, key = { it.id }) { order ->
                            val customerLabel =
                                order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.summary_walk_in)
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
                                    text = date.toJavaLocalDate().format(uiDateFormatter),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(ordersByDate[date].orEmpty(), key = { it.id }) { order ->
                                val customerLabel =
                                    order.customerId?.let { id -> customerNames[id] }?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.summary_walk_in)
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

private fun buildChefMessage(
    rangeLabel: String,
    items: List<OrderLineItem>,
    unparsedLines: List<String>,
    headerPrefix: String,
    noProductsFoundLabel: String,
    unparsedHeader: String
): String {
    val lines = mutableListOf<String>()
    lines += "$headerPrefix $rangeLabel"
    if (items.isEmpty()) {
        lines += noProductsFoundLabel
    } else {
        lines += ""
        lines += items.map { "${it.name}: ${formatQuantity(it.quantity)}" }
    }
    if (unparsedLines.isNotEmpty()) {
        lines += ""
        lines += unparsedHeader
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

private fun formatRangeLabel(
    mode: SummaryRangeMode,
    range: DateRange,
    anchorDate: LocalDate,
    weekRangeFormat: String
): String {
    val locale = Locale.getDefault()
    val dayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    return when (mode) {
        SummaryRangeMode.DAY -> anchorDate.toJavaLocalDate().format(dayFormatter)
        SummaryRangeMode.WEEK -> {
            val endInclusive = range.endExclusive.plus(-1, DateTimeUnit.DAY)
            weekRangeFormat.format(
                range.startInclusive.toJavaLocalDate().format(dayFormatter),
                endInclusive.toJavaLocalDate().format(dayFormatter)
            )
        }
        SummaryRangeMode.MONTH -> anchorDate.toJavaLocalDate().format(monthFormatter)
    }
}

