package com.zeynbakers.order_management_system.order.ui

import kotlinx.datetime.LocalDate

data class CalendarDayUi(
    val date: LocalDate,
    val hasOrders: Boolean,
    val orderCount: Int
)
