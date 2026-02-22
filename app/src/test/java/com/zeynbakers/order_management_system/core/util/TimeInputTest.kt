package com.zeynbakers.order_management_system.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeInputTest {

    @Test
    fun `formatHourMinuteAmPm converts 24-hour values to am pm`() {
        assertEquals("12:00 AM", formatHourMinuteAmPm(0, 0))
        assertEquals("12:00 PM", formatHourMinuteAmPm(12, 0))
        assertEquals("3:05 PM", formatHourMinuteAmPm(15, 5))
        assertEquals("9:30 AM", formatHourMinuteAmPm(9, 30))
    }

    @Test
    fun `formatPickupTimeForDisplay formats parsed value and falls back`() {
        assertEquals("9:30 AM", formatPickupTimeForDisplay("930"))
        assertEquals("custom time", formatPickupTimeForDisplay("custom time"))
        assertNull(formatPickupTimeForDisplay(null))
    }
}
