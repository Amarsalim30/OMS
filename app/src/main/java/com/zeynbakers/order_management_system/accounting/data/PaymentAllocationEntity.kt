@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("receiptId"),
        Index("orderId"),
        Index("customerId"),
        Index("accountEntryId")
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
