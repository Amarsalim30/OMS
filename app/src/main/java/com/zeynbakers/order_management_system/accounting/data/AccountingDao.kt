package com.zeynbakers.order_management_system.accounting.data

import androidx.room.*
import java.math.BigDecimal

@Dao
interface AccountingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountEntry(entry: AccountEntryEntity): Long

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    suspend fun getPaymentsForOrder(orderId: Long): List<PaymentEntity>

    @Query("SELECT * FROM account_entries WHERE orderId = :orderId")
    suspend fun getEntriesForOrder(orderId: Long): List<AccountEntryEntity>

    @Query("""
        SELECT 
            IFNULL(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) as billed,
            IFNULL(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) as paid
        FROM account_entries
        WHERE customerId = :customerId
    """)
    suspend fun getLedgerTotals(customerId: Long): LedgerTotals
}

data class LedgerTotals(
    val billed: BigDecimal?,
    val paid: BigDecimal?
)
