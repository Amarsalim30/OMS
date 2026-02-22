@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
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
    indices = [Index("orderDate"), Index("customerId")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderDate: LocalDate,

    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    val notes: String,

    val pickupTime: String? = null,

    val status: OrderStatus = OrderStatus.PENDING,
    val statusOverride: OrderStatusOverride? = null,

    val totalAmount: BigDecimal,

    val customerId: Long? = null
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
