@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.domain.LedgerSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CustomerLedgerViewModel(private val accountingDao: AccountingDao) : ViewModel() {

    private val _ledger = MutableStateFlow<LedgerSummary?>(null)
    val ledger = _ledger.asStateFlow()

    fun loadLedger(customerId: Long) {
        viewModelScope.launch {
            val totals = accountingDao.getLedgerTotals(customerId)
            val billed = totals.billed ?: BigDecimal.ZERO
            val paid = totals.paid ?: BigDecimal.ZERO

            _ledger.value = LedgerSummary(customerId, billed, paid, billed - paid)
        }
    }
}
