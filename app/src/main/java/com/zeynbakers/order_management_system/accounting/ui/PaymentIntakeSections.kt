@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import com.zeynbakers.order_management_system.core.util.formatKes
import java.math.BigDecimal

private enum class StepState {
    Idle,
    Active,
    Complete
}

@Composable
internal fun IntakeStepRow(
    hasSource: Boolean,
    hasAssignments: Boolean,
    hasSelected: Boolean
) {
    val sourceState = if (hasSource) StepState.Complete else StepState.Active
    val assignState =
        when {
            !hasSource -> StepState.Idle
            hasAssignments -> StepState.Complete
            else -> StepState.Active
        }
    val postState =
        when {
            !hasAssignments -> StepState.Idle
            hasSelected -> StepState.Complete
            else -> StepState.Active
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StepChip(
            step = "1",
            label = stringResource(R.string.money_step_source),
            state = sourceState,
            modifier = Modifier.weight(1f)
        )
        StepChip(
            step = "2",
            label = stringResource(R.string.money_step_assign),
            state = assignState,
            modifier = Modifier.weight(1f)
        )
        StepChip(
            step = "3",
            label = stringResource(R.string.money_step_post),
            state = postState,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StepChip(
    step: String,
    label: String,
    state: StepState,
    modifier: Modifier = Modifier
) {
    val (container, content) =
        when (state) {
            StepState.Complete ->
                MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            StepState.Active ->
                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            StepState.Idle ->
                MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = content.copy(alpha = 0.12f),
                contentColor = content,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun IntakeSummaryRow(
    totalDetected: Int,
    readyCount: Int,
    needsMatchCount: Int,
    duplicateCount: Int
) {
    val hasAny = totalDetected > 0 || readyCount > 0 || needsMatchCount > 0 || duplicateCount > 0
    if (!hasAny) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryPill(
            label = stringResource(R.string.money_detected),
            value = totalDetected,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SummaryPill(
            label = stringResource(R.string.money_ready),
            value = readyCount,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )
        SummaryPill(
            label = stringResource(R.string.money_needs_match),
            value = needsMatchCount,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        )
        SummaryPill(
            label = stringResource(R.string.money_duplicates),
            value = duplicateCount,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IntakeSummaryCard(
    totalDetected: Int,
    readyCount: Int,
    needsMatchCount: Int,
    duplicateCount: Int,
    selectedCount: Int,
    selectedAmount: BigDecimal,
    onSelectMatched: () -> Unit,
    onClearSelection: () -> Unit
) {
    AppCard {
        Text(text = stringResource(R.string.summary_title), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryPill(
                label = stringResource(R.string.money_detected),
                value = totalDetected,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SummaryPill(
                label = stringResource(R.string.money_ready),
                value = readyCount,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            )
            SummaryPill(
                label = stringResource(R.string.money_needs_match),
                value = needsMatchCount,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            )
            SummaryPill(
                label = stringResource(R.string.money_duplicates),
                value = duplicateCount,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.money_selected_total, selectedCount, formatKes(selectedAmount)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = onSelectMatched) {
                    Text(stringResource(R.string.money_select_matched))
                }
                TextButton(onClick = onClearSelection) { Text(stringResource(R.string.action_clear)) }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: Int,
    container: Color,
    content: Color
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun ApplyReadyBar(
    selectedReadyCount: Int,
    selectedReadyAmount: BigDecimal,
    readyCount: Int,
    readyAmount: BigDecimal,
    onApplySelected: () -> Unit,
    onApplyAllReady: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.money_selected_total, selectedReadyCount, formatKes(selectedReadyAmount)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.money_ready_total, readyCount, formatKes(readyAmount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onApplySelected,
                enabled = selectedReadyCount > 0,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.money_apply_selected))
            }
            if (selectedReadyCount in 1 until readyCount) {
                TextButton(onClick = onApplyAllReady) {
                    Text(stringResource(R.string.money_apply_all_ready))
                }
            }
        }
    }
}
