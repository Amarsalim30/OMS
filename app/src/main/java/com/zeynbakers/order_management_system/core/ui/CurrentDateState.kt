package com.zeynbakers.order_management_system.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun rememberCurrentDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    var currentDate by remember(timeZone) {
        mutableStateOf(Clock.System.now().toLocalDateTime(timeZone).date)
    }

    LaunchedEffect(timeZone) {
        while (isActive) {
            val now = Clock.System.now()
            val today = now.toLocalDateTime(timeZone).date
            currentDate = today
            val waitMillis = nextDateRefreshDelayMillis(now.toEpochMilliseconds(), timeZone)
            delay(waitMillis)
        }
    }

    return currentDate
}

internal fun nextDateRefreshDelayMillis(nowEpochMillis: Long, timeZone: TimeZone): Long {
    val now = Instant.fromEpochMilliseconds(nowEpochMillis)
    val today = now.toLocalDateTime(timeZone).date
    val nextMidnight = today.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)
    return (nextMidnight.toEpochMilliseconds() - nowEpochMillis).coerceAtLeast(1L)
}
