package com.zeynbakers.order_management_system.accounting.data

import androidx.room.*
import java.math.BigDecimal

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = com.yourcompany.orderapp.order.data.OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,
    val amount: BigDecimal,
    val method: PaymentMethod,
    val paidAt: Long
)

enum class PaymentMethod {
    CASH,
    MPESA
}

