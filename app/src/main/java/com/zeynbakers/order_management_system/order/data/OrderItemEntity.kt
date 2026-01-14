@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.*
import java.math.BigDecimal

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,

    val name: String,
    val category: ItemCategory,
    val quantity: Int,
    val unitPrice: BigDecimal
)

enum class ItemCategory {
    BAKED,
    FRIED,
    OTHER
}
