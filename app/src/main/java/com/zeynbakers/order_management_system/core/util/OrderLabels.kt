package com.zeynbakers.order_management_system.core.util

import java.math.BigDecimal
import kotlinx.datetime.LocalDate

fun formatOrderLabel(
    date: LocalDate?,
    customerName: String?,
    notes: String?,
    totalAmount: BigDecimal? = null,
    maxNotes: Int = 24
): String {
    val parts = mutableListOf<String>()
    if (date != null) {
        parts.add(formatShortDate(date))
    }
    val customer = customerName?.trim().takeIf { !it.isNullOrBlank() }
    if (customer != null) {
        parts.add(customer)
    }
    val note = notes?.trim().takeIf { !it.isNullOrBlank() }
    if (note != null) {
        val trimmed =
            if (note.length > maxNotes) {
                "${note.take(maxNotes).trim()}..."
            } else {
                note
            }
        parts.add(trimmed)
    }
    if (totalAmount != null) {
        parts.add(formatKes(totalAmount))
    }
    return parts.joinToString(" - ").ifBlank { "Order" }
}

fun formatOrderLabelWithId(
    orderId: Long?,
    date: LocalDate?,
    customerName: String?,
    notes: String?,
    totalAmount: BigDecimal? = null,
    maxNotes: Int = 24
): String {
    val base = formatOrderLabel(date, customerName, notes, totalAmount, maxNotes)
    return if (orderId != null) "$base (ID $orderId)" else base
}

fun formatShortDate(date: LocalDate): String {
    val month =
        when (date.monthNumber) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Month"
        }
    return "$month ${date.dayOfMonth}"
}
