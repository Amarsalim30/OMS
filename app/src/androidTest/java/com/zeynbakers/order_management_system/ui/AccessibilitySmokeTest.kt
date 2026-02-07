package com.zeynbakers.order_management_system.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilitySmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarPrimaryActionsMeetTouchTarget() {
        composeRule.setContent {
            MaterialTheme {
                CalendarScreen(
                    days = emptyList(),
                    currentYear = 2026,
                    currentMonth = 2,
                    baseYear = 2026,
                    baseMonth = 2,
                    monthSnapshots = emptyMap(),
                    monthTotal = BigDecimal.ZERO,
                    monthBadgeCount = 0,
                    selectedDate = LocalDate(2026, 2, 7),
                    onSelectDate = {},
                    onOpenDay = {},
                    onSaveOrder = { _, _, _, _, _, _ -> },
                    searchCustomers = { emptyList() },
                    onSummaryClick = {},
                    onMonthSettled = { _, _ -> },
                    openQuickAddDate = null,
                    onQuickAddConsumed = {}
                )
            }
        }

        val minTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }

        val addOrderNode = composeRule.onNodeWithContentDescription("Add order")
            .assert(SemanticsMatcher("exists") { true })
            .fetchSemanticsNode()
        assertTrue(addOrderNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(addOrderNode.boundsInRoot.height >= minTouchTargetPx)

        val todayNode = composeRule.onNodeWithContentDescription("Today")
            .assert(SemanticsMatcher("exists") { true })
            .fetchSemanticsNode()
        assertTrue(todayNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(todayNode.boundsInRoot.height >= minTouchTargetPx)
    }

    @Test
    fun dayDetailFiltersStayDiscoverableAtLargeFontScale() {
        val date = LocalDate(2026, 2, 7)
        val order =
            OrderEntity(
                id = 1L,
                orderDate = date,
                notes = "Bread order",
                totalAmount = BigDecimal("1000.00")
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                MaterialTheme {
                    DayDetailScreen(
                        date = date,
                        orders = listOf(order),
                        dayTotal = BigDecimal("1000.00"),
                        customerNames = emptyMap(),
                        orderPaidAmounts = emptyMap(),
                        onBack = {},
                        onSaveOrder = { _, _, _, _, _, _ -> },
                        onDeleteOrder = {},
                        loadOrderPaymentAllocations = { emptyList() },
                        loadMoveOrderOptions = { _, _ -> emptyList() },
                        onDeleteOrderWithPayments = { _, _, _, _, _, _ -> true },
                        onOrderPaymentHistory = {},
                        onReceivePayment = {},
                        loadCustomerById = { null },
                        searchCustomers = { emptyList() },
                        draft = null,
                        onDraftChange = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("More filters").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Search orders").assert(SemanticsMatcher("exists") { true })
    }
}
