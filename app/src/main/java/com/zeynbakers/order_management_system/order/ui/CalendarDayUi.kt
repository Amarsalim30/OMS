package com.zeynbakers.order_management_system.order.ui

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

enum class PaymentState {
    UNPAID,
    PARTIAL,
    PAID,
    OVERPAID
}

data class CalendarDayUi(
    val date: LocalDate,
    val orderCount: Int,
    val totalAmount: BigDecimal,
    val isToday: Boolean,
    val isInCurrentMonth: Boolean,
    val paymentState: PaymentState?
)
