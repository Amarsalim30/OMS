package com.zeynbakers.order_management_system.order.printing

import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.order.data.ItemCategory
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {

    @Test
    fun `formats receipt with items total and thank you`() {
        val order =
            OrderEntity(
                id = 42L,
                orderDate = LocalDate(2026, 5, 25),
                totalAmount = BigDecimal("500"),
                pickupTime = "09:00"
            )
        val orderItems = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 42L,
                productId = null,
                productNameSnapshot = "Bread",
                unitPriceSnapshot = BigDecimal("100"),
                categorySnapshot = ItemCategory.BAKED,
                quantity = 2,
                priceOverride = null
            ),
            OrderItemEntity(
                id = 2L,
                orderId = 42L,
                productId = null,
                productNameSnapshot = "Scones",
                unitPriceSnapshot = BigDecimal("100"),
                categorySnapshot = ItemCategory.BAKED,
                quantity = 3,
                priceOverride = null
            )
        )

        val receipt = ReceiptFormatter.formatOrder("Zeyn Bakers", order, orderItems, "Jane Doe", null)

        assertTrue(receipt.contains("ZEYN BAKERS"))
        assertTrue(receipt.contains("Order #42"))
        assertTrue(receipt.contains("2026-05-25"))
        assertTrue(receipt.contains("Jane Doe"))
        assertTrue(receipt.contains("Bread x2"))
        assertTrue(receipt.contains("Scones x3"))
        assertTrue(receipt.contains("TOTAL: KSh 500"))
        assertTrue(receipt.contains("Thank you"))
    }

    @Test
    fun `formats receipt with empty items`() {
        val order =
            OrderEntity(
                id = 7L,
                orderDate = LocalDate(2026, 5, 25),
                totalAmount = BigDecimal("1200")
            )
        val orderItems = emptyList<OrderItemEntity>()

        val receipt = ReceiptFormatter.formatOrder("Store", order, orderItems, null, null)

        assertTrue(receipt.contains("TOTAL: KSh 1,200") || receipt.contains("TOTAL: KSh 1200"))
    }
}
