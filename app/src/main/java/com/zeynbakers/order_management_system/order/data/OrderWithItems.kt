@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Embedded
import androidx.room.Relation
import com.zeynbakers.order_management_system.product.data.ProductEntity

// Domain model for order with its items
data class OrderWithItems(
    @Embedded
    val order: OrderEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)

// Domain model for order item with product information
data class OrderItemWithProduct(
    val id: Long,
    val orderId: Long,
    val productId: Long?,
    val productNameSnapshot: String,
    val unitPriceSnapshot: java.math.BigDecimal,
    val categorySnapshot: ItemCategory,
    val quantity: Int,
    val priceOverride: java.math.BigDecimal?,
    
    // Joined product information
    val productName: String?,
    val productEmoji: String?,
    val productDefaultPrice: java.math.BigDecimal?
) {
    // Helper to get effective display name (prefer product name if available)
    val displayName: String
        get() = productName ?: productNameSnapshot
    
    // Helper to get effective emoji (prefer product emoji if available)
    val displayEmoji: String?
        get() = productEmoji
    
    // Helper to get effective price (override or snapshot)
    val effectivePrice: java.math.BigDecimal
        get() = priceOverride ?: unitPriceSnapshot
    
    // Helper to get line total
    val lineTotal: java.math.BigDecimal
        get() = effectivePrice.multiply(java.math.BigDecimal.valueOf(quantity.toLong()))
}