package com.zeynbakers.order_management_system.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zeynbakers.order_management_system.R

data class AppFilterOption(
    val key: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterRow(
    options: List<AppFilterOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    primaryCount: Int = 3
) {
    if (options.isEmpty()) return

    val primary = options.take(primaryCount)
    val secondary = options.drop(primaryCount)
    var showMore by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        primary.forEach { option ->
            FilterChip(
                selected = selectedKey == option.key,
                onClick = { onSelect(option.key) },
                label = { Text(option.label) }
            )
        }
        if (secondary.isNotEmpty()) {
            TextButton(onClick = { showMore = true }) {
                Text(stringResource(R.string.action_more_filters))
            }
        }
    }

    if (showMore) {
        ModalBottomSheet(onDismissRequest = { showMore = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = selectedKey == option.key,
                        onClick = {
                            onSelect(option.key)
                            showMore = false
                        },
                        label = { Text(option.label) }
                    )
                }
            }
        }
    }
}
