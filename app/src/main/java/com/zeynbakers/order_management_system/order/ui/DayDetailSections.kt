package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
internal fun DaySummaryCard(dayTotal: BigDecimal, stats: DaySummaryStats) {
    val balanceLabel =
            if (stats.balance.signum() >= 0) {
                stringResource(R.string.day_balance_label)
            } else {
                stringResource(R.string.day_over_label)
            }
    val balanceValue = formatKes(stats.balance.abs())
    val balanceColor =
            if (stats.balance.signum() > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

    Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
        ) {
            SummaryMetric(
                    label = stringResource(R.string.day_total_label),
                    value = formatKes(dayTotal),
                    modifier = Modifier.weight(1f),
                    valueStyle = MaterialTheme.typography.titleLarge
            )
            SummaryMetric(
                    label = stringResource(R.string.day_paid_label),
                    value = formatKes(stats.totalPaid),
                    modifier = Modifier.weight(1f),
                    alignEnd = true
            )
            SummaryMetric(
                    label = balanceLabel,
                    value = balanceValue,
                    modifier = Modifier.weight(1f),
                    valueColor = balanceColor,
                    alignEnd = true
            )
        }
    }
}

@Composable
private fun SummaryMetric(
        label: String,
        value: String,
        modifier: Modifier = Modifier,
        valueColor: Color = MaterialTheme.colorScheme.onSurface,
        alignEnd: Boolean = false,
        valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val alignment = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign
        )
        Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                textAlign = textAlign
        )
    }
}

@Composable
internal fun EmptyDayState(title: String, subtitle: String) {
    AppEmptyState(
            title = title,
            body = subtitle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
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
                PaymentState.UNPAID ->
                        stringResource(
                                R.string.day_balance_due_amount,
                                formatKes(order.totalAmount)
                        )
                PaymentState.PARTIAL ->
                        stringResource(R.string.day_balance_due_amount, formatKes(balance))
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
    val pickupLabel = plannerPickupDisplay(order.pickupTime)
    val pickupText =
            if (pickupLabel != null) {
                stringResource(R.string.day_pickup_time_value, pickupLabel)
            } else {
                stringResource(R.string.day_pickup_no_time)
            }

    AppCard(
            modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                        onEdit()
                    }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                    color = stateColor,
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.width(5.dp).heightIn(min = 40.dp)
            ) {}
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                            text = order.notes,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                            text = formatKes(order.totalAmount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(min = 72.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                        text = pickupText,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                                if (pickupLabel != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                )
                Spacer(Modifier.height(2.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = customerLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = customerTextColor,
                            modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                        text = "$statusLabel | $detailLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
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
                    IconButton(
                            onClick = onDelete,
                    ) {
                        Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.day_delete_order),
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
