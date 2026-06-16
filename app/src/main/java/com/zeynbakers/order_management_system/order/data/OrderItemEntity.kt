@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,

    // Live reference to product (can be null for custom items)
    val productId: Long? = null,

    // Immutable snapshot for historical accuracy
    val productNameSnapshot: String,
    val unitPriceSnapshot: BigDecimal,
    val categorySnapshot: ItemCategory,

    val quantity: Int,

    // Optional: Override price at order time
    val priceOverride: BigDecimal? = null
) {
    // Helper to get the effective price (override or snapshot)
    val effectivePrice: BigDecimal
        get() = priceOverride ?: unitPriceSnapshot
}

/**
 * Domain model for order item with product information.
 */
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

enum class ItemCategory {
    BAKED,
    FRIED,
    OTHER
}