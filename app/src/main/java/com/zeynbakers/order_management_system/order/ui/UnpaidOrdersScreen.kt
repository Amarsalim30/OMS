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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate

private enum class OrdersFilter(val labelRes: Int) {
    NEWEST(R.string.unpaid_filter_newest),
    OLDEST(R.string.unpaid_filter_oldest),
    LARGEST_DUE(R.string.unpaid_filter_largest_due),
    OVERDUE(R.string.unpaid_filter_overdue)
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
        title: String? = null,
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
    val screenTitle = title ?: stringResource(R.string.unpaid_title)
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
                        title = { Text(screenTitle) },
                        navigationIcon = {
                            if (showBack) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = stringResource(R.string.action_settings)
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
                                label = { Text(stringResource(filter.labelRes)) }
                        )
                    }
                }
            }

            if (orders.isEmpty()) {
                item { UnpaidEmptyState(text = stringResource(R.string.unpaid_empty_all_caught_up)) }
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
                                onReceivePayment = { onReceivePayment(order) }
                        )
                    }
                }
            }
        }
    }
}
