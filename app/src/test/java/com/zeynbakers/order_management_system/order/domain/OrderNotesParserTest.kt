package com.zeynbakers.order_management_system.order.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderNotesParserTest {

    @Test
    fun `parses simple quantity suffix`() {
        val parsed = parseOrderNotes("Bread 2")
        assertEquals(listOf(OrderLineItem(name = "Bread", quantity = BigDecimal("2"))), parsed.items)
        assertEquals(emptyList<String>(), parsed.unparsed)
    }

    @Test
    fun `parses simple quantity prefix`() {
        val parsed = parseOrderNotes("3 Scones")
        assertEquals(listOf(OrderLineItem(name = "Scones", quantity = BigDecimal("3"))), parsed.items)
    }

    @Test
    fun `splits on newlines commas and semicolons`() {
        val parsed = parseOrderNotes("Bread 2, Scones 3;\nMuffins x4")
        assertEquals(
            listOf(
                OrderLineItem("Bread", BigDecimal("2")),
                OrderLineItem("Scones", BigDecimal("3")),
                OrderLineItem("Muffins", BigDecimal("4"))
            ),
            parsed.items
        )
    }

    @Test
    fun `aggregates case-insensitively`() {
        val totals =
            aggregateLineItems(
                listOf(
                    "Bread 2\nScones 3",
                    "bread 1, Scones 2"
                )
            )
        assertEquals(
            listOf(
                OrderLineItem("Bread", BigDecimal("3")),
                OrderLineItem("Scones", BigDecimal("5"))
            ),
            totals
        )
    }

    @Test
    fun `filters currency-like lines as unparsed`() {
        val parsed = parseOrderNotes("KES 1000\nBread 2")
        assertEquals(listOf(OrderLineItem("Bread", BigDecimal("2"))), parsed.items)
        assertEquals(listOf("KES 1000"), parsed.unparsed)
    }
}

