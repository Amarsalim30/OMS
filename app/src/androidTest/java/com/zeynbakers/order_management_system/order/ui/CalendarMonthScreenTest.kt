package com.zeynbakers.order_management_system.order.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class CalendarMonthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedDayRendersAsSelected() {
        val day =
            CalendarDayUi(
                date = LocalDate(2026, 1, 16),
                orderCount = 0,
                totalAmount = BigDecimal.ZERO,
                isToday = false,
                isInCurrentMonth = true,
                paymentState = null
            )

        composeRule.setContent {
            MaterialTheme {
                DayCell(
                    day = day,
                    isSelected = true,
                    onSelectDate = {},
                    onOpenDay = {},
                    onQuickAdd = {}
                )
            }
        }

        composeRule.onNodeWithTag("day-cell-2026-01-16")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun todayStateIncludesTodayInContentDescription() {
        val day =
            CalendarDayUi(
                date = LocalDate(2026, 1, 16),
                orderCount = 0,
                totalAmount = BigDecimal.ZERO,
                isToday = true,
                isInCurrentMonth = true,
                paymentState = null
            )
        val expectedDescription = "16 January 2026, today, no orders"

        composeRule.setContent {
            MaterialTheme {
                DayCell(
                    day = day,
                    isSelected = false,
                    onSelectDate = {},
                    onOpenDay = {},
                    onQuickAdd = {}
                )
            }
        }

        composeRule.onNodeWithTag("day-cell-2026-01-16")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf(expectedDescription)
                )
            )
    }

    @Test
    fun markersRenderAndOverflowShowsPlusCount() {
        val day =
            CalendarDayUi(
                date = LocalDate(2026, 1, 16),
                orderCount = 5,
                totalAmount = BigDecimal("250"),
                isToday = false,
                isInCurrentMonth = true,
                paymentState = PaymentState.PARTIAL,
                orderStates =
                    listOf(
                        PaymentState.UNPAID,
                        PaymentState.PARTIAL,
                        PaymentState.PAID,
                        PaymentState.PAID,
                        PaymentState.OVERPAID
                    )
            )

        composeRule.setContent {
            MaterialTheme {
                DayCell(
                    day = day,
                    isSelected = false,
                    onSelectDate = {},
                    onOpenDay = {},
                    onQuickAdd = {}
                )
            }
        }

        composeRule.onNodeWithTag("day-markers-2026-01-16").assertExists()
        composeRule.onNodeWithTag("day-markers-2026-01-16-overflow").assertExists()
        composeRule.onNodeWithText("+2").assertExists()
    }

    @Test
    fun markersWithoutOverflowDoNotShowPlusCount() {
        val day =
            CalendarDayUi(
                date = LocalDate(2026, 1, 16),
                orderCount = 3,
                totalAmount = BigDecimal("150"),
                isToday = false,
                isInCurrentMonth = true,
                paymentState = PaymentState.PAID,
                orderStates =
                    listOf(
                        PaymentState.PAID,
                        PaymentState.PAID,
                        PaymentState.PAID
                    )
            )

        composeRule.setContent {
            MaterialTheme {
                DayCell(
                    day = day,
                    isSelected = false,
                    onSelectDate = {},
                    onOpenDay = {},
                    onQuickAdd = {}
                )
            }
        }

        composeRule.onNodeWithTag("day-markers-2026-01-16").assertExists()
        composeRule.onNodeWithTag("day-markers-2026-01-16-overflow").assertDoesNotExist()
    }
}
