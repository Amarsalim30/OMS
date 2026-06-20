package com.zeynbakers.order_management_system.order.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class OrderNotesParserTest {

    @Test
    fun `parse simple items`() {
        val notes = "2 bread, 3 milk"
        val parsed = parseOrderNotes(notes)

        assertEquals(2, parsed.items.size)
        assertEquals("bread", parsed.items[0].name)
        assertEquals(0, BigDecimal("2").compareTo(parsed.items[0].quantity))
        assertEquals("milk", parsed.items[1].name)
        assertEquals(0, BigDecimal("3").compareTo(parsed.items[1].quantity))
    }

    @Test
    fun `parse bulleted items with complex separators`() {
        val notes = """
            - 5 Croissants; 10 Meatpies
            * 2 Birthday Cakes, 1 Swiss Roll
        """.trimIndent()
        val parsed = parseOrderNotes(notes)

        assertEquals(4, parsed.items.size)
        assertEquals("Croissants", parsed.items[0].name)
        assertEquals("Meatpies", parsed.items[1].name)
        assertEquals("Birthday Cakes", parsed.items[2].name)
        assertEquals("Swiss Roll", parsed.items[3].name)
    }

    @Test
    fun `ignores currency and total lines`() {
        val notes = """
            2 bread
            Total: KSh 500
            Paid 200
        """.trimIndent()
        val parsed = parseOrderNotes(notes)

        assertEquals(1, parsed.items.size)
        assertEquals("bread", parsed.items[0].name)
        assertEquals(2, parsed.unparsed.size)
        assertEquals("Total: KSh 500", parsed.unparsed[0])
    }

    @Test
    fun `aggregate line items`() {
        val items = listOf(
            OrderLineItem("Bread", BigDecimal("2")),
            OrderLineItem("bread", BigDecimal("3")),
            OrderLineItem("Milk", BigDecimal("1"))
        )
        val aggregated = aggregateOrderLineItems(items)

        assertEquals(2, aggregated.size)
        assertEquals("Bread", aggregated[0].name)
        assertEquals(0, BigDecimal("5").compareTo(aggregated[0].quantity))
        assertEquals("Milk", aggregated[1].name)
    }
}
