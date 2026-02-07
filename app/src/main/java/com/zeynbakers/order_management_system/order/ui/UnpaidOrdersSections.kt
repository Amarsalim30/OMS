package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate

@Composable
internal fun SummaryCard(
    count: Int,
    totalOutstanding: BigDecimal,
    hideBalances: Boolean
) {
    AppCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.unpaid_total_outstanding),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text =
                        if (hideBalances) {
                            stringResource(R.string.money_amount_hidden)
                        } else {
                            formatKes(totalOutstanding)
                        },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.5).sp
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text =
                        pluralStringResource(
                            id = R.plurals.unpaid_orders_count,
                            count = count,
                            count
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun StickyDateHeader(date: LocalDate, today: LocalDate) {
    val todayLabel = stringResource(R.string.unpaid_date_today)
    val yesterdayLabel = stringResource(R.string.unpaid_date_yesterday)
    val dateLabel = remember(date, today, todayLabel, yesterdayLabel) {
        formatRelativeDate(date, today, todayLabel, yesterdayLabel)
    }
    val isOverdue = date < today
    val isToday = date == today

    val textColor =
        when {
            isToday -> MaterialTheme.colorScheme.primary
            isOverdue -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (isOverdue) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.unpaid_overdue_cd),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
internal fun UnpaidOrderRow(
    modifier: Modifier = Modifier,
    order: OrderEntity,
    customerLabel: String?,
    paidAmount: BigDecimal,
    balance: BigDecimal,
    hideBalances: Boolean,
    onOpenDay: () -> Unit,
    onReceivePayment: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val walkInLabel = stringResource(R.string.unpaid_customer_walk_in)
    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(onClick = onOpenDay)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = getInitials(customerLabel ?: walkInLabel),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customerLabel ?: walkInLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (order.notes.isNotBlank()) {
                                Text(
                                    text = order.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text =
                                    if (hideBalances) {
                                        stringResource(R.string.money_amount_hidden)
                                    } else {
                                        formatKes(balance)
                                    },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color =
                                    if (balance < BigDecimal.ZERO) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                            )
                            if (paidAmount > BigDecimal.ZERO) {
                                Text(
                                    text =
                                        if (hideBalances) {
                                            stringResource(
                                                R.string.unpaid_paid_amount,
                                                stringResource(R.string.money_amount_hidden)
                                            )
                                        } else {
                                            stringResource(R.string.unpaid_paid_amount, formatKes(paidAmount))
                                        },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progress =
                    if (order.totalAmount > BigDecimal.ZERO) {
                        paidAmount
                            .divide(order.totalAmount, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier =
                        Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReceivePayment()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = stringResource(R.string.unpaid_action_pay),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.unpaid_action_pay),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun getInitials(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull() }
        .joinToString("")
        .uppercase()
}

@Composable
internal fun UnpaidEmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRelativeDate(
    date: LocalDate,
    today: LocalDate,
    todayLabel: String,
    yesterdayLabel: String
): String {
    return when (date) {
        today -> todayLabel
        today.minus(kotlinx.datetime.DatePeriod(days = 1)) -> yesterdayLabel
        else -> {
            val javaDate = date.toJavaLocalDate()
            val formatter =
                if (date.year == today.year) {
                    DateTimeFormatter.ofPattern("EEE, dd MMM")
                } else {
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
                }
            javaDate.format(formatter)
        }
    }
}
