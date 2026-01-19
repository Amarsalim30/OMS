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

    @Query("SELECT * FROM account_entries WHERE orderId = :orderId AND type = 'CREDIT' ORDER BY date ASC, id ASC")
    suspend fun getCreditEntriesForOrder(orderId: Long): List<AccountEntryEntity>

    @Query(
        """
        SELECT * FROM account_entries
        WHERE customerId = :customerId
        AND orderId IS NULL
        AND type = 'CREDIT'
        ORDER BY date ASC, id ASC
        """
    )
    suspend fun getCustomerUnassignedCredits(customerId: Long): List<AccountEntryEntity>

    @Query("SELECT * FROM account_entries WHERE orderId = :orderId AND type = :type ORDER BY date DESC, id DESC")
    suspend fun getEntriesForOrderByType(orderId: Long, type: EntryType): List<AccountEntryEntity>

    @Query("UPDATE account_entries SET customerId = :customerId WHERE orderId = :orderId")
    suspend fun updateCustomerIdForOrderEntries(orderId: Long, customerId: Long?)

    @Query("UPDATE account_entries SET orderId = :orderId, description = :description WHERE id = :id")
    suspend fun updateAccountEntryOrderIdAndDescription(id: Long, orderId: Long?, description: String)

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0)
        FROM account_entries
        WHERE orderId = :orderId
        AND type = 'CREDIT'
        """
    )
    suspend fun getCreditTotalForOrder(orderId: Long): BigDecimal

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0)
        FROM account_entries
        WHERE orderId = :orderId
        AND type = 'WRITE_OFF'
        """
    )
    suspend fun getWriteOffTotalForOrder(orderId: Long): BigDecimal

    @Query("UPDATE account_entries SET amount = :amount WHERE id = :id")
    suspend fun updateAccountEntryAmount(id: Long, amount: BigDecimal)

    @Query("DELETE FROM account_entries WHERE id = :id")
    suspend fun deleteAccountEntryById(id: Long)

    @Query("DELETE FROM account_entries WHERE orderId = :orderId AND type = 'DEBIT'")
    suspend fun deleteDebitEntriesForOrder(orderId: Long)

    @Query("DELETE FROM account_entries WHERE orderId = :orderId AND type = 'WRITE_OFF'")
    suspend fun deleteWriteOffEntriesForOrder(orderId: Long)

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

    @Transaction
    suspend fun applyAvailableCustomerCreditToOrder(
        orderId: Long,
        customerId: Long,
        orderTotal: BigDecimal,
        now: Long
    ) {
        if (orderTotal <= BigDecimal.ZERO) return

        val alreadyPaid = getPaidForOrder(orderId)
        var remaining = orderTotal - alreadyPaid
        if (remaining <= BigDecimal.ZERO) return

        val credits = getCustomerUnassignedCredits(customerId)
        for (entry in credits) {
            if (remaining <= BigDecimal.ZERO) break
            if (entry.amount <= BigDecimal.ZERO) continue

            val applyAmount = if (entry.amount > remaining) remaining else entry.amount
            val appliedDescription = "Applied credit from entry #${entry.id} to Order #$orderId"

            if (applyAmount.compareTo(entry.amount) == 0) {
                updateAccountEntryOrderIdAndDescription(entry.id, orderId, appliedDescription)
                remaining -= entry.amount
            } else {
                updateAccountEntryAmount(entry.id, entry.amount - applyAmount)
                insertAccountEntry(
                    AccountEntryEntity(
                        orderId = orderId,
                        customerId = customerId,
                        type = EntryType.CREDIT,
                        amount = applyAmount,
                        date = now,
                        description = appliedDescription
                    )
                )
                remaining -= applyAmount
            }
        }
    }

    @Transaction
    suspend fun moveOrderCreditsToCustomerLevel(orderId: Long) {
        val credits = getCreditEntriesForOrder(orderId)
        for (entry in credits) {
            val description = "Credit from cancelled Order #$orderId: ${entry.description}"
            updateAccountEntryOrderIdAndDescription(entry.id, null, description)
        }
    }

    /**
     * Keeps order-level credits/write-offs within the order total after edits.
     *
     * Assumption (from UI behavior): if an order total is reduced below already-applied payments,
     * the excess CREDIT becomes customer-level "extra payment" (orderId = null), while excess
     * WRITE_OFF is simply reduced (it is not money and should not create customer credit).
     */
    @Transaction
    suspend fun reconcileOrderSettlementToTotal(
        orderId: Long,
        customerId: Long?,
        orderTotal: BigDecimal,
        now: Long
    ) {
        if (orderTotal < BigDecimal.ZERO) return

        val creditTotal = getCreditTotalForOrder(orderId)
        val targetCredit = if (creditTotal > orderTotal) orderTotal else creditTotal
        val extraCredit = creditTotal - targetCredit
        if (extraCredit > BigDecimal.ZERO) {
            reduceEntriesBy(
                entries = getEntriesForOrderByType(orderId, EntryType.CREDIT),
                reduceBy = extraCredit
            )
            if (customerId != null) {
                insertAccountEntry(
                    AccountEntryEntity(
                        orderId = null,
                        customerId = customerId,
                        type = EntryType.CREDIT,
                        amount = extraCredit,
                        date = now,
                        description = "Extra payment moved from Order #$orderId"
                    )
                )
            }
        }

        val writeOffTotal = getWriteOffTotalForOrder(orderId)
        val remainingAfterCredits = orderTotal - targetCredit
        val targetWriteOff = if (writeOffTotal > remainingAfterCredits) remainingAfterCredits else writeOffTotal
        val extraWriteOff = writeOffTotal - targetWriteOff
        if (extraWriteOff > BigDecimal.ZERO) {
            reduceEntriesBy(
                entries = getEntriesForOrderByType(orderId, EntryType.WRITE_OFF),
                reduceBy = extraWriteOff
            )
        }
    }

    private suspend fun reduceEntriesBy(entries: List<AccountEntryEntity>, reduceBy: BigDecimal) {
        var remaining = reduceBy
        for (entry in entries) {
            if (remaining <= BigDecimal.ZERO) break
            if (entry.amount <= remaining) {
                deleteAccountEntryById(entry.id)
                remaining -= entry.amount
            } else {
                updateAccountEntryAmount(entry.id, entry.amount - remaining)
                remaining = BigDecimal.ZERO
            }
        }
    }

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type IN ('CREDIT', 'WRITE_OFF') THEN amount ELSE 0 END), 0)
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
               IFNULL(SUM(CASE WHEN type IN ('CREDIT', 'WRITE_OFF') THEN amount ELSE 0 END), 0)
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
            IFNULL(SUM(CASE WHEN a.type IN ('CREDIT', 'WRITE_OFF') THEN a.amount ELSE 0 END), 0) as paid,
            IFNULL(SUM(CASE WHEN a.type = 'DEBIT' THEN a.amount ELSE 0 END), 0) -
            IFNULL(SUM(CASE WHEN a.type IN ('CREDIT', 'WRITE_OFF') THEN a.amount ELSE 0 END), 0) as balance
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
            IFNULL(SUM(CASE WHEN type IN ('CREDIT', 'WRITE_OFF') THEN amount ELSE 0 END), 0) as paid
        FROM account_entries
        WHERE orderId IN (:orderIds)
        GROUP BY orderId
        """
    )
    suspend fun getPaidForOrders(orderIds: List<Long>): List<OrderPaymentSummary>

    @Query(
        """
        SELECT
            orderId as orderId,
            MAX(date) as lastPaymentAt
        FROM account_entries
        WHERE orderId IN (:orderIds)
        AND type IN ('CREDIT', 'WRITE_OFF')
        GROUP BY orderId
        """
    )
    suspend fun getLastPaymentDatesForOrders(orderIds: List<Long>): List<OrderPaymentActivity>

    @Query("""
        SELECT 
            IFNULL(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) as billed,
            IFNULL(SUM(CASE WHEN type IN ('CREDIT', 'WRITE_OFF') THEN amount ELSE 0 END), 0) as paid
        FROM account_entries
        WHERE customerId = :customerId
    """)
    suspend fun getLedgerTotals(customerId: Long): LedgerTotals
}

data class LedgerTotals(
    val billed: BigDecimal?,
    val paid: BigDecimal?
)
