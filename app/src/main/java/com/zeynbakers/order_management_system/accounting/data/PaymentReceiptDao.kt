@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PaymentReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: PaymentReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(receipts: List<PaymentReceiptEntity>)

    @Update
    suspend fun update(receipt: PaymentReceiptEntity)

    @Query("SELECT * FROM payment_receipts WHERE id = :id")
    suspend fun getById(id: Long): PaymentReceiptEntity?

    @Query("SELECT * FROM payment_receipts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<PaymentReceiptEntity>

    @Query("SELECT * FROM payment_receipts ORDER BY receivedAt DESC, id DESC")
    suspend fun getAll(): List<PaymentReceiptEntity>

    @Query("SELECT * FROM payment_receipts WHERE customerId = :customerId ORDER BY receivedAt DESC, id DESC")
    suspend fun getByCustomerId(customerId: Long): List<PaymentReceiptEntity>

    @Query(
        """
        SELECT DISTINCT r.*
        FROM payment_receipts r
        INNER JOIN payment_allocations a ON a.receiptId = r.id
        WHERE a.orderId = :orderId
        ORDER BY r.receivedAt DESC, r.id DESC
        """
    )
    suspend fun getByOrderId(orderId: Long): List<PaymentReceiptEntity>

    @Query("SELECT * FROM payment_receipts WHERE transactionCode IN (:codes)")
    suspend fun getByCodes(codes: List<String>): List<PaymentReceiptEntity>

    @Query("SELECT * FROM payment_receipts WHERE hash IN (:hashes)")
    suspend fun getByHashes(hashes: List<String>): List<PaymentReceiptEntity>

    @Query("UPDATE payment_receipts SET status = :status, voidedAt = :voidedAt, voidReason = :voidReason WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PaymentReceiptStatus, voidedAt: Long?, voidReason: String?)
}
