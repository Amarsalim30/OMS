@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("orderDate"),
        Index("customerId"),
        Index(value = ["orderDate", "createdAt", "id"]),
        Index(value = ["customerId", "orderDate", "createdAt", "id"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderDate: LocalDate,

    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    val pickupTime: String? = null,

    val status: OrderStatus = OrderStatus.PENDING,
    val statusOverride: OrderStatusOverride? = null,

    val totalAmount: BigDecimal,

    val customerId: Long? = null
)

/**
 * Domain model for an order with its items.
 */
data class OrderWithItems(
    @Embedded
    val order: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

enum class OrderStatusOverride {
    OPEN,
    CLOSED
}
