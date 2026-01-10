package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.domain.LedgerSummary
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CustomerViewModel(
    private val db: DatabaseProvider
) : ViewModel() {

    private val orderDao = db.getDatabase().orderDao()

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
