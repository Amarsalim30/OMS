package com.zeynbakers.order_management_system.order.ui.common

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

enum class PaymentState {
    UNPAID,
    PARTIAL,
    PAID,
    OVERPAID
}

@Immutable
data class CalendarDayUi(
    val date: LocalDate,
    val orderCount: Int,
    val totalAmount: BigDecimal,
    val isToday: Boolean,
    val isInCurrentMonth: Boolean,
    val paymentState: PaymentState?,
    val orderStates: List<PaymentState> = emptyList()
)
