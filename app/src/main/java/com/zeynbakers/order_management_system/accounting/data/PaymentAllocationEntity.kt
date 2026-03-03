@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal

@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountEntryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["reversalEntryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("receiptId"),
        Index("orderId"),
        Index("customerId"),
        Index("accountEntryId"),
        Index("reversalEntryId")
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val orderId: Long?,
    val customerId: Long?,
    val amount: BigDecimal,
    val type: PaymentAllocationType,
    val status: PaymentAllocationStatus,
    val accountEntryId: Long?,
    val reversalEntryId: Long?,
    val createdAt: Long,
    val voidedAt: Long?,
    val voidReason: String?
)

enum class PaymentAllocationType {
    ORDER,
    CUSTOMER_CREDIT
}

enum class PaymentAllocationStatus {
    APPLIED,
    VOIDED
}
