package com.zeynbakers.order_management_system.accounting.ui

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentIntakeHistoryViewModelTest {

    @Test
    fun `historyLoadErrorMessageOrNull returns null for cancellation`() {
        assertNull(historyLoadErrorMessageOrNull(CancellationException("cancelled"), "fallback"))
    }

    @Test
    fun `historyLoadErrorMessageOrNull returns throwable message for errors`() {
        assertEquals("boom", historyLoadErrorMessageOrNull(IllegalStateException("boom"), "fallback"))
    }

    @Test
    fun `historyLoadErrorMessageOrNull returns fallback message when missing`() {
        assertEquals(
            "fallback",
            historyLoadErrorMessageOrNull(IllegalStateException(), "fallback")
        )
    }
}
