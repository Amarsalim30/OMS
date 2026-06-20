package com.zeynbakers.order_management_system.core.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.helper.ui.NotesHistoryViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.order.ui.calendar.CalendarViewModel
import com.zeynbakers.order_management_system.order.ui.day_detail.DayDetailViewModel
import com.zeynbakers.order_management_system.order.ui.summary.SummaryViewModel
import com.zeynbakers.order_management_system.product.ui.ProductViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AppViewModelFactory(
    private val database: AppDatabase,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(database) as T
            modelClass.isAssignableFrom(DayDetailViewModel::class.java) -> {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                DayDetailViewModel(database, now.date) as T
            }

            modelClass.isAssignableFrom(SummaryViewModel::class.java) -> SummaryViewModel(database) as T
            modelClass.isAssignableFrom(CustomerAccountsViewModel::class.java) ->
                CustomerAccountsViewModel(database, appContext) as T
            modelClass.isAssignableFrom(PaymentIntakeViewModel::class.java) ->
                PaymentIntakeViewModel(database, appContext) as T
            modelClass.isAssignableFrom(PaymentIntakeHistoryViewModel::class.java) ->
                PaymentIntakeHistoryViewModel(database, appContext) as T
            modelClass.isAssignableFrom(LedgerViewModel::class.java) -> LedgerViewModel(database) as T
            modelClass.isAssignableFrom(NotesHistoryViewModel::class.java) -> NotesHistoryViewModel(database) as T
            modelClass.isAssignableFrom(ProductViewModel::class.java) -> ProductViewModel(database) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
