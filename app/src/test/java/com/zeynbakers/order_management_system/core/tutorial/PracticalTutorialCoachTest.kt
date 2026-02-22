package com.zeynbakers.order_management_system.core.tutorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticalTutorialCoachTest {
    @Test
    fun routeMatchesPrefix_handles_exact_nested_and_query_routes() {
        assertTrue(routeMatchesPrefix("money", "money"))
        assertTrue(routeMatchesPrefix("payment_history/all?focusReceiptId={focusReceiptId}", "payment_history/all"))
        assertTrue(routeMatchesPrefix("customer/12/statement", "customer"))
        assertFalse(routeMatchesPrefix("orders", "customers"))
        assertFalse(routeMatchesPrefix(null, "calendar"))
    }

    @Test
    fun practicalTutorialSteps_has_expected_terminal_summary_step() {
        val steps = practicalTutorialSteps()

        assertTrue(steps.size >= 12)
        assertEquals(PracticalTutorialAction.OpenCalendar, steps.first().primaryAction)
        assertNull(steps.last().routePrefix)
        assertEquals(PracticalTutorialAction.None, steps.last().primaryAction)
    }
}

