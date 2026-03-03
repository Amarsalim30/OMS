package com.zeynbakers.order_management_system.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatKes(amount: BigDecimal): String {
    val rounded = amount.setScale(0, RoundingMode.HALF_UP)
    val formatter = NumberFormat.getNumberInstance()
    return "KES ${formatter.format(rounded)}"
}

fun formatDateTime(epochMillis: Long): String {
    val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val timePart = formatHourMinuteAmPm(
        hour24 = calendar.get(Calendar.HOUR_OF_DAY),
        minute = calendar.get(Calendar.MINUTE)
    )
    return "${dateFormatter.format(Date(epochMillis))}, $timePart"
}
