package com.zeynbakers.order_management_system.order.ui

import com.zeynbakers.order_management_system.order.data.ItemCategory
import java.math.BigDecimal

/**
 * Temporary UI state for order items before saving to database.
 * Used in the order editor and cart UI components.
 */
data class OrderItemDraft(
    val productId: Long? = null,
    val emoji: String,
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val categorySnapshot: ItemCategory? = null
) {
    val lineTotal: BigDecimal
        get() = unitPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
}
