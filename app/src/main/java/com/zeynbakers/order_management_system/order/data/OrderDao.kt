package com.zeynbakers.order_management_system.order.data

import androidx.room.*
import java.math.BigDecimal

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Update
    suspend fun update(order: OrderEntity)

    @Delete
    suspend fun delete(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE orderDate = :date")
    suspend fun getOrdersByDate(date: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE customerId = :customerId")
    suspend fun getOrdersByCustomer(customerId: Long): List<OrderEntity>

    @Query("SELECT SUM(totalAmount) FROM orders WHERE customerId = :customerId")
    suspend fun getTotalBilled(customerId: Long): BigDecimal?

    @Query("SELECT SUM(amountPaid) FROM orders WHERE customerId = :customerId")
    suspend fun getTotalPaid(customerId: Long): BigDecimal?

    @Query("""
    SELECT 
        IFNULL(SUM(totalAmount), 0) 
    FROM orders 
    WHERE customerId = :customerId 
    AND status != 'CANCELLED'
""")
suspend fun totalBilled(customerId: Long): BigDecimal

@Query("""
    SELECT 
        IFNULL(SUM(amountPaid), 0) 
    FROM orders 
    WHERE customerId = :customerId 
    AND status != 'CANCELLED'
""")
suspend fun totalPaid(customerId: Long): BigDecimal

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
