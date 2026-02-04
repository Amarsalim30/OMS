@file:Suppress("unused")

package com.zeynbakers.order_management_system.core.db

import androidx.room.*
import com.zeynbakers.order_management_system.accounting.data.*
import com.zeynbakers.order_management_system.customer.data.CustomerDao
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.*

@Database(
        entities =
                [
                        OrderEntity::class,
                        OrderItemEntity::class,
                        CustomerEntity::class,
                        PaymentEntity::class,
                        AccountEntryEntity::class,
                        PaymentReceiptEntity::class,
                        PaymentAllocationEntity::class],
        version = 9,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun accountingDao(): AccountingDao
    abstract fun paymentReceiptDao(): PaymentReceiptDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
}
