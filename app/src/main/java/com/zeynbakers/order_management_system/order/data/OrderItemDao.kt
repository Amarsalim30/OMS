@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsFlow(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE productId = :productId")
    suspend fun getOrderItemsByProduct(productId: Long): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OrderItemEntity): Long

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Long)

    @Query("DELETE FROM order_items WHERE id = :itemId")
    suspend fun deleteOrderItem(itemId: Long)

    @Query("UPDATE order_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateOrderItemQuantity(itemId: Long, quantity: Int)

    @Query("UPDATE order_items SET priceOverride = :priceOverride WHERE id = :itemId")
    suspend fun updateOrderItemPrice(itemId: Long, priceOverride: java.math.BigDecimal?)

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderWithItems(orderId: Long): OrderWithItems?

    @Query("""
        SELECT oi.*, p.name as productName, p.emoji as productEmoji, p.default_price as productDefaultPrice
        FROM order_items oi 
        LEFT JOIN products p ON oi.productId = p.id 
        WHERE oi.orderId = :orderId
    """)
    suspend fun getOrderItemsWithProducts(orderId: Long): List<OrderItemWithProduct>
}