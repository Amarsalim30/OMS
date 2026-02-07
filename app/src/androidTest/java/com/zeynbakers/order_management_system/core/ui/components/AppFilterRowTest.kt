package com.zeynbakers.order_management_system.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppFilterRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun secondaryOptionCanBeSelectedFromMoreFilters() {
        composeRule.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf("all") }
                Column {
                    AppFilterRow(
                        options =
                            listOf(
                                AppFilterOption("all", "All"),
                                AppFilterOption("due", "Due"),
                                AppFilterOption("credit", "Credit"),
                                AppFilterOption("settled", "Settled")
                            ),
                        selectedKey = selected,
                        onSelect = { selected = it }
                    )
                    Text("selected:$selected")
                }
            }
        }

        composeRule.onNodeWithText("More filters").performClick()
        composeRule.onNodeWithText("Settled").performClick()
        composeRule.onNodeWithText("selected:settled").assert(SemanticsMatcher("exists") { true })
    }
}
