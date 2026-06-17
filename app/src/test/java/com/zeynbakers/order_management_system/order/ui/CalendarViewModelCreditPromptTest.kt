package com.zeynbakers.order_management_system.order.ui.calendar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalendarViewModelCreditPromptTest {

    @Test
    fun `returns true when customer has credit and outstanding`() {
        val result =
            shouldPromptForAvailableCredit(
                customerId = 12L,
                availableCredit = BigDecimal("50"),
                outstandingAfterSave = BigDecimal("50")
            )
        assertTrue(result)
    }

    @Test
    fun `returns false when customer is null`() {
        val result =
            shouldPromptForAvailableCredit(
                customerId = null,
                availableCredit = BigDecimal("50"),
                outstandingAfterSave = BigDecimal("10")
            )
        assertFalse(result)
    }

    @Test
    fun `returns false when credit is zero or negative`() {
        assertFalse(
            shouldPromptForAvailableCredit(
                customerId = 1L,
                availableCredit = BigDecimal.ZERO,
                outstandingAfterSave = BigDecimal("10")
            )
        )
        assertFalse(
            shouldPromptForAvailableCredit(
                customerId = 1L,
                availableCredit = BigDecimal("-1"),
                outstandingAfterSave = BigDecimal("10")
            )
        )
    }

    @Test
    fun `returns false when order is fully covered`() {
        assertFalse(
            shouldPromptForAvailableCredit(
                customerId = 1L,
                availableCredit = BigDecimal("50"),
                outstandingAfterSave = BigDecimal.ZERO
            )
        )
        assertFalse(
            shouldPromptForAvailableCredit(
                customerId = 1L,
                availableCredit = BigDecimal("50"),
                outstandingAfterSave = BigDecimal("-5")
            )
        )
    }
}
