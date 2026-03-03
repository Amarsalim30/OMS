package com.zeynbakers.order_management_system.customer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class CustomerListActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionsAreVisibleWithoutLongPress() {
        composeRule.setContent {
            MaterialTheme {
                CustomerListScreen(
                    query = "",
                    summaries =
                        listOf(
                            CustomerAccountSummary(
                                customerId = 1L,
                                name = "Alex Doe",
                                phone = "0712345678",
                                billed = BigDecimal("1500"),
                                paid = BigDecimal("500"),
                                balance = BigDecimal("1000")
                            )
                        ),
                    onQueryChange = {},
                    onCustomerClick = {},
                    onBack = {},
                    showBack = false
                )
            }
        }

        composeRule.onNodeWithText("Pay").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Order").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Message").assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun progressiveFilterControlIsVisible() {
        composeRule.setContent {
            MaterialTheme {
                CustomerListScreen(
                    query = "",
                    summaries = emptyList(),
                    onQueryChange = {},
                    onCustomerClick = {},
                    onBack = {},
                    showBack = false
                )
            }
        }

        composeRule.onNodeWithText("More filters").assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun settledCustomerWithOrders_isVisibleByDefault() {
        composeRule.setContent {
            MaterialTheme {
                CustomerListScreen(
                    query = "",
                    summaries =
                        listOf(
                            CustomerAccountSummary(
                                customerId = 2L,
                                name = "Settled With Orders",
                                phone = "0711111111",
                                billed = BigDecimal.ZERO,
                                paid = BigDecimal.ZERO,
                                balance = BigDecimal.ZERO,
                                hasOrders = true
                            )
                        ),
                    onQueryChange = {},
                    onCustomerClick = {},
                    onBack = {},
                    showBack = false
                )
            }
        }

        composeRule.onNodeWithText("Settled With Orders").assert(SemanticsMatcher("exists") { true })
    }
}
