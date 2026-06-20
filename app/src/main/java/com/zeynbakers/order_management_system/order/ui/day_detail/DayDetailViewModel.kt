package com.zeynbakers.order_management_system.order.ui.day_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import com.zeynbakers.order_management_system.product.data.ProductEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

class DayDetailViewModel(
    private val database: AppDatabase,
    private val initialDate: LocalDate
) : ViewModel() {

    private val orderDao = database.orderDao()
    private val accountingDao = database.accountingDao()
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()

    private val _currentDate = MutableStateFlow(initialDate)

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<OrderUiModel>> = _currentDate
        .flatMapLatest { date ->
            orderDao.getActiveOrdersWithItemsByDateFlow(date.toString())
        }
        .map { withItemsList ->
            val orderIds = withItemsList.map { it.order.id }

            val paidByOrder = if (orderIds.isEmpty()) emptyMap() else {
                accountingDao.getPaidForOrders(orderIds).associateBy { it.orderId }
            }

            val customerIds = withItemsList.mapNotNull { it.order.customerId }.distinct()
            val customersById = if (customerIds.isEmpty()) emptyMap() else {
                customerDao.getByIds(customerIds).associateBy { it.id }
            }

            withItemsList.map { withItems ->
                val entity = withItems.order
                OrderUiModel(
                    order = entity,
                    items = withItems.items,
                    customer = entity.customerId?.let { customersById[it] },
                    paidAmount = paidByOrder[entity.id]?.paid ?: BigDecimal.ZERO
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayTotal: StateFlow<BigDecimal> = orders
        .map { it.sumOf { model -> model.order.totalAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    val quickFillProducts: StateFlow<List<ProductEntity>> = productDao.getPopularProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadOrders(date: LocalDate) {
        _currentDate.value = date
    }
}
