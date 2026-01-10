package com.zeynbakers.order_management_system.accounting.data

import androidx.room.*
import java.math.BigDecimal

@Entity(
    tableName = "account_entries",
    indices = [Index("orderId"), Index("date")]
)
data class AccountEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val orderId: Long,
    val type: EntryType,
    val amount: BigDecimal,
    val date: Long,
    val description: String
)

enum class EntryType {
    INCOME,
    EXPENSE
}
