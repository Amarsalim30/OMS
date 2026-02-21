package com.zeynbakers.order_management_system.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.helper.ui.NotesHistoryViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.order.ui.OrderViewModel

class AppViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OrderViewModel::class.java) -> OrderViewModel(database) as T
            modelClass.isAssignableFrom(CustomerAccountsViewModel::class.java) -> CustomerAccountsViewModel(database) as T
            modelClass.isAssignableFrom(PaymentIntakeViewModel::class.java) -> PaymentIntakeViewModel(database) as T
            modelClass.isAssignableFrom(PaymentIntakeHistoryViewModel::class.java) -> PaymentIntakeHistoryViewModel(database) as T
            modelClass.isAssignableFrom(LedgerViewModel::class.java) -> LedgerViewModel(database) as T
            modelClass.isAssignableFrom(NotesHistoryViewModel::class.java) -> NotesHistoryViewModel(database) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
