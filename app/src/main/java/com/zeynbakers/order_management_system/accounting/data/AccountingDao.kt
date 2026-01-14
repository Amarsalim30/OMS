@file:Suppress("unused")

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

    @Query("DELETE FROM account_entries WHERE orderId = :orderId AND type = 'DEBIT'")
    suspend fun deleteDebitEntriesForOrder(orderId: Long)

    @Transaction
    suspend fun upsertDebitForOrder(
        orderId: Long,
        customerId: Long?,
        amount: BigDecimal,
        date: Long,
        description: String
    ) {
        deleteDebitEntriesForOrder(orderId)
        insertAccountEntry(
            AccountEntryEntity(
                orderId = orderId,
                customerId = customerId,
                type = EntryType.DEBIT,
                amount = amount,
                date = date,
                description = description
            )
        )
    }

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0)
        FROM account_entries
        WHERE orderId = :orderId
        """
    )
    suspend fun getPaidForOrder(orderId: Long): BigDecimal

    @Query("SELECT * FROM account_entries WHERE customerId = :customerId ORDER BY date DESC")
    suspend fun getLedgerForCustomer(customerId: Long): List<AccountEntryEntity>

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) -
               IFNULL(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0)
        FROM account_entries
        WHERE customerId = :customerId
        """
    )
    suspend fun getCustomerBalance(customerId: Long): BigDecimal

    @Query(
        """
        SELECT
            c.id as customerId,
            c.name as name,
            c.phone as phone,
            IFNULL(SUM(CASE WHEN a.type = 'DEBIT' THEN a.amount ELSE 0 END), 0) as billed,
            IFNULL(SUM(CASE WHEN a.type = 'CREDIT' THEN a.amount ELSE 0 END), 0) as paid,
            IFNULL(SUM(CASE WHEN a.type = 'DEBIT' THEN a.amount ELSE 0 END), 0) -
            IFNULL(SUM(CASE WHEN a.type = 'CREDIT' THEN a.amount ELSE 0 END), 0) as balance
        FROM customers c
        LEFT JOIN account_entries a ON a.customerId = c.id
        WHERE c.name LIKE :query OR c.phone LIKE :query
        GROUP BY c.id
        ORDER BY c.name
        """
    )
    suspend fun getCustomerAccountSummaries(query: String): List<CustomerAccountSummary>

    @Query(
        """
        SELECT
            orderId as orderId,
            IFNULL(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) as paid
        FROM account_entries
        WHERE orderId IN (:orderIds)
        GROUP BY orderId
        """
    )
    suspend fun getPaidForOrders(orderIds: List<Long>): List<OrderPaymentSummary>

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
