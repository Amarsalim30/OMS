@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.domain

import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.EntryType
import java.math.BigDecimal
import kotlinx.datetime.Clock

class PaymentProcessor(
    private val accountingDao: AccountingDao
) {

    suspend fun recordPayment(
        customerId: Long,
        amount: BigDecimal,
        note: String?
    ) {
        accountingDao.insertAccountEntry(
            AccountEntryEntity(
                orderId = null,
                customerId = customerId,
                amount = amount,
                type = EntryType.CREDIT,
                date = Clock.System.now().toEpochMilliseconds(),
                description = note ?: "Payment"
            )
        )
    }
}
