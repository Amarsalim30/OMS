@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "payment_receipts",
    indices = [
        Index(value = ["transactionCode"], unique = true),
        Index(value = ["hash"], unique = true),
        Index("customerId"),
        Index("receivedAt")
    ]
)
data class PaymentReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: BigDecimal,
    val receivedAt: Long,
    val method: PaymentMethod,
    val transactionCode: String?,
    val hash: String?,
    val senderName: String?,
    val senderPhone: String?,
    val rawText: String?,
    val customerId: Long?,
    val note: String?,
    val status: PaymentReceiptStatus,
    val createdAt: Long,
    val voidedAt: Long?,
    val voidReason: String?
)

enum class PaymentReceiptStatus {
    UNAPPLIED,
    PARTIAL,
    APPLIED,
    VOIDED
}
