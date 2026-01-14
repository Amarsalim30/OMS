package com.zeynbakers.order_management_system.order.ui

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class CalendarDayUi(
    val date: LocalDate,
    val orderCount: Int,
    val totalAmount: BigDecimal,
    val isToday: Boolean
)
