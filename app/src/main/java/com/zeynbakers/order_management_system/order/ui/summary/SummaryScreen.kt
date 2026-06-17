@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.order.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.order.domain.OrderLineItem
import com.zeynbakers.order_management_system.order.domain.formatQuantity
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import com.zeynbakers.order_management_system.order.ui.models.toDisplaySummary
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class SummaryRangeMode(val labelRes: Int) {
    DAY(R.string.summary_range_day),
    WEEK(R.string.summary_range_week),
    MONTH(R.string.summary_range_month)
}

private data class DateRange(val startInclusive: LocalDate, val endExclusive: LocalDate)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    monthLabel: String,
    monthTotal: BigDecimal,
    initialDate: LocalDate,
    orders: List<OrderUiModel>,
    rangeTotal: BigDecimal,
    onAnchorDateChange: (LocalDate) -> Unit,
    onLoadRange: (LocalDate, LocalDate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
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
    val uiDateFormatter =
        remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val messageHeaderPrefix = stringResource(R.string.summary_message_header_prefix)
    val messageNoProducts = stringResource(R.string.summary_message_no_products)
    val chefListCopiedMessage = stringResource(R.string.summary_chef_list_copied)

    LaunchedEffect(range) {
        onLoadRange(range.startInclusive, range.endExclusive)
    }

    val aggregatedItems =
        remember(orders) {
            val allItems = orders.flatMap { it.items }
            val totalsByName = mutableMapOf<String, BigDecimal>()
            allItems.forEach { item ->
                val name = item.productNameSnapshot
                totalsByName[name] = (totalsByName[name] ?: BigDecimal.ZERO) + BigDecimal.valueOf(
                    item.quantity.toLong()
                )
            }
            totalsByName.entries.sortedBy { it.key }.map { (name, qty) ->
                OrderLineItem(name, qty)
            }
        }

    val chefMessage = remember(
        rangeLabel,
        aggregatedItems,
        messageHeaderPrefix,
        messageNoProducts
    ) {
        buildChefMessage(
            rangeLabel = rangeLabel,
            items = aggregatedItems,
            unparsedLines = emptyList(),
            headerPrefix = messageHeaderPrefix,
            noProductsFoundLabel = messageNoProducts,
            unparsedHeader = ""
        )
    }

    if (isDatePickerOpen) {
        val initialMillis =
            remember(anchorDate, isDatePickerOpen) {
                anchorDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
            }
        )
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
                TextButton(onClick = {
                    isDatePickerOpen = false
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(
                state = pickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .padding(horizontal = 8.dp)
            )

        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = stringResource(R.string.summary_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    val copyChefListLabel = stringResource(R.string.summary_copy_chef_list)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(chefMessage))
                            scope.launch { uiEvents.showSnackbar(chefListCopiedMessage) }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = copyChefListLabel },
                        enabled = aggregatedItems.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    anchorLabel = stringResource(
                        R.string.summary_anchor_label,
                        anchorDate.toJavaLocalDate().format(uiDateFormatter)
                    ),
                    orderCount = orders.size,
                    rangeTotal = rangeTotal,
                    hasChefList = aggregatedItems.isNotEmpty(),
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

            if (mode != SummaryRangeMode.DAY) {
                val ordersByDate = orders.groupBy { it.order.orderDate }
                val datesAsc = ordersByDate.keys.sorted()

                item { SectionHeader(title = stringResource(R.string.summary_daily_view)) }
                if (datesAsc.isEmpty()) {
                    item { SummaryEmptyState() }
                } else {
                    items(datesAsc, key = { it.toString() }) { date ->
                        val dayOrders = ordersByDate[date].orEmpty()
                        val dayTotal =
                            dayOrders.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.order.totalAmount }

                        val dayAggregatedItems = run {
                            val allItems = dayOrders.flatMap { it.items }
                            val totalsByName = mutableMapOf<String, BigDecimal>()
                            allItems.forEach { item ->
                                val name = item.productNameSnapshot
                                totalsByName[name] =
                                    (totalsByName[name] ?: BigDecimal.ZERO) + BigDecimal.valueOf(
                                        item.quantity.toLong()
                                    )
                            }
                            totalsByName.entries.sortedBy { it.key }.map { (name, qty) ->
                                OrderLineItem(name, qty)
                            }
                        }

                        val dayMessage =
                            buildChefMessage(
                                rangeLabel = date.toJavaLocalDate().format(uiDateFormatter),
                                items = dayAggregatedItems,
                                unparsedLines = emptyList(),
                                headerPrefix = messageHeaderPrefix,
                                noProductsFoundLabel = messageNoProducts,
                                unparsedHeader = ""
                            )
                        DailySummaryCard(
                            date = date,
                            orders = dayOrders.map { it.order },
                            orderItemsMap = dayOrders.associate { it.order.id to it.items },
                            customerNames = dayOrders.associate {
                                (it.order.customerId ?: 0L) to (it.customer?.name ?: "")
                            },
                            onCopyNotes = {
                                clipboardManager.setText(AnnotatedString(dayMessage))
                            }
                        )
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.summary_orders)) }

            if (orders.isEmpty()) {
                item { SummaryEmptyState() }
            } else {
                when (mode) {
                    SummaryRangeMode.DAY -> {
                        items(orders, key = { it.order.id }) { orderUi ->
                            val order = orderUi.order
                            val customerLabel = orderUi.customer?.name
                            val pickupDisplay =
                                com.zeynbakers.order_management_system.core.util.formatPickupTimeForDisplay(
                                    order.pickupTime
                                )
                            OrderSummaryCard(
                                customerLabel = customerLabel,
                                orderItems = orderUi.items,
                                total = order.totalAmount,
                                pickupTime = pickupDisplay,
                                onCopyNotes = {
                                    clipboardManager.setText(AnnotatedString(orderUi.items.toDisplaySummary()))
                                }
                            )
                        }
                    }

                    SummaryRangeMode.WEEK, SummaryRangeMode.MONTH -> {
                        val ordersByDate = orders.groupBy { it.order.orderDate }
                        val datesDesc = ordersByDate.keys.sortedDescending()
                        datesDesc.forEach { date ->
                            item {
                                Text(
                                    text = date.toJavaLocalDate().format(uiDateFormatter),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(ordersByDate[date].orEmpty(), key = { it.order.id }) { orderUi ->
                                val order = orderUi.order
                                val customerLabel = orderUi.customer?.name
                                val pickupDisplay =
                                    com.zeynbakers.order_management_system.core.util.formatPickupTimeForDisplay(
                                        order.pickupTime
                                    )
                                OrderSummaryCard(
                                    customerLabel = customerLabel,
                                    orderItems = orderUi.items,
                                    total = order.totalAmount,
                                    pickupTime = pickupDisplay,
                                    onCopyNotes = {
                                        clipboardManager.setText(AnnotatedString(orderUi.items.toDisplaySummary()))
                                    }
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

