package com.zeynbakers.order_management_system.order.ui.models

import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import java.math.BigDecimal

/**
 * UI model representing an order with its items, customer, and payment status.
 */
data class OrderUiModel(
    val order: OrderEntity,
    val items: List<OrderItemEntity>,
    val customer: CustomerEntity?,
    val paidAmount: BigDecimal
) {
    val balance: BigDecimal
        get() = order.totalAmount.subtract(paidAmount)
}

/**
 * Extension to format a list of items for display as a single line summary.
 */
fun List<OrderItemEntity>.toDisplaySummary(): String {
    if (isEmpty()) return ""
    return joinToString(", ") { "${it.productNameSnapshot} x${it.quantity}" }
}
