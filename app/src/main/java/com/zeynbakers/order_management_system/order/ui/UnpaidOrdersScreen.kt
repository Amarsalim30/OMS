package com.zeynbakers.order_management_system.order.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.rememberCurrentDate
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

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
    var pendingSwipePayOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var pendingSwipeDeleteOrder by remember { mutableStateOf<OrderEntity?>(null) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
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

    LaunchedEffect(pendingSwipePayOrder?.id) {
        val pendingOrder = pendingSwipePayOrder ?: return@LaunchedEffect
        onReceivePayment(pendingOrder)
        pendingSwipePayOrder = null
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
                        val notes = order.notes.lowercase()
                        customer.contains(query) || total.contains(query) || notes.contains(query)
                    }
                }
            }

    // Group by LocalDate
    val grouped = remember(filteredOrders) { filteredOrders.groupBy { it.orderDate } }
    val totalOutstanding =
            remember(filteredOrders, paidAmounts) {
                filteredOrders.fold(BigDecimal.ZERO) { acc, order ->
                    val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                    acc + (order.totalAmount - paid)
                }
            }
    val activeContextLabel =
            when {
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

    // Headers
    val dates =
            remember(filteredOrders, today) {
                filteredOrders
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
                                    Text(stringResource(R.string.day_search_notes_or_customer))
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
                                                searchQuery = ""
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
                modifier = Modifier.fillMaxSize().padding(padding),
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
                    Text(
                            text = stringResource(R.string.unpaid_swipe_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                dates.forEach { date ->
                    // Lazy item keys on Android must be Bundle-saveable types.
                    stickyHeader(key = date.toString()) { StickyDateHeader(date = date, today = today) }
                    items(grouped[date].orEmpty(), key = { it.id }) { order ->
                        val paid = paidAmounts[order.id] ?: BigDecimal.ZERO
                        val balance = order.totalAmount - paid
                        val customerLabel =
                                order.customerId?.let { id -> customerNames[id] }?.takeIf {
                                    it.isNotBlank()
                                }

                        val dismissState =
                                rememberSwipeToDismissBoxState(
                                        confirmValueChange = {
                                            when (it) {
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    pendingSwipePayOrder = order
                                                    false
                                                }
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    pendingSwipeDeleteOrder = order
                                                    false
                                                }
                                                SwipeToDismissBoxValue.Settled -> false
                                            }
                                        }
                                )

                        SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier,
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = true,
                                backgroundContent = { SwipeBackground(dismissState) },
                                content = {
                                    UnpaidOrderRow(
                                            order = order,
                                            customerLabel = customerLabel,
                                    paidAmount = paid,
                                    balance = balance,
                                    onOpenDay = { onOpenDay(order.orderDate) },
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

    if (direction == SwipeToDismissBoxValue.StartToEnd) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = stringResource(R.string.unpaid_action_pay),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.size(8.dp))
                Text(
                        text = stringResource(R.string.unpaid_action_pay).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
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
