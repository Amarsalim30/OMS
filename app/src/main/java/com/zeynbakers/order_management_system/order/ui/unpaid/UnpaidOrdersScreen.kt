package com.zeynbakers.order_management_system.order.ui.unpaid

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

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
    customerPhones: Map<Long, String>,
    onBack: () -> Unit,
    onOpenDay: (LocalDate, Long?) -> Unit,
    onReceivePayment: (OrderEntity) -> Unit,
    onDeleteOrder: (OrderEntity) -> Unit,
    title: String? = null,
    showBack: Boolean = true
) {
    val today = rememberCurrentDate()
    val screenTitle = title ?: stringResource(R.string.unpaid_title)
    var selectedFilterKey by rememberSaveable { mutableStateOf(OrdersFilter.NEWEST.name) }
    val selectedFilter =
        remember(selectedFilterKey) {
            OrdersFilter.entries.firstOrNull { it.name == selectedFilterKey }
                ?: OrdersFilter.NEWEST
        }

    // Search State
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingSwipeDeleteOrder by remember { mutableStateOf<OrderEntity?>(null) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
    }

    // Base Sort
    val sortedOrders =
        remember(orders, paidAmounts, selectedFilter, today) {
            val base =
                when (selectedFilter) {
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

    // Filter Logic
    val filteredOrders =
        remember(sortedOrders, searchQuery, customerNames) {
            if (searchQuery.isBlank()) {
                sortedOrders
            } else {
                val query = searchQuery.trim().lowercase()
                sortedOrders.filter { order ->
                    val customer = customerNames[order.customerId]?.lowercase().orEmpty()
                    val total = order.totalAmount.toString()
                    customer.contains(query) || total.contains(query)
                }
            }
        }

    val showDateSections = selectedFilter != OrdersFilter.LARGEST_DUE
    val groupedByDate =
        remember(filteredOrders, selectedFilter, today) {
            if (!showDateSections) {
                sortedMapOf<LocalDate, List<OrderEntity>>()
            } else {
                val comparator =
                    when (selectedFilter) {
                        OrdersFilter.NEWEST ->
                            compareByDescending<LocalDate> { date ->
                                when {
                                    date == today -> 2
                                    date > today -> 1
                                    else -> 0
                                }
                            }.thenByDescending { it }

                        OrdersFilter.OLDEST,
                        OrdersFilter.OVERDUE -> compareBy<LocalDate> { it }

                        OrdersFilter.LARGEST_DUE -> compareByDescending<LocalDate> { it }
                    }
                filteredOrders.groupBy { it.orderDate }.toSortedMap(comparator)
            }
        }
    val totalOutstanding =
        remember(filteredOrders, paidAmounts) {
            filteredOrders.fold(BigDecimal.ZERO) { acc, order ->
                val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                acc + (order.totalAmount - paid)
            }
        }
    val activeContextLabel =
        when {
            searchQuery.isNotBlank() && selectedFilter == OrdersFilter.NEWEST ->
                stringResource(
                    R.string.unpaid_active_context_search_only,
                    searchQuery.trim()
                )

            searchQuery.isNotBlank() ->
                stringResource(
                    R.string.unpaid_active_context_filter_search,
                    stringResource(selectedFilter.labelRes),
                    searchQuery.trim()
                )

            selectedFilter != OrdersFilter.NEWEST ->
                stringResource(
                    R.string.unpaid_active_context_filter_only,
                    stringResource(selectedFilter.labelRes)
                )

            else -> null
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (isSearchActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(64.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                stringResource(
                                    R.string.day_search_notes_customer_amount_pickup
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription =
                                        stringResource(R.string.action_back)
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription =
                                            stringResource(R.string.action_clear)
                                    )
                                }
                            }
                        }
                    )
                }
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle) },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription =
                                        stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription =
                                    stringResource(R.string.action_search)
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp), // Extra padding for bottom content
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(
                    count = filteredOrders.size,
                    totalOutstanding = totalOutstanding,
                    modifier = Modifier.tutorialCoachTarget(TutorialCoachTargets.OrdersSummaryCard)
                )
            }

            if (orders.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(OrdersFilter.values(), key = { it.name }) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilterKey = filter.name },
                                label = { Text(stringResource(filter.labelRes)) }
                            )
                        }
                    }
                }
                if (activeContextLabel != null) {
                    item {
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = activeContextLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    val emptyText =
                        if (searchQuery.isNotBlank()) {
                            stringResource(
                                R.string.unpaid_empty_search_result,
                                searchQuery.trim()
                            )
                        } else {
                            stringResource(R.string.unpaid_empty_all_caught_up)
                        }
                    UnpaidEmptyState(text = emptyText)
                }
            } else {
                if (showDateSections) {
                    groupedByDate.forEach { (date, dateOrders) ->
                        // Lazy item keys on Android must be Bundle-saveable types.
                        stickyHeader(key = date.toString()) {
                            StickyDateHeader(date = date, today = today)
                        }
                        items(dateOrders, key = { it.id }) { order ->
                            val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                            val balance = order.totalAmount - paid
                            val customerLabel =
                                order.customerId?.let { id -> customerNames[id] }?.takeIf {
                                    it.isNotBlank()
                                }
                            val customerPhone =
                                order.customerId?.let { id -> customerPhones[id] }

                            val dismissState =
                                rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        when (it) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                pendingSwipeDeleteOrder = order
                                                false
                                            }

                                            SwipeToDismissBoxValue.StartToEnd,
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    }
                                )

                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = { SwipeBackground(dismissState) },
                                content = {
                                    UnpaidOrderRow(
                                        order = order,
                                        customerLabel = customerLabel,
                                        customerPhone = customerPhone,
                                        paidAmount = paid,
                                        balance = balance,
                                        onOpenDay = {
                                            onOpenDay(order.orderDate, order.id)
                                        },
                                        onReceivePayment = { onReceivePayment(order) }
                                    )
                                }
                            )
                        }
                    }
                } else {
                    items(filteredOrders, key = { it.id }) { order ->
                        val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                        val balance = order.totalAmount - paid
                        val customerLabel =
                            order.customerId?.let { id -> customerNames[id] }?.takeIf {
                                it.isNotBlank()
                            }
                        val customerPhone =
                            order.customerId?.let { id -> customerPhones[id] }

                        val dismissState =
                            rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            pendingSwipeDeleteOrder = order
                                            false
                                        }

                                        SwipeToDismissBoxValue.StartToEnd,
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                }
                            )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = { SwipeBackground(dismissState) },
                            content = {
                                UnpaidOrderRow(
                                    order = order,
                                    customerLabel = customerLabel,
                                    customerPhone = customerPhone,
                                    paidAmount = paid,
                                    balance = balance,
                                    onOpenDay = { onOpenDay(order.orderDate, order.id) },
                                    onReceivePayment = { onReceivePayment(order) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    pendingSwipeDeleteOrder?.let { order ->
        val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
        AlertDialog(
            onDismissRequest = { pendingSwipeDeleteOrder = null },
            title = { Text(stringResource(R.string.unpaid_delete_title)) },
            text = {
                Text(
                    if (paid > BigDecimal.ZERO) {
                        stringResource(
                            R.string.unpaid_delete_message_with_payments,
                            formatKes(paid)
                        )
                    } else {
                        stringResource(R.string.unpaid_delete_message_plain)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOrder(order)
                        pendingSwipeDeleteOrder = null
                    }
                ) {
                    Text(stringResource(R.string.customer_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwipeDeleteOrder = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection

    if (direction == SwipeToDismissBoxValue.EndToStart) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.customer_action_delete).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.customer_action_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
internal fun SummaryCard(
    count: Int,
    totalOutstanding: BigDecimal,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
    customerPhone: String?,
    paidAmount: BigDecimal,
    balance: BigDecimal,
    onOpenDay: () -> Unit,
    onReceivePayment: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasCustomer = !customerLabel.isNullOrBlank()
    val primaryLabel =
        customerLabel?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.unpaid_unnamed_order)
    val pickupDisplay =
        com.zeynbakers.order_management_system.core.util.formatPickupTimeForDisplay(order.pickupTime)
    val initials = getInitialsForOrder(customerLabel, "")
    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clickable(onClick = onOpenDay)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primaryLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasCustomer) {
                                Text(
                                    text = customerLabel.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            pickupDisplay?.let {
                                Text(
                                    text = stringResource(R.string.day_pickup_time_value, it),
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
                                text = formatKes(balance),
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
                                    text = stringResource(
                                        R.string.unpaid_paid_amount,
                                        formatKes(paidAmount)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.width(8.dp))

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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = stringResource(R.string.unpaid_action_pay),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
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

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val orderDate = order.orderDate.toString()
                        val message = """*Order Reminder* 📋
                                        *Order:* #${order.id}
                                        *Date:* $orderDate
                                        *Amount Due:* ${formatKes(balance)}
                                        *Total Order:* ${formatKes(order.totalAmount)}
                                        
                                        Please arrange payment at your earliest convenience. Thank you! 🙏"""
                        val intent = if (!customerPhone.isNullOrBlank()) {
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("smsto:$customerPhone")
                                putExtra("sms_body", message)
                            }
                        } else {
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                        }
                        context.startActivity(Intent.createChooser(intent, "Share order"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share order",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun getInitialsForOrder(customerName: String?, notes: String): String {
    val source = if (!customerName.isNullOrBlank()) customerName else notes
    val firstLetter = source
        .filter { it.isLetter() }
        .firstOrNull()
    return firstLetter?.uppercase() ?: "?"
}

@Composable
internal fun UnpaidEmptyState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
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
