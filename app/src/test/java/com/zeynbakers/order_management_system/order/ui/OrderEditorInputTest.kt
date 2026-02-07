package com.zeynbakers.order_management_system.order.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderEditorInputTest {

    @Test
    fun `sanitizeAmountInput keeps first decimal point and strips other chars`() {
        assertEquals("12.34", sanitizeAmountInput("12.34"))
        assertEquals("12.34", sanitizeAmountInput("12..3a4"))
        assertEquals("12", sanitizeAmountInput("a1b2"))
        assertEquals(".5", sanitizeAmountInput(".5"))
    }

    @Test
    fun `sanitizePickupTimeInput keeps time chars and limits length`() {
        assertEquals("09:30", sanitizePickupTimeInput("09:30"))
        assertEquals("9.30", sanitizePickupTimeInput("9.30pm"))
        assertEquals("12345", sanitizePickupTimeInput("123456"))
    }
}

