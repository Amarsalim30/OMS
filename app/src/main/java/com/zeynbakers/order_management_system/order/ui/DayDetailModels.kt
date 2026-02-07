package com.zeynbakers.order_management_system.order.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppFilterOption
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal

internal fun resolvePaymentState(total: BigDecimal, paidAmount: BigDecimal): PaymentState {
    if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
        return PaymentState.UNPAID
    }
    val balance = total.subtract(paidAmount)
    return when {
        balance.compareTo(BigDecimal.ZERO) > 0 -> PaymentState.PARTIAL
        balance.compareTo(BigDecimal.ZERO) == 0 -> PaymentState.PAID
        else -> PaymentState.OVERPAID
    }
}

@Composable
internal fun paymentStateColor(state: PaymentState): Color {
    return when (state) {
        PaymentState.PAID -> MaterialTheme.colorScheme.tertiary
        PaymentState.PARTIAL -> MaterialTheme.colorScheme.secondary
        PaymentState.UNPAID -> MaterialTheme.colorScheme.error
        PaymentState.OVERPAID -> MaterialTheme.colorScheme.primary
    }
}

@Composable
internal fun paymentStateLabel(state: PaymentState): String {
    return when (state) {
        PaymentState.PAID -> stringResource(R.string.day_status_paid)
        PaymentState.PARTIAL -> stringResource(R.string.day_status_partial)
        PaymentState.UNPAID -> stringResource(R.string.day_status_unpaid)
        PaymentState.OVERPAID -> stringResource(R.string.day_status_overpaid)
    }
}

internal fun computeDayStats(
    orders: List<OrderEntity>,
    orderPaidAmounts: Map<Long, BigDecimal>,
    dayTotal: BigDecimal
): DaySummaryStats {
    var paidCount = 0
    var partialCount = 0
    var unpaidCount = 0
    var overpaidCount = 0
    var totalPaid = BigDecimal.ZERO

    orders.forEach { order ->
        val paidAmount = orderPaidAmounts[order.id] ?: BigDecimal.ZERO
        totalPaid = totalPaid.add(paidAmount)
        when (resolvePaymentState(order.totalAmount, paidAmount)) {
            PaymentState.PAID -> paidCount += 1
            PaymentState.PARTIAL -> partialCount += 1
            PaymentState.UNPAID -> unpaidCount += 1
            PaymentState.OVERPAID -> overpaidCount += 1
        }
    }

    return DaySummaryStats(
        orderCount = orders.size,
        paidCount = paidCount,
        partialCount = partialCount,
        unpaidCount = unpaidCount,
        overpaidCount = overpaidCount,
        totalPaid = totalPaid,
        balance = dayTotal.subtract(totalPaid)
    )
}

internal fun titleCase(value: String): String {
    return value.lowercase().replaceFirstChar { it.uppercase() }
}

internal data class DaySummaryStats(
    val orderCount: Int,
    val paidCount: Int,
    val partialCount: Int,
    val unpaidCount: Int,
    val overpaidCount: Int,
    val totalPaid: BigDecimal,
    val balance: BigDecimal
)

data class OrderDraft(
    val notes: String,
    val totalText: String,
    val customerName: String,
    val customerPhone: String,
    val pickupTime: String,
    val editingOrderId: Long?
)

internal enum class DayOrderFilter(val labelRes: Int) {
    All(R.string.day_filter_all),
    Unpaid(R.string.day_filter_unpaid),
    Partial(R.string.day_filter_partial),
    Paid(R.string.day_filter_paid),
    Overpaid(R.string.day_filter_overpaid)
}

@Composable
internal fun dayOrderFilterOptions(
    totalOrders: Int,
    stats: DaySummaryStats
): List<AppFilterOption> {
    return DayOrderFilter.values().map { filter ->
        val count =
            when (filter) {
                DayOrderFilter.All -> totalOrders
                DayOrderFilter.Unpaid -> stats.unpaidCount
                DayOrderFilter.Partial -> stats.partialCount
                DayOrderFilter.Paid -> stats.paidCount
                DayOrderFilter.Overpaid -> stats.overpaidCount
            }
        val baseLabel = stringResource(filter.labelRes)
        val label = if (count == 0 && filter != DayOrderFilter.All) {
            baseLabel
        } else {
            stringResource(R.string.day_filter_with_count, baseLabel, count)
        }
        AppFilterOption(filter.name, label)
    }
}

internal enum class DeleteMoveTarget {
    ORDER,
    OLDEST_ORDERS,
    CUSTOMER_CREDIT
}

internal fun dayEmptyStateRes(
    orders: List<OrderEntity>,
    orderFilter: DayOrderFilter,
    searchQuery: String
): Pair<Int, Int> {
    return when {
        orders.isEmpty() ->
            Pair(
                R.string.day_empty_no_orders_title,
                R.string.day_empty_no_orders_subtitle
            )
        searchQuery.isNotBlank() ->
            Pair(
                R.string.day_empty_no_matches_title,
                R.string.day_empty_no_matches_subtitle
            )
        orderFilter == DayOrderFilter.Unpaid ->
            Pair(
                R.string.day_empty_no_unpaid_title,
                R.string.day_empty_no_unpaid_subtitle
            )
        orderFilter == DayOrderFilter.Partial ->
            Pair(
                R.string.day_empty_no_partial_title,
                R.string.day_empty_no_partial_subtitle
            )
        orderFilter == DayOrderFilter.Paid ->
            Pair(
                R.string.day_empty_no_paid_title,
                R.string.day_empty_no_paid_subtitle
            )
        orderFilter == DayOrderFilter.Overpaid ->
            Pair(
                R.string.day_empty_no_overpaid_title,
                R.string.day_empty_no_overpaid_subtitle
            )
        else ->
            Pair(
                R.string.day_empty_no_orders_filter_title,
                R.string.day_empty_no_orders_filter_subtitle
            )
    }
}
