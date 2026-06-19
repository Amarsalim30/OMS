@file:Suppress("unused")

package com.zeynbakers.order_management_system.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationDao
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptDao
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptEntity
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteDao
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteEntity
import com.zeynbakers.order_management_system.customer.data.CustomerDao
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemDao
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.product.data.ProductDao
import com.zeynbakers.order_management_system.product.data.ProductEntity

const val APP_DATABASE_SCHEMA_VERSION = 17

@Database(
        entities =
                [
                        OrderEntity::class,
                        OrderItemEntity::class,
                        CustomerEntity::class,
                        PaymentEntity::class,
                        AccountEntryEntity::class,
                        PaymentReceiptEntity::class,
                        PaymentAllocationEntity::class,
                        HelperNoteEntity::class,
                        ProductEntity::class],
        version = APP_DATABASE_SCHEMA_VERSION,
        exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun accountingDao(): AccountingDao
    abstract fun paymentReceiptDao(): PaymentReceiptDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun helperNoteDao(): HelperNoteDao
    abstract fun productDao(): ProductDao
}
