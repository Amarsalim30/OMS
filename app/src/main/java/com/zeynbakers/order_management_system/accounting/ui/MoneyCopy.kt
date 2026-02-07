package com.zeynbakers.order_management_system.accounting.ui

import androidx.annotation.StringRes
import com.zeynbakers.order_management_system.R

object MoneyCopy {
    @StringRes
    fun paymentHistoryTitle(filter: PaymentHistoryFilter): Int {
        return when (filter) {
            PaymentHistoryFilter.All -> R.string.money_payment_history_all
            is PaymentHistoryFilter.Customer -> R.string.money_payment_history_customer
            is PaymentHistoryFilter.Order -> R.string.money_payment_history_order
        }
    }
}
