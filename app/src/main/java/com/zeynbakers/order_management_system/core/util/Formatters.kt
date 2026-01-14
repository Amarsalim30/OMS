package com.zeynbakers.order_management_system.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatKes(amount: BigDecimal): String {
    val rounded = amount.setScale(0, RoundingMode.HALF_UP)
    val formatter = NumberFormat.getNumberInstance()
    return "KES ${formatter.format(rounded)}"
}

fun formatDateTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
