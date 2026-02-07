package com.zeynbakers.order_management_system.order.ui

import java.math.BigDecimal
import java.util.Calendar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal fun buildMonthGrid(
    year: Int,
    month: Int,
    today: LocalDate,
    orderData: Map<LocalDate, CalendarDayUi>,
    weekStart: Int
): List<CalendarDayUi> {
    val start = LocalDate(year, month, 1)
    val daysInMonth = daysInMonth(year, month)
    val endOfMonth = LocalDate(year, month, daysInMonth)
    val firstDayIndex = weekStart
    val startIndex = calendarDayIndex(start)
    val endIndex = calendarDayIndex(endOfMonth)
    val leadingDays = (startIndex - firstDayIndex + 7) % 7
    val trailingDays = (6 - ((endIndex - firstDayIndex + 7) % 7) + 7) % 7
    val gridStart = start.plus(-leadingDays, DateTimeUnit.DAY)

    val totalDays = leadingDays + daysInMonth + trailingDays
    return (0 until totalDays).map { offset ->
        val date = gridStart.plus(offset, DateTimeUnit.DAY)
        val existing = orderData[date]
        if (existing != null) {
            existing.copy(isToday = date == today, isInCurrentMonth = date.monthNumber == month)
        } else {
            CalendarDayUi(
                date = date,
                orderCount = 0,
                totalAmount = BigDecimal.ZERO,
                isToday = date == today,
                isInCurrentMonth = date.monthNumber == month,
                paymentState = null
            )
        }
    }
}

private fun calendarDayIndex(date: LocalDate): Int {
    return when (date.dayOfWeek.ordinal) {
        0 -> Calendar.MONDAY
        1 -> Calendar.TUESDAY
        2 -> Calendar.WEDNESDAY
        3 -> Calendar.THURSDAY
        4 -> Calendar.FRIDAY
        5 -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
    }
}

internal fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    if (month == 0 || year == 0) return Pair(year, month)
    val total = (year * 12) + (month - 1) + delta
    val newYear = total / 12
    val newMonth = (total % 12) + 1
    return Pair(newYear, newMonth)
}

internal fun monthOffset(anchorYear: Int, anchorMonth: Int, targetYear: Int, targetMonth: Int): Int {
    val anchorTotal = (anchorYear * 12) + (anchorMonth - 1)
    val targetTotal = (targetYear * 12) + (targetMonth - 1)
    return targetTotal - anchorTotal
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

internal fun formatMonthTitle(year: Int, month: Int): String {
    if (month !in 1..12 || year <= 0) return "Loading..."
    val monthName =
        when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Month"
        }
    return "$monthName $year"
}
