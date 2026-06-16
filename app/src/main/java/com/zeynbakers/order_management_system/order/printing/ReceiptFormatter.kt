package com.zeynbakers.order_management_system.order.printing

import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.core.util.formatPickupTimeForDisplay
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.order.domain.formatQuantity

object ReceiptFormatter {
    private const val LINE_WIDTH = 32
    private const val SEPARATOR = "--------------------------------"

    fun formatOrder(
        storeName: String,
        order: OrderEntity,
        orderItems: List<OrderItemEntity> = emptyList(),
        customerLabel: String? = null,
        customerPhone: String? = null
    ): String {
        val headerName = storeName.trim().ifBlank { "Store" }
        val dateLine = buildDateLine(order)
        val lines = mutableListOf<String>()

        lines += headerName.uppercase()
        lines += "Order #${order.id}"
        lines += dateLine
        if (!customerLabel.isNullOrBlank()) {
            lines += customerLabel.trim()
        }
        if (!customerPhone.isNullOrBlank()) {
            lines += customerPhone.trim()
        }
        lines += ""
        lines += SEPARATOR

        if (orderItems.isNotEmpty()) {
            orderItems.forEach { item ->
                val qty = item.quantity.toString()
                lines += formatItemLine(item.productNameSnapshot, qty)
            }
        }

        lines += ""
        lines += "TOTAL: ${formatKes(order.totalAmount)}"
        lines += ""
        lines += "Thank you"

        return lines.joinToString("\n")
    }

    private fun buildDateLine(order: OrderEntity): String {
        val pickup = formatPickupTimeForDisplay(order.pickupTime)
        return if (pickup != null) {
            "${order.orderDate}  $pickup"
        } else {
            order.orderDate.toString()
        }
    }

    private fun formatItemLine(name: String, quantity: String): String {
        val left = "${name.trim()} x$quantity"
        if (left.length <= LINE_WIDTH) {
            return left
        }
        return left.take(LINE_WIDTH)
    }
}
