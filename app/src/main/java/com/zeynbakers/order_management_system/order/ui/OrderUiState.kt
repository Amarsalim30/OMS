@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.ui

import com.zeynbakers.order_management_system.order.data.OrderEntity
import kotlinx.datetime.LocalDate

data class OrderUiState(
    val date: LocalDate,
    val orders: List<OrderEntity> = emptyList(),
    val editingOrder: OrderEntity? = null
)
