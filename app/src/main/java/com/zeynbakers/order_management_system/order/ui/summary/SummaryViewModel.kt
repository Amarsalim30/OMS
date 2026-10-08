package com.zeynbakers.order_management_system.order.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(
    database: AppDatabase
) : ViewModel() {

    private val orderDao = database.orderDao()
    private val accountingDao = database.accountingDao()
    private val customerDao = database.customerDao()

    private val _dateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    val summaryOrders: StateFlow<List<OrderUiModel>> = _dateRange
        .flatMapLatest { range ->
            if (range == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                orderDao.getOrdersWithItemsBetweenFlow(range.first.toString(), range.second.toString())
                    .map { ordersWithItems ->
                        val orderIds = ordersWithItems.map { it.order.id }
                        val customerIds = ordersWithItems.mapNotNull { it.order.customerId }.distinct()

                        val paidByOrder = if (orderIds.isEmpty()) emptyMap() else {
                            accountingDao.getPaidForOrders(orderIds).associateBy({ it.orderId }, { it.paid })
                        }
                        val customersById = if (customerIds.isEmpty()) emptyMap() else {
                            customerDao.getByIds(customerIds).associateBy { it.id }
                        }

                        ordersWithItems.map { orderWithItems ->
                            OrderUiModel(
                                order = orderWithItems.order,
                                items = orderWithItems.items,
                                customer = orderWithItems.order.customerId?.let { customersById[it] },
                                paidAmount = paidByOrder[orderWithItems.order.id] ?: BigDecimal.ZERO
                            )
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryTotal: StateFlow<BigDecimal> = summaryOrders
        .map { orders -> orders.sumOf { it.order.totalAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    fun loadRange(startInclusive: LocalDate, endExclusive: LocalDate) {
        _dateRange.value = startInclusive to endExclusive
    }
}
