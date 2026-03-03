package com.zeynbakers.order_management_system

import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationHelpersTest {

    @Test
    fun `topLevelRouteFor maps day route to calendar`() {
        assertEquals(AppRoutes.Calendar, topLevelRouteFor("day/2026-02-26"))
    }

    @Test
    fun `topLevelRouteFor maps payment history route to money`() {
        assertEquals(AppRoutes.Money, topLevelRouteFor("payment_history/all"))
    }

    @Test
    fun `topLevelRouteFor returns null for unknown route`() {
        assertNull(topLevelRouteFor("unknown_route"))
    }

    @Test
    fun `shouldAppendSharedPaymentText returns true only for money collect route`() {
        assertTrue(
            shouldAppendSharedPaymentText(
                currentRoute = AppRoutes.Money,
                isMoneyCollectTab = true
            )
        )
        assertFalse(
            shouldAppendSharedPaymentText(
                currentRoute = AppRoutes.Money,
                isMoneyCollectTab = false
            )
        )
        assertFalse(
            shouldAppendSharedPaymentText(
                currentRoute = AppRoutes.Calendar,
                isMoneyCollectTab = true
            )
        )
    }

    @Test
    fun `sharedPaymentTextPreview normalizes windows newlines`() {
        assertEquals(
            "line1\nline2",
            sharedPaymentTextPreview("line1\r\nline2")
        )
    }

    @Test
    fun `sharedPaymentTextPreview truncates long text`() {
        val raw = "abcdefghijklmnopqrstuvwxyz"
        assertEquals(
            "abcdefg...",
            sharedPaymentTextPreview(raw, maxChars = 7)
        )
    }
}
