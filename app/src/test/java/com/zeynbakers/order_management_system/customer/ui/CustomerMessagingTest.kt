package com.zeynbakers.order_management_system.customer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerMessagingTest {

    @Test
    fun `resolveMessagingNumber returns normalized e164 when possible`() {
        assertEquals("+254712345678", resolveMessagingNumber("0712345678"))
    }

    @Test
    fun `resolveMessagingNumber returns null for no digits`() {
        assertNull(resolveMessagingNumber("abc()"))
    }

    @Test
    fun `whatsappDigits strips non digit characters`() {
        assertEquals("254712345678", whatsappDigits("+254 712 345 678"))
    }
}
