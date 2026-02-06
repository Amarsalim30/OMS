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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate

private enum class OrdersFilter(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LARGEST_DUE("Largest Due"),
    OVERDUE("Overdue")
}

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
    val today = rememberCurrentDate()
    val selectedFilter = rememberSaveable { mutableStateOf(OrdersFilter.NEWEST) }
    // Sort base order for row ordering inside dates
    val sortedOrders =
            remember(orders, paidAmounts, selectedFilter.value, today) {
                val base =
                        when (selectedFilter.value) {
                            OrdersFilter.NEWEST ->
                                    orders.sortedWith(
                                            compareByDescending<OrderEntity> { it.orderDate }
                                                    .thenByDescending { it.createdAt }
                                    )
                            OrdersFilter.OLDEST ->
                                    orders.sortedWith(
                                            compareBy<OrderEntity> { it.orderDate }.thenBy {
                                                it.createdAt
                                            }
                                    )
                            OrdersFilter.LARGEST_DUE ->
                                    orders.sortedByDescending { order ->
                                        val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                                        order.totalAmount - paid
                                    }
                            OrdersFilter.OVERDUE ->
                                    orders
                                            .filter { it.orderDate < today }
                                            .sortedWith(
                                                    compareBy<OrderEntity> { it.orderDate }.thenBy {
                                                        it.createdAt
                                                    }
                                            )
                        }
                base
            }
    // Group by LocalDate
    val grouped = remember(sortedOrders) { sortedOrders.groupBy { it.orderDate } }
    // Headers: today first, then future dates (desc), then past dates (desc)
    val dates =
            remember(sortedOrders, today) {
                sortedOrders
                        .map { it.orderDate }
                        .distinct()
                        .sortedWith(
                                compareByDescending<LocalDate> { date ->
                                    when {
                                        date == today -> 2
                                        date > today -> 1
                                        else -> 0
                                    }
                                }
                                        .thenByDescending { it }
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
            item {
                LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(OrdersFilter.values(), key = { it.name }) { filter ->
                        FilterChip(
                                selected = selectedFilter.value == filter,
                                onClick = { selectedFilter.value = filter },
                                label = { Text(filter.label) }
                        )
                    }
                }
            }

            if (orders.isEmpty()) {
                item { EmptyState(text = "All caught up! No unpaid orders.") }
            } else {
                dates.forEach { date ->
                    stickyHeader { StickyDateHeader(date = date, today = today) }
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
                                onReceivePayment = { onReceivePayment(order) },
                                today = today
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                        text = "Total Outstanding",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                        text = formatKes(totalOutstanding),
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
                        text = "$count Orders",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StickyDateHeader(date: LocalDate, today: LocalDate) {
    val dateLabel = remember(date, today) { formatRelativeDate(date, today) }
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
            color =
                    MaterialTheme.colorScheme
                            .background, // Match scaffold background for sticky header
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
                        contentDescription = "Overdue",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun UnpaidOrderRow(
        modifier: Modifier = Modifier,
        order: OrderEntity,
        customerLabel: String?,
        paidAmount: BigDecimal,
        balance: BigDecimal,
        onOpenDay: () -> Unit,
        onReceivePayment: () -> Unit,
        today: LocalDate
) {
    val haptic = LocalHapticFeedback.current
    Card(
            modifier =
                    modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(onClick = onOpenDay),
            shape = MaterialTheme.shapes.small,
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Avatar / Icon
                Surface(
                        shape = MaterialTheme.shapes.medium, // Squircle
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                                text = getInitials(customerLabel ?: "Walk-in"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Main Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment =
                                    Alignment.Top // Align top to handle multiline names vs amount
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = customerLabel ?: "Walk-in",
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

                        // Amount & Status
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                    text = formatKes(balance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color =
                                            if (balance < BigDecimal.ZERO)
                                                    MaterialTheme.colorScheme.primary // Credit
                                            else MaterialTheme.colorScheme.error // Due
                            )
                            if (paidAmount > BigDecimal.ZERO) {
                                Text(
                                        text = "paid ${formatKes(paidAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Progress + Action
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
                        } else 0f

                LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = MaterialTheme.colorScheme.tertiary, // Green/Money color
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Quick Action
                Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.clickable {
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
                                contentDescription = "Pay",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = "PAY",
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
    return name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
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

private fun formatRelativeDate(date: LocalDate, today: LocalDate): String {
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
