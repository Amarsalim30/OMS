package com.zeynbakers.order_management_system.order.ui.summary

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

class SummaryViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val orderDao = database.orderDao()
    private val accountingDao = database.accountingDao()
    private val customerDao = database.customerDao()

    private val _summaryOrders = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val summaryOrders: StateFlow<List<OrderUiModel>> = _summaryOrders.asStateFlow()

    private val _summaryTotal = MutableStateFlow(BigDecimal.ZERO)
    val summaryTotal: StateFlow<BigDecimal> = _summaryTotal.asStateFlow()

    fun loadRange(startInclusive: LocalDate, endExclusive: LocalDate) {
        viewModelScope.launch {
            val entities =
                orderDao.getOrdersBetween(startInclusive.toString(), endExclusive.toString())
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

            _summaryOrders.value = models
            _summaryTotal.value = models.sumOf { it.order.totalAmount }
        }
    }
}
