@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.domain.LedgerSummary
import com.zeynbakers.order_management_system.core.db.AppDatabase
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CustomerViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val orderDao = database.orderDao()

    fun loadLedger(customerId: Long, onResult: (LedgerSummary) -> Unit) {
        viewModelScope.launch {
            val billed = orderDao.totalBilled(customerId)
            val paid = orderDao.totalPaid(customerId)

            onResult(
                LedgerSummary(
                    customerId = customerId,
                    totalBilled = billed ?: BigDecimal.ZERO,
                    totalPaid = paid ?: BigDecimal.ZERO,
                    outstanding = (billed ?: BigDecimal.ZERO) - (paid ?: BigDecimal.ZERO)
                )
            )
        }
    }
}
