package com.zeynbakers.order_management_system.order.ui.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.core.ui.theme.DarkWeekendSaturday
import com.zeynbakers.order_management_system.core.ui.theme.DarkWeekendSunday
import com.zeynbakers.order_management_system.core.ui.theme.OutOfMonthDateColor
import com.zeynbakers.order_management_system.order.ui.common.CalendarDayUi
import com.zeynbakers.order_management_system.order.ui.common.PaymentState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

private const val MAX_VISIBLE_MARKERS = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DayCell(
    day: CalendarDayUi,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onQuickAdd: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onBoundsChanged: ((Rect) -> Unit)? = null
) {
    val hasOrders = day.orderCount > 0
    val markerStates = remember(day.orderStates, day.orderCount, day.paymentState) {
        resolveMarkerStates(day)
    }
    val dayDescription = remember(day, markerStates, isSelected) {
        buildDayContentDescription(day, markerStates, isSelected)
    }
    val dayTag = remember(day.date) { "day-cell-${day.date}" }
    val markerTag = remember(day.date) { "day-markers-${day.date}" }
    val compactTotal =
        remember(day.totalAmount, hasOrders) {
            if (hasOrders) formatKesCompact(day.totalAmount) else null
        }
    val statusTextColor =
        when (day.paymentState) {
            PaymentState.UNPAID -> calendarPaymentStateColor(PaymentState.UNPAID)
            PaymentState.PAID -> calendarPaymentStateColor(PaymentState.PAID)
            PaymentState.PARTIAL -> calendarPaymentStateColor(PaymentState.PARTIAL)
            PaymentState.OVERPAID -> calendarPaymentStateColor(PaymentState.OVERPAID)
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dayOfWeek = remember(day.date) {
        val javaDate = day.date.toJavaLocalDate()
        val cal = Calendar.getInstance()
        cal.set(javaDate.year, javaDate.monthValue - 1, javaDate.dayOfMonth)
        cal.get(Calendar.DAY_OF_WEEK)
    }

    val baseTextColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val dayTextColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            !day.isInCurrentMonth -> OutOfMonthDateColor
            isDark && dayOfWeek == Calendar.SATURDAY -> DarkWeekendSaturday
            isDark && dayOfWeek == Calendar.SUNDAY -> DarkWeekendSunday
            else -> baseTextColor
        }

    val containerColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> Color.Transparent
        }
    val selectionBorder =
        if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    val todayRingColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        }
    val handleClick = {
        onSelectDate(day.date)
        onOpenDay(day.date)
    }

    Surface(
        modifier = modifier
            .testTag(dayTag)
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged?.invoke(coordinates.boundsInRoot())
            }
            .semantics(mergeDescendants = true) {
                contentDescription = dayDescription
                role = Role.Button
                selected = isSelected
            }
            .combinedClickable(
                enabled = day.isInCurrentMonth,
                onClickLabel = "Open day",
                onLongClickLabel = "Quick add order",
                onClick = { handleClick() },
                onLongClick = { onQuickAdd(day.date) }
            ),
        color = containerColor,
        border = selectionBorder,
        shape = CircleShape,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = if (day.isToday) BorderStroke(1.5.dp, todayRingColor) else null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = dayTextColor,
                            maxLines = 1
                        )
                    }
                }
            }
            if (hasOrders) {
                Text(
                    text = "${day.orderCount} - $compactTotal",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (markerStates.isNotEmpty()) {
                    val visibleMarkers = markerStates.take(MAX_VISIBLE_MARKERS)
                    val overflowCount = markerStates.size - visibleMarkers.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(markerTag),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleMarkers.forEachIndexed { index, state ->
                            Surface(
                                color = calendarPaymentStateColor(state),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(7.dp)
                                    .testTag("$markerTag-$index")
                            ) {}
                        }
                        if (overflowCount > 0) {
                            Text(
                                text = "+$overflowCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.testTag("$markerTag-overflow")
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun calendarPaymentStateColor(state: PaymentState): Color {
    return when (state) {
        PaymentState.UNPAID -> Color(0xFFE53935)
        PaymentState.PAID -> Color(0xFF2E7D32)
        PaymentState.PARTIAL -> Color(0xFFFFA000)
        PaymentState.OVERPAID -> Color(0xFF5C6BC0)
    }
}

private fun formatKesCompact(amount: BigDecimal): String {
    val rounded = amount.setScale(0, RoundingMode.HALF_UP).toLong()
    val abs = kotlin.math.abs(rounded)
    val sign = if (rounded < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> sign + formatOneDecimal(abs, 1_000_000, "m")
        abs >= 1_000 -> sign + formatOneDecimal(abs, 1_000, "k")
        else -> "$sign$abs"
    }
}

private fun formatOneDecimal(value: Long, divisor: Long, suffix: String): String {
    val whole = value / divisor
    val remainder = (value % divisor) / (divisor / 10)
    return if (remainder == 0L) {
        "$whole$suffix"
    } else {
        "$whole.$remainder$suffix"
    }
}

private fun resolveMarkerStates(day: CalendarDayUi): List<PaymentState> {
    if (day.orderStates.isNotEmpty()) {
        return day.orderStates
    }
    if (day.paymentState != null && day.orderCount > 0) {
        return List(day.orderCount) { day.paymentState }
    }
    return emptyList()
}

private fun buildDayContentDescription(
    day: CalendarDayUi,
    markerStates: List<PaymentState>,
    isSelected: Boolean
): String {
    val monthLabel = day.date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val builder = StringBuilder()
    builder.append("${day.date.dayOfMonth} $monthLabel ${day.date.year}")
    if (day.isToday) {
        builder.append(", today")
    }
    if (day.orderCount > 0) {
        builder.append(", ${day.orderCount} orders")
        val unpaidCount = markerStates.count { it == PaymentState.UNPAID }
        val partialCount = markerStates.count { it == PaymentState.PARTIAL }
        val paidCount = markerStates.count { it == PaymentState.PAID }
        val overpaidCount = markerStates.count { it == PaymentState.OVERPAID }
        if (unpaidCount > 0) builder.append(", $unpaidCount unpaid")
        if (partialCount > 0) builder.append(", $partialCount partial")
        if (paidCount > 0) builder.append(", $paidCount paid")
        if (overpaidCount > 0) builder.append(", $overpaidCount overpaid")
    } else {
        builder.append(", no orders")
    }
    if (isSelected) {
        builder.append(", selected")
    }
    return builder.toString()
}
