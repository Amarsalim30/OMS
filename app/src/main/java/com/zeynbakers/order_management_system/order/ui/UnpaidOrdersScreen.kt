package com.zeynbakers.order_management_system.order.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UnpaidOrdersScreen(
        orders: List<OrderEntity>,
        paidAmounts: Map<Long, BigDecimal>,
        customerNames: Map<Long, String>,
        onBack: () -> Unit,
        onOpenDay: (LocalDate) -> Unit,
        onReceivePayment: (OrderEntity) -> Unit,
        onSettingsClick: () -> Unit = {},
        title: String = "Unpaid Orders",
        showBack: Boolean = true
) {
    val totalOutstanding =
            remember(orders, paidAmounts) {
                orders.fold(BigDecimal.ZERO) { acc, order ->
                    val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                    acc + (order.totalAmount - paid)
                }
            }
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    // Sort Newest -> Oldest for row ordering inside dates
    val sortedOrders =
            remember(orders) {
                orders.sortedWith(
                    compareByDescending<OrderEntity> { it.orderDate }
                        .thenByDescending { it.createdAt }
                )
            }
    // Group by LocalDate
    val grouped = remember(sortedOrders) { sortedOrders.groupBy { it.orderDate } }
    // Headers: today first, then future dates (desc), then past dates (desc)
    val dates =
            remember(sortedOrders, today) {
                sortedOrders.map { it.orderDate }
                    .distinct()
                    .sortedWith(
                        compareByDescending<LocalDate> { date ->
                            when {
                                date == today -> 2
                                date > today -> 1
                                else -> 0
                            }
                        }.thenByDescending { it }
                    )
            }

    Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (showBack) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                )
                            }
                        }
                )
            }
    ) { padding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp), // Extra padding for bottom content
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SummaryCard(count = orders.size, totalOutstanding = totalOutstanding) }

            if (orders.isEmpty()) {
                item { EmptyState(text = "All caught up! No unpaid orders.") }
            } else {
                dates.forEach { date ->
                    stickyHeader { StickyDateHeader(date = date) }
                    items(grouped[date].orEmpty(), key = { it.id }) { order ->
                        val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                        val balance = order.totalAmount - paid
                        val customerLabel =
                                order.customerId?.let { id -> customerNames[id] }?.takeIf {
                                    it.isNotBlank()
                                }

                        UnpaidOrderRow(
                                order = order,
                                customerLabel = customerLabel,
                                paidAmount = paid,
                                balance = balance,
                                onOpenDay = { onOpenDay(order.orderDate) },
                                onReceivePayment = { onReceivePayment(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(count: Int, totalOutstanding: BigDecimal) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
            shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accented strip
            Box(
                    modifier =
                            Modifier.width(6.dp)
                                    .height(100.dp) // Fixed height enough to cover content
                                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                        text = "TOTAL OUTSTANDING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                        text = formatKes(totalOutstanding),
                        style = MaterialTheme.typography.displaySmall, // Large impact
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = "$count orders pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyDateHeader(date: LocalDate) {
    val dateLabel = remember(date) { formatRelativeDate(date) }

    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background, // Match background for sticky effect
            tonalElevation = 0.dp
    ) {
        Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun UnpaidOrderRow(
        order: OrderEntity,
        customerLabel: String?,
        paidAmount: BigDecimal,
        balance: BigDecimal,
        onOpenDay: () -> Unit,
        onReceivePayment: () -> Unit
) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(onClick = onOpenDay),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer Name & Time
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                    if (customerLabel == null) {
                        // Walk-in Icon
                        Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Walk-in",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                                text = "Walk-in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                                text = customerLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, // Highlight customer
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Notes / Product
            if (order.notes.isNotBlank()) {
                Text(
                        text = order.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
            }

            // Financial Utility Section
            val total = order.totalAmount
            val progress =
                    if (total > BigDecimal.ZERO) {
                        paidAmount
                            .divide(total, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    } else 0f
            val displayBalance = if (balance < BigDecimal.ZERO) balance.abs() else balance
            val isOverpaid = balance < BigDecimal.ZERO

            Column {
                LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paid (Left)
                    Text(
                            text = "Paid: ${formatKes(paidAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Due/Credit (Right) - Highlighted
                    Text(
                            text =
                                    if (isOverpaid) {
                                        "Credit: ${formatKes(displayBalance)}"
                                    } else {
                                        "Due: ${formatKes(displayBalance)}"
                                    },
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace, // Utility feel
                            fontWeight = FontWeight.Bold,
                            color =
                                    if (isOverpaid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Actions Footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Future: Call button if phone exists

                OutlinedButton(
                        onClick = onReceivePayment,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors =
                                ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                )
                ) { Text("Record Payment") }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
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

private fun formatRelativeDate(date: LocalDate): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (date) {
        today -> "Today"
        today.minus(kotlinx.datetime.DatePeriod(days = 1)) -> "Yesterday"
        else -> {
            // Using java.time.format for formatted string "Mon, 24 Oct"
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
