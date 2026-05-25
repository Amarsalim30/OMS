package com.zeynbakers.order_management_system.order.printing

import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {

    @Test
    fun `formats receipt with parsed items total and thank you`() {
        val order =
            OrderEntity(
                id = 42L,
                orderDate = LocalDate(2026, 5, 25),
                notes = "Bread 2, Scones 3",
                totalAmount = BigDecimal("500"),
                pickupTime = "09:00"
            )

        val receipt = ReceiptFormatter.formatOrder("Zeyn Bakers", order, "Jane Doe")

        assertTrue(receipt.contains("ZEYN BAKERS"))
        assertTrue(receipt.contains("Order #42"))
        assertTrue(receipt.contains("2026-05-25"))
        assertTrue(receipt.contains("Jane Doe"))
        assertTrue(receipt.contains("Bread x2"))
        assertTrue(receipt.contains("Scones x3"))
        assertTrue(receipt.contains("TOTAL: KES 500"))
        assertTrue(receipt.contains("Thank you"))
    }

    @Test
    fun `falls back to notes when no parsed items`() {
        val order =
            OrderEntity(
                id = 7L,
                orderDate = LocalDate(2026, 5, 25),
                notes = "Custom cake order",
                totalAmount = BigDecimal("1200")
            )

        val receipt = ReceiptFormatter.formatOrder("Store", order, null)

        assertTrue(receipt.contains("Custom cake order"))
        assertTrue(receipt.contains("TOTAL: KES 1,200") || receipt.contains("TOTAL: KES 1200"))
    }
}
