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
        tableName = "account_entries",
        foreignKeys =
                [
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
                        )],
        indices =
                [
                        Index("orderId"),
                        Index("customerId"),
                        Index("date"),
                        Index(value = ["orderId", "type", "date", "id"]),
                        Index(value = ["customerId", "date", "id"]),
                        Index(value = ["customerId", "orderId", "type", "date", "id"])
                ]
)
data class AccountEntryEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val orderId: Long? = null,
        val customerId: Long? = null,
        val type: EntryType,
        val amount: BigDecimal,
        val date: Long,
        val description: String
)

enum class EntryType {
    DEBIT,
    CREDIT,
    WRITE_OFF,
    REVERSAL
}
