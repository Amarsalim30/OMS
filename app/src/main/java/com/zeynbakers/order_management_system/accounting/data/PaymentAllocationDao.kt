@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PaymentAllocationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(allocation: PaymentAllocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(allocations: List<PaymentAllocationEntity>)

    @Query("SELECT * FROM payment_allocations WHERE receiptId = :receiptId ORDER BY createdAt ASC, id ASC")
    suspend fun getByReceiptId(receiptId: Long): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE receiptId IN (:receiptIds)")
    suspend fun getByReceiptIds(receiptIds: List<Long>): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations WHERE orderId = :orderId ORDER BY createdAt DESC, id DESC")
    suspend fun getByOrderId(orderId: Long): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocations")
    suspend fun getAll(): List<PaymentAllocationEntity>

    @Query(
        """
        UPDATE payment_allocations
        SET status = :status,
            reversalEntryId = :reversalEntryId,
            voidedAt = :voidedAt,
            voidReason = :voidReason
        WHERE id = :id
        """
    )
    suspend fun markVoided(
        id: Long,
        status: PaymentAllocationStatus,
        reversalEntryId: Long?,
        voidedAt: Long?,
        voidReason: String?
    )
}
