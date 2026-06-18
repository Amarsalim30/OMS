@file:Suppress("DEPRECATION")

package com.zeynbakers.order_management_system.accounting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppSpacing
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
    hasSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        stringResource(R.string.money_step_source) to if (hasSource) StepState.Complete else StepState.Active,
        stringResource(R.string.money_step_assign) to when {
            !hasSource -> StepState.Idle
            hasAssignments -> StepState.Complete
            else -> StepState.Active
        },
        stringResource(R.string.money_step_post) to when {
            !hasAssignments -> StepState.Idle
            hasSelected -> StepState.Complete
            else -> StepState.Active
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (label, state) ->
            val isActive = state != StepState.Idle

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = when (state) {
                        StepState.Complete -> MaterialTheme.colorScheme.primary
                        StepState.Active -> MaterialTheme.colorScheme.primaryContainer
                        StepState.Idle -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (state) {
                                StepState.Complete -> MaterialTheme.colorScheme.onPrimary
                                StepState.Active -> MaterialTheme.colorScheme.onPrimaryContainer
                                StepState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppSpacing.xSmall))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    )
                )
            }
            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppSpacing.small)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/**
 * Bottom bar shown when there are ready payments to apply.
 */
@Composable
internal fun ApplyReadyBar(
    selectedReadyCount: Int,
    selectedReadyAmount: BigDecimal,
    readyCount: Int,
    readyAmount: BigDecimal,
    onApplyReady: () -> Unit,
    onApplySelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(AppSpacing.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.money_ready_total,
                        readyCount,
                        formatKes(readyAmount)
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedReadyCount in 1 until readyCount) {
                    Text(
                        text = stringResource(
                            R.string.money_selected_total,
                            selectedReadyCount,
                            formatKes(selectedReadyAmount)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedReadyCount in 1 until readyCount) {
                OutlinedButton(
                    onClick = onApplySelected,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.money_apply_selected_count, selectedReadyCount))
                }
            }

            Button(
                onClick = onApplyReady,
                enabled = readyCount > 0,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.money_apply_ready_count, readyCount))
            }
        }
    }
}
