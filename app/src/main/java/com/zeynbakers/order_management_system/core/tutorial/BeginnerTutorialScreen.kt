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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeginnerTutorialScreen(
    onBack: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenNotifications: () -> Unit
) {
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
            }

            TutorialStepCard(
                step = 1,
                title = stringResource(R.string.tutorial_step1_title),
                body = stringResource(R.string.tutorial_step1_body),
                actionLabel = stringResource(R.string.tutorial_action_open_calendar),
                actionIcon = Icons.Filled.CalendarToday,
                onActionClick = onOpenCalendar
            )

            TutorialStepCard(
                step = 2,
                title = stringResource(R.string.tutorial_step2_title),
                body = stringResource(R.string.tutorial_step2_body),
                actionLabel = stringResource(R.string.tutorial_action_open_orders),
                actionIcon = Icons.AutoMirrored.Filled.ListAlt,
                onActionClick = onOpenOrders
            )

            TutorialStepCard(
                step = 3,
                title = stringResource(R.string.tutorial_step3_title),
                body = stringResource(R.string.tutorial_step3_body),
                actionLabel = stringResource(R.string.tutorial_action_open_customers),
                actionIcon = Icons.Filled.People,
                onActionClick = onOpenCustomers
            )

            TutorialStepCard(
                step = 4,
                title = stringResource(R.string.tutorial_step4_title),
                body = stringResource(R.string.tutorial_step4_body),
                actionLabel = stringResource(R.string.tutorial_action_open_money),
                actionIcon = Icons.Filled.AccountBalanceWallet,
                onActionClick = onOpenMoney
            )

            AppCard {
                Text(
                    text = stringResource(R.string.tutorial_step5_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.tutorial_step5_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text(
                            text = stringResource(R.string.more_backup_restore),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Text(
                            text = stringResource(R.string.more_notifications),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            AppCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.tutorial_finish_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialStepCard(
    step: Int,
    title: String,
    body: String,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onActionClick: () -> Unit
) {
    AppCard {
        Text(
            text = stringResource(R.string.tutorial_step_label, step),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ElevatedButton(
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
            Icon(actionIcon, contentDescription = null)
            Text(text = actionLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
