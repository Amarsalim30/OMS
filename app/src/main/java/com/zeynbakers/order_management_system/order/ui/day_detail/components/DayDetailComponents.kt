package com.zeynbakers.order_management_system.order.ui.day_detail.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.components.AppEmptyState
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.ui.common.PaymentState
import com.zeynbakers.order_management_system.order.ui.day_detail.models.DaySummaryStats
import com.zeynbakers.order_management_system.order.ui.day_detail.models.paymentStateColor
import com.zeynbakers.order_management_system.order.ui.day_detail.models.paymentStateLabel
import com.zeynbakers.order_management_system.order.ui.day_detail.models.plannerPickupDisplay
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import com.zeynbakers.order_management_system.order.ui.models.toDisplaySummary
import java.math.BigDecimal

@Composable
internal fun DaySummaryCard(dayTotal: BigDecimal, stats: DaySummaryStats) {
    val balanceLabel =
        if (stats.balance.signum() >= 0) {
            stringResource(R.string.day_outstanding_label)
        } else {
            stringResource(R.string.day_credit_label)
        }
    val balanceValue = formatKes(stats.balance.abs())
    val balanceColor =
        if (stats.balance.signum() > 0) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
internal fun OrderListItem(
    orderUi: OrderUiModel,
    isFocused: Boolean,
    onEdit: () -> Unit,
    onPaymentHistory: () -> Unit,
    onReceivePayment: () -> Unit,
    onPrintReceipt: () -> Unit
) {
    val order = orderUi.order
    val customerLabel = orderUi.customer?.name ?: stringResource(R.string.customer_unknown)
    val paidAmount = orderUi.paidAmount
    val paymentState =
        com.zeynbakers.order_management_system.order.ui.day_detail.models.resolvePaymentState(
            order.totalAmount,
            paidAmount
        )
    val stateColor = paymentStateColor(paymentState)
    val statusLabel = paymentStateLabel(paymentState)
    val balance = order.totalAmount.subtract(paidAmount)
    val showReceive = paymentState == PaymentState.UNPAID || paymentState == PaymentState.PARTIAL

    val pickupLabel = plannerPickupDisplay(order.pickupTime)

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Customer and Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (pickupLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.day_pickup_time_value, pickupLabel),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = formatKes(order.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Order Content
            Text(
                text = orderUi.items.toDisplaySummary(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Status and Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(label = statusLabel, color = stateColor)

                val detailLabel = when (paymentState) {
                    PaymentState.UNPAID -> stringResource(
                        R.string.day_balance_due_amount,
                        formatKes(order.totalAmount)
                    )

                    PaymentState.PARTIAL -> stringResource(
                        R.string.day_balance_due_amount,
                        formatKes(balance)
                    )

                    PaymentState.OVERPAID -> stringResource(
                        R.string.day_overpaid_by_amount,
                        formatKes(paidAmount.subtract(order.totalAmount))
                    )

                    PaymentState.PAID -> stringResource(R.string.day_paid_in_full)
                }

                Text(
                    text = " • $detailLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (paymentState == PaymentState.PAID) MaterialTheme.colorScheme.onSurfaceVariant else stateColor,
                    fontWeight = if (paymentState != PaymentState.PAID) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.day_edit_order),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onPaymentHistory, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = stringResource(R.string.day_history),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showReceive) {
                    Button(
                        onClick = onReceivePayment,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.customer_action_record_payment),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onPrintReceipt,
                    modifier = (if (showReceive) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Print,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.order_print_receipt),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            letterSpacing = 0.5.sp
        )
    }
}
