package com.zeynbakers.order_management_system.order.ui

import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayDetailModelsTest {

    @Test
    fun `sortOrdersForPlanner places timed orders before untimed orders`() {
        val untimed = testOrder(id = 1L, pickupTime = null, createdAt = 300L)
        val timed = testOrder(id = 2L, pickupTime = "09:30", createdAt = 100L)

        val sorted = sortOrdersForPlanner(listOf(untimed, timed))

        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun `sortOrdersForPlanner sorts timed orders by pickup time ascending`() {
        val noon = testOrder(id = 1L, pickupTime = "12:00", createdAt = 100L)
        val early = testOrder(id = 2L, pickupTime = "08:15", createdAt = 200L)
        val morning = testOrder(id = 3L, pickupTime = "09:00", createdAt = 300L)

        val sorted = sortOrdersForPlanner(listOf(noon, early, morning))

        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `sortOrdersForPlanner keeps untimed orders newest first`() {
        val oldest = testOrder(id = 1L, pickupTime = null, createdAt = 100L)
        val newest = testOrder(id = 2L, pickupTime = "", createdAt = 300L)
        val middle = testOrder(id = 3L, pickupTime = null, createdAt = 200L)

        val sorted = sortOrdersForPlanner(listOf(oldest, newest, middle))

        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `plannerPickupDisplay normalizes valid time`() {
        assertEquals("09:30", plannerPickupDisplay("930"))
    }

    private fun testOrder(
        id: Long,
        pickupTime: String?,
        createdAt: Long
    ): OrderEntity {
        return OrderEntity(
            id = id,
            orderDate = LocalDate(2026, 2, 22),
            createdAt = createdAt,
            updatedAt = createdAt,
            notes = "Test $id",
            pickupTime = pickupTime,
            totalAmount = BigDecimal("100")
        )
    }
}
