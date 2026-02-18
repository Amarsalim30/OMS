package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

@Composable
internal fun DaySummaryCard(
    date: LocalDate,
    dayTotal: BigDecimal,
    stats: DaySummaryStats
) {
    val monthLabel = titleCase(date.month.name)
    val dayOfWeekLabel = titleCase(date.dayOfWeek.name)
    val balanceLabel =
        if (stats.balance.signum() >= 0) {
            stringResource(R.string.day_balance_label)
        } else {
            stringResource(R.string.day_over_label)
        }
    val balanceValue = formatKes(stats.balance.abs())

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = monthLabel.take(3).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayOfWeekLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$monthLabel ${date.dayOfMonth}, ${date.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.day_total_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatKes(dayTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(label = stringResource(R.string.day_paid_label), value = formatKes(stats.totalPaid))
                SummaryMetric(label = balanceLabel, value = balanceValue)
            }

            Spacer(Modifier.height(8.dp))

            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = stringResource(R.string.day_orders_label),
                    count = stats.orderCount,
                    color = MaterialTheme.colorScheme.primary
                )
                if (stats.paidCount > 0) {
                    SummaryChip(
                        label = stringResource(R.string.day_status_paid),
                        count = stats.paidCount,
                        color = paymentStateColor(PaymentState.PAID)
                    )
                }
                if (stats.partialCount > 0) {
                    SummaryChip(
                        label = stringResource(R.string.day_status_partial),
                        count = stats.partialCount,
                        color = paymentStateColor(PaymentState.PARTIAL)
                    )
                }
                if (stats.unpaidCount > 0) {
                    SummaryChip(
                        label = stringResource(R.string.day_status_unpaid),
                        count = stats.unpaidCount,
                        color = paymentStateColor(PaymentState.UNPAID)
                    )
                }
                if (stats.overpaidCount > 0) {
                    SummaryChip(
                        label = stringResource(R.string.day_status_overpaid),
                        count = stats.overpaidCount,
                        color = paymentStateColor(PaymentState.OVERPAID)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = stringResource(R.string.day_chip_count, label, count),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun EmptyDayState(
    title: String,
    subtitle: String
) {
    AppEmptyState(
        title = title,
        body = subtitle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
internal fun OrderListItem(
    order: OrderEntity,
    customerLabel: String,
    paidAmount: BigDecimal,
    paymentState: PaymentState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPaymentHistory: () -> Unit,
    onReceivePayment: () -> Unit
) {
    val stateColor = paymentStateColor(paymentState)
    val statusLabel = paymentStateLabel(paymentState)
    val balance = order.totalAmount.subtract(paidAmount)
    val showReceive = paymentState == PaymentState.UNPAID || paymentState == PaymentState.PARTIAL
    val detailLabel =
        when (paymentState) {
            PaymentState.UNPAID -> stringResource(R.string.day_balance_due_amount, formatKes(order.totalAmount))
            PaymentState.PARTIAL -> stringResource(R.string.day_balance_due_amount, formatKes(balance))
            PaymentState.OVERPAID ->
                stringResource(
                    R.string.day_overpaid_by_amount,
                    formatKes(paidAmount.subtract(order.totalAmount))
                )
            PaymentState.PAID -> stringResource(R.string.day_paid_in_full)
        }
    val customerTextColor =
        if (customerLabel == stringResource(R.string.day_no_customer)) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = stateColor,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .width(6.dp)
                    .heightIn(min = 48.dp)
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = order.notes,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatKes(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(min = 84.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = customerLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = customerTextColor
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = statusLabel, color = stateColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = detailLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.day_tap_to_edit_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onPaymentHistory) {
                        Text(stringResource(R.string.day_history))
                    }
                    if (showReceive) {
                        Button(onClick = onReceivePayment) {
                            Text(stringResource(R.string.customer_action_record_payment))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.day_delete_order))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
