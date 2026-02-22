package com.zeynbakers.order_management_system.core.tutorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard

private data class TutorialCoachStep(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
    val actionLabelRes: Int? = null,
    val onAction: (() -> Unit)? = null,
    val secondaryActionLabelRes: Int? = null,
    val onSecondaryAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeginnerTutorialScreen(
    onBack: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenNotesHistory: () -> Unit,
    onOpenHelperSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenNotifications: () -> Unit,
    onFinish: () -> Unit = onBack,
    initialStep: Int = 0
) {
    val steps = remember(
        onOpenCalendar,
        onOpenOrders,
        onOpenCustomers,
        onOpenMoney,
        onOpenNotesHistory,
        onOpenHelperSettings,
        onOpenBackup,
        onOpenNotifications
    ) {
        listOf(
            TutorialCoachStep(
                titleRes = R.string.tutorial_step1_title,
                bodyRes = R.string.tutorial_step1_body,
                icon = Icons.Filled.CalendarToday,
                actionLabelRes = R.string.tutorial_action_open_calendar,
                onAction = onOpenCalendar
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step2_title,
                bodyRes = R.string.tutorial_step2_body,
                icon = Icons.AutoMirrored.Filled.ListAlt,
                actionLabelRes = R.string.tutorial_action_open_orders,
                onAction = onOpenOrders
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step3_title,
                bodyRes = R.string.tutorial_step3_body,
                icon = Icons.Filled.People,
                actionLabelRes = R.string.tutorial_action_open_customers,
                onAction = onOpenCustomers
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step4_title,
                bodyRes = R.string.tutorial_step4_body,
                icon = Icons.Filled.AccountBalanceWallet,
                actionLabelRes = R.string.tutorial_action_open_money,
                onAction = onOpenMoney
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step5_title,
                bodyRes = R.string.tutorial_step5_body,
                icon = Icons.AutoMirrored.Filled.Notes,
                actionLabelRes = R.string.tutorial_action_open_notes_history,
                onAction = onOpenNotesHistory
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step6_title,
                bodyRes = R.string.tutorial_step6_body,
                icon = Icons.Filled.Mic,
                actionLabelRes = R.string.tutorial_action_open_helper_settings,
                onAction = onOpenHelperSettings
            ),
            TutorialCoachStep(
                titleRes = R.string.tutorial_step7_title,
                bodyRes = R.string.tutorial_step7_body,
                icon = Icons.Filled.Settings,
                actionLabelRes = R.string.more_backup_restore,
                onAction = onOpenBackup,
                secondaryActionLabelRes = R.string.more_notifications,
                onSecondaryAction = onOpenNotifications
            )
        )
    }

    val clampedInitialStep = initialStep.coerceIn(0, steps.lastIndex)
    var stepIndex by rememberSaveable(clampedInitialStep) {
        mutableIntStateOf(clampedInitialStep)
    }
    val currentStep = steps[stepIndex]
    val progress = (stepIndex + 1).toFloat() / steps.size.toFloat()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.tutorial_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppCard {
                Text(
                    text = stringResource(R.string.tutorial_header_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.tutorial_header_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tutorial_where_to_find_tabs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tutorial_where_to_find_more),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tutorial_where_to_find_mpesa_share),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AppCard {
                Text(
                    text = stringResource(R.string.tutorial_coach_progress, stepIndex + 1, steps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = currentStep.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(currentStep.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(currentStep.bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentStep.onAction != null && currentStep.actionLabelRes != null) {
                    ElevatedButton(
                        onClick = currentStep.onAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(currentStep.actionLabelRes))
                    }
                }
                if (currentStep.onSecondaryAction != null && currentStep.secondaryActionLabelRes != null) {
                    OutlinedButton(
                        onClick = currentStep.onSecondaryAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null
                            )
                            Text(stringResource(currentStep.secondaryActionLabelRes))
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.tutorial_coach_action_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                    enabled = stepIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.tutorial_coach_previous))
                }
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_coach_skip),
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        if (stepIndex == steps.lastIndex) {
                            onFinish()
                        } else {
                            stepIndex = (stepIndex + 1).coerceAtMost(steps.lastIndex)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            if (stepIndex == steps.lastIndex) {
                                stringResource(R.string.tutorial_coach_finish)
                            } else {
                                stringResource(R.string.tutorial_coach_next)
                            },
                        textAlign = TextAlign.Center
                    )
                }
            }

            AppCard {
                Text(
                    text = stringResource(R.string.tutorial_finish_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
