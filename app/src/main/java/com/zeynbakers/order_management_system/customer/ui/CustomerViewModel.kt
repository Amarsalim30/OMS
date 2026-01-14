@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.domain.LedgerSummary
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.core.db.AppDatabase
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CustomerViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val accountingDao: AccountingDao = database.accountingDao()

    fun loadLedger(customerId: Long, onResult: (LedgerSummary) -> Unit) {
        viewModelScope.launch {
            val totals = accountingDao.getLedgerTotals(customerId)
            val billed = totals.billed ?: BigDecimal.ZERO
            val paid = totals.paid ?: BigDecimal.ZERO

            onResult(
                LedgerSummary(
                    customerId = customerId,
                    totalBilled = billed,
                    totalPaid = paid,
                    outstanding = billed - paid
                )
            )
        }
    }
}
