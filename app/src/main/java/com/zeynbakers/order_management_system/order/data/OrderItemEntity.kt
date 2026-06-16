@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.*
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

enum class ItemCategory {
    BAKED,
    FRIED,
    OTHER
}