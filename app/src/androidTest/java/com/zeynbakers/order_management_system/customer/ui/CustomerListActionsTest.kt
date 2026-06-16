package com.zeynbakers.order_management_system.customer.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class CustomerListActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionsAreVisibleWithoutLongPress() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.customer_actions))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.customer_action_record_payment))
            .assert(SemanticsMatcher("exists") { true })
        composeRule
            .onNodeWithText(context.getString(R.string.customer_action_new_order))
            .assert(SemanticsMatcher("exists") { true })
        composeRule
            .onNodeWithText(context.getString(R.string.customer_action_message))
            .assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun progressiveFilterControlIsVisible() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.action_more_filters))
            .assert(SemanticsMatcher("exists") { true })
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
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Settled With Orders").assert(SemanticsMatcher("exists") { true })
    }
}
