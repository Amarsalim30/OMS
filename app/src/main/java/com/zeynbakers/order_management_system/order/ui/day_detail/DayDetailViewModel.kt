package com.zeynbakers.order_management_system.order.ui.day_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

class DayDetailViewModel(
    private val database: AppDatabase,
    private val initialDate: LocalDate
) : ViewModel() {

    private val orderDao = database.orderDao()
    private val accountingDao = database.accountingDao()
    private val customerDao = database.customerDao()

    private val _orders = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val orders: StateFlow<List<OrderUiModel>> = _orders.asStateFlow()

    private val _dayTotal = MutableStateFlow(BigDecimal.ZERO)
    val dayTotal: StateFlow<BigDecimal> = _dayTotal.asStateFlow()

    init {
        loadOrders(initialDate)
    }

    fun loadOrders(date: LocalDate) {
        viewModelScope.launch {
            val entities = orderDao.getOrdersByDate(date.toString())
            val orderIds = entities.map { it.id }

            val paidByOrder = if (orderIds.isEmpty()) emptyMap() else {
                accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
            }

            val customerIds = entities.mapNotNull { it.customerId }.distinct()
            val customersById = if (customerIds.isEmpty()) emptyMap() else {
                customerDao.getByIds(customerIds).associateBy { it.id }
            }

            val itemDao = database.orderItemDao()

            val models = entities.map { entity ->
                OrderUiModel(
                    order = entity,
                    items = itemDao.getOrderItems(entity.id),
                    customer = entity.customerId?.let { customersById[it] },
                    paidAmount = paidByOrder[entity.id] ?: BigDecimal.ZERO
                )
            }

            _orders.value = models
            _dayTotal.value = models.sumOf { it.order.totalAmount }
        }
    }
}
