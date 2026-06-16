@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.*
import java.math.BigDecimal

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Update
    suspend fun update(order: OrderEntity)

    @Delete
    suspend fun delete(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE orderDate = :date ORDER BY createdAt DESC")
    suspend fun getOrdersByDate(date: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerId = :customerId")
    suspend fun getOrdersByCustomer(customerId: Long): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders
        WHERE customerId = :customerId
          AND status != 'CANCELLED'
          AND (statusOverride IS NULL OR statusOverride != 'CLOSED')
        ORDER BY orderDate ASC, createdAt ASC, id ASC
        """
    )
    suspend fun getOpenOrdersByCustomer(customerId: Long): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders
        WHERE customerId = :customerId
        ORDER BY orderDate DESC, createdAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getOrdersByCustomerLimited(customerId: Long, limit: Int): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE customerId = :customerId")
    suspend fun countOrdersForCustomer(customerId: Long): Int

    @Query("SELECT * FROM orders")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE status != 'CANCELLED' ORDER BY orderDate DESC, createdAt DESC")
    suspend fun getActiveOrders(): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders
        WHERE status != 'CANCELLED'
          AND (statusOverride IS NULL OR statusOverride != 'CLOSED')
        ORDER BY orderDate DESC, createdAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getOpenOrdersLimited(limit: Int): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders
        WHERE customerId = :customerId
          AND status != 'CANCELLED'
          AND (statusOverride IS NULL OR statusOverride != 'CLOSED')
        ORDER BY orderDate DESC, createdAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getOpenOrdersByCustomerLimited(customerId: Long, limit: Int): List<OrderEntity>

    @Query(
        """
        SELECT * FROM orders
        WHERE customerId = :customerId
          AND status != 'CANCELLED'
          AND (statusOverride IS NULL OR statusOverride != 'CLOSED')
        ORDER BY orderDate ASC, createdAt ASC, id ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getOpenOrdersByCustomerPaged(customerId: Long, limit: Int, offset: Int): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE id IN (:orderIds)")
    suspend fun getOrdersByIds(orderIds: List<Long>): List<OrderEntity>

    @Query("UPDATE orders SET status = 'COMPLETED' WHERE id = :orderId")
    suspend fun markCompleted(orderId: Long)

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE id = :orderId")
    suspend fun markCancelled(orderId: Long)

    @Query("UPDATE orders SET statusOverride = :statusOverride, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateStatusOverride(orderId: Long, statusOverride: String?, updatedAt: Long)

    @Query("SELECT SUM(totalAmount) FROM orders WHERE customerId = :customerId")
    suspend fun getTotalBilled(customerId: Long): BigDecimal?

    @Query("SELECT IFNULL(SUM(totalAmount), 0) FROM orders WHERE orderDate = :date")
    suspend fun getTotalForDate(date: String): BigDecimal

    @Query(
        """
        SELECT IFNULL(SUM(totalAmount), 0)
        FROM orders
        WHERE orderDate >= :start
        AND orderDate < :end
        """
    )
    suspend fun getTotalBetween(start: String, end: String): BigDecimal

    @Query(
        """
        SELECT
            c.id as customerId,
            c.name as name,
            c.phone as phone,
            IFNULL(SUM(o.totalAmount), 0) as total
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.id
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    suspend fun getCustomerTotals(): List<CustomerTotal>

    @Query("""
    SELECT 
        IFNULL(SUM(totalAmount), 0) 
    FROM orders 
    WHERE customerId = :customerId 
    AND status != 'CANCELLED'
""")
suspend fun totalBilled(customerId: Long): BigDecimal

@Query("""
    SELECT * FROM orders
    WHERE orderDate >= :start
    AND orderDate < :end
""")
suspend fun getOrdersBetween(
    start: String,
    end: String
): List<OrderEntity>

}
