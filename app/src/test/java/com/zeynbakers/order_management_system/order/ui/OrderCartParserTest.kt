package com.zeynbakers.order_management_system.order.ui

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCartParserTest {

    @Test
    fun `serializes and parses cart lines with unit price`() {
        val items =
            listOf(
                CartItem(emoji = "🥧", name = "Meatpie", quantity = 2, unitPrice = BigDecimal("500")),
                CartItem(emoji = "🍞", name = "Bread", quantity = 1, unitPrice = BigDecimal("80"))
            )
        val notes = OrderCartParser.serializeCartToNotes(items)
        val parsed = OrderCartParser.parseNotesToCart(notes)

        assertEquals(2, parsed.size)
        assertEquals(0, BigDecimal("1080").compareTo(OrderCartParser.cartTotal(parsed)))
        assertTrue(notes.contains("Meatpie x 2 @ 500"))
    }

    @Test
    fun `parses legacy lines without price`() {
        val parsed = OrderCartParser.parseNotesToCart("Bread x 2")
        assertEquals(1, parsed.size)
        assertEquals("Bread", parsed[0].name)
        assertEquals(2, parsed[0].quantity)
        assertEquals(BigDecimal.ZERO, parsed[0].unitPrice)
    }
}
