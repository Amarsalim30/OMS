package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CalendarTopAppBar(
    monthTitle: String,
    todayDay: Int,
    onMonthPickerClick: () -> Unit,
    onToday: () -> Unit,
    onSummaryClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            TextButton(
                onClick = onMonthPickerClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.calendar_select_month)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onSummaryClick) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = stringResource(R.string.calendar_summary)
                )
            }
        },
        actions = {
            IconButton(onClick = onToday) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = stringResource(R.string.calendar_today),
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = todayDay.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 5.dp)
                    )
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.action_more)
                )
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MonthPickerSheet(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = Locale.getDefault()
    var pickerYear by remember { mutableStateOf(initialYear) }
    val monthList = remember { Month.values().toList() }
    val monthLabels = remember(locale) { DateFormatSymbols(locale).shortMonths }

    LaunchedEffect(initialYear) {
        pickerYear = initialYear
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { pickerYear -= 1 }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.calendar_previous_year)
                    )
                }
                Text(
                    text = pickerYear.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { pickerYear += 1 }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.calendar_next_year)
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(monthList) { month ->
                    val monthNumber = month.ordinal + 1
                    val isSelected = pickerYear == initialYear && monthNumber == initialMonth
                    val label = monthLabels.getOrNull(monthNumber - 1)?.takeIf { it.isNotBlank() }
                        ?: month.name.lowercase().replaceFirstChar { it.uppercase() }
                    Surface(
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { onMonthSelected(pickerYear, monthNumber) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MonthSummaryCard(
    ownerTitle: String? = null,
    ownerSubtitle: String? = null,
    ownerHighlight: String? = null,
    monthTotal: BigDecimal?,
    dueCount: Int,
    hasOrders: Boolean,
    onDueClick: () -> Unit,
    onAddOrder: () -> Unit,
    onLegendClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (!ownerTitle.isNullOrBlank() || !ownerSubtitle.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ownerTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ownerSubtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!ownerHighlight.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = ownerHighlight,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.calendar_month_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalLabel = monthTotal?.let { formatKes(it) } ?: stringResource(R.string.calendar_loading)
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasOrders) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_no_orders),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    TextButton(onClick = onAddOrder) {
                        Text(stringResource(R.string.calendar_add_order))
                    }
                } else if (dueCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.clickable(role = Role.Button) { onDueClick() }
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_unpaid_count, dueCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                IconButton(onClick = onLegendClick) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.calendar_payment_status_info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LegendInfoSheet(
    weekStart: Int,
    systemWeekStart: Int,
    onDismiss: () -> Unit,
    onWeekStartChange: (Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_payment_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatusLegendRow()
            Text(
                text = stringResource(R.string.calendar_unpaid_includes_partial),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.calendar_tip_tap_day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.calendar_week_starts_on),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = weekStart == systemWeekStart,
                    onClick = { onWeekStartChange(null) },
                    label = { Text(stringResource(R.string.calendar_week_start_system)) }
                )
                FilterChip(
                    selected = weekStart == Calendar.MONDAY,
                    onClick = { onWeekStartChange(Calendar.MONDAY) },
                    label = { Text(stringResource(R.string.calendar_week_start_mon)) }
                )
                FilterChip(
                    selected = weekStart == Calendar.SUNDAY,
                    onClick = { onWeekStartChange(Calendar.SUNDAY) },
                    label = { Text(stringResource(R.string.calendar_week_start_sun)) }
                )
            }
        }
    }
}

@Composable
private fun StatusLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(label = stringResource(R.string.day_status_unpaid), state = PaymentState.UNPAID)
        LegendItem(label = stringResource(R.string.day_status_partial), state = PaymentState.PARTIAL)
        LegendItem(label = stringResource(R.string.day_status_paid), state = PaymentState.PAID)
        LegendItem(label = stringResource(R.string.day_status_overpaid), state = PaymentState.OVERPAID)
    }
}

@Composable
private fun LegendItem(label: String, state: PaymentState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = calendarPaymentStateColor(state),
            shape = CircleShape,
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun WeekdayHeaderRow(weekStart: Int) {
    val locale = Locale.getDefault()
    val labels = remember(locale, weekStart) {
        val dayLabels = DateFormatSymbols(locale).shortWeekdays
        val dayOrder =
            (0 until 7).map { offset ->
                ((weekStart - 1 + offset) % 7) + 1
            }
        dayOrder.map { dayLabels[it] }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val dayIndex = ((weekStart - 1 + index) % 7) + 1
            val color =
                when (dayIndex) {
                    Calendar.SATURDAY -> MaterialTheme.colorScheme.primary
                    Calendar.SUNDAY -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun MonthGrid(
    days: List<CalendarDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit,
    onDayBoundsChanged: ((LocalDate, Rect) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val verticalPadding = 4.dp
    val horizontalPadding = 10.dp
    val spacing = 3.dp
    val weeks = remember(days) { days.chunked(7) }
    BoxWithConstraints(modifier = modifier) {
        val rows = (days.size / 7).coerceAtLeast(1)
        val availableHeight = maxHeight - (verticalPadding * 2) - (spacing * (rows - 1))
        val sixRowHeight = (maxHeight - (verticalPadding * 2) - (spacing * 5)) / 6
        val cellHeight = (availableHeight / rows)
            .coerceAtMost(sixRowHeight)
            .coerceAtLeast(48.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            isSelected = selectedDate == day.date,
                            onSelectDate = onSelectDate,
                            onOpenDay = onOpenDay,
                            onQuickAdd = onQuickAdd,
                            onBoundsChanged = { bounds -> onDayBoundsChanged?.invoke(day.date, bounds) },
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .heightIn(min = 48.dp)
                        )
                    }
                    repeat((7 - week.size).coerceAtLeast(0)) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                        )
                    }
                }
            }
        }
    }
}
