@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: CustomerEntity?,
    balance: BigDecimal,
    financeSummary: CustomerFinanceSummary?,
    orders: List<CustomerOrderUi>,
    onBack: () -> Unit,
    onPaymentHistory: (Long) -> Unit,
    onOpenStatement: (Long) -> Unit,
    onReceivePayment: () -> Unit,
    onOrderPaymentHistory: (Long) -> Unit,
    onUpdateOrderStatusOverride: (Long, OrderStatusOverride?) -> Unit,
    onWriteOffOrder: (Long) -> Unit
) {
    var orderFilter by remember { mutableStateOf(OrderFilter.All) }

    val filteredOrders by remember(orders, orderFilter) {
        derivedStateOf { filterOrders(orders, orderFilter) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = customer?.name?.ifBlank { stringResource(R.string.money_customer) }
                            ?: stringResource(R.string.money_customer),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BalanceCard(
                    customer = customer,
                    balance = balance,
                    financeSummary = financeSummary,
                    canReceive = customer != null,
                    onReceivePayment = onReceivePayment,
                    onViewStatement = { customer?.id?.let { onOpenStatement(it) } },
                    onViewPaymentHistory = { customer?.id?.let { onPaymentHistory(it) } }
                )
            }

            item {
                OrdersHeader(
                    orders = orders,
                    orderFilter = orderFilter,
                    onFilterChange = { orderFilter = it },
                    orderCount = filteredOrders.size
                )
            }

            if (filteredOrders.isEmpty()) {
                item {
                    val emptyMessageRes =
                        if (orders.isEmpty()) {
                            R.string.customer_detail_no_orders
                        } else {
                            R.string.customer_detail_no_orders_for_filter
                        }
                    Text(
                        text = stringResource(emptyMessageRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(filteredOrders, key = { it.order.id }) { order ->
                    OrderRow(
                        order = order,
                        onPaymentHistory = { onOrderPaymentHistory(order.order.id) },
                        onUpdateOverride = { override -> onUpdateOrderStatusOverride(order.order.id, override) },
                        onWriteOff = { onWriteOffOrder(order.order.id) }
                    )
                }
            }
        }
    }
}

