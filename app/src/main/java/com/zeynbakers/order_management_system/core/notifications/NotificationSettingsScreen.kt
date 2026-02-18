package com.zeynbakers.order_management_system.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { NotificationPreferences(context) }
    var settings by remember { mutableStateOf(prefs.readSettings()) }
    var hasPermission by remember { mutableStateOf(hasNotificationPermission(context)) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted || !requiresPermission()
            if (granted) {
                NotificationScheduler.setEnabled(context, true)
                settings = prefs.readSettings()
            }
        }

    LaunchedEffect(Unit) {
        settings = prefs.readSettings()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
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
            modifier =
                Modifier
                    .padding(padding)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppScreenHeaderCard(
                title = stringResource(R.string.notifications_owner_title),
                subtitle = stringResource(R.string.notifications_owner_subtitle),
                leadingIcon = Icons.Filled.NotificationsActive,
                highlight = if (settings.enabled) {
                    stringResource(R.string.notifications_owner_enabled)
                } else {
                    stringResource(R.string.notifications_owner_disabled)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.notifications_order_reminders),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.notifications_order_reminders_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { enabled ->
                        if (enabled && requiresPermission() && !hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@Switch
                        }
                        NotificationScheduler.setEnabled(context, enabled)
                        settings = prefs.readSettings()
                    }
                )
            }

            if (requiresPermission() && !hasPermission) {
                Text(
                    text = stringResource(R.string.notifications_permission_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.notifications_grant_permission))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.notifications_reminder_lead_time),
                    style = MaterialTheme.typography.titleMedium
                )
                val leadTimeOptions =
                    listOf(
                        NotificationPreferences.LEAD_TIME_15_MIN to stringResource(R.string.notifications_lead_time_15_min),
                        NotificationPreferences.LEAD_TIME_30_MIN to stringResource(R.string.notifications_lead_time_30_min),
                        NotificationPreferences.LEAD_TIME_1_HOUR to stringResource(R.string.notifications_lead_time_1_hour),
                        NotificationPreferences.LEAD_TIME_2_HOURS to stringResource(R.string.notifications_lead_time_2_hours),
                        NotificationPreferences.LEAD_TIME_12_HOURS to stringResource(R.string.notifications_lead_time_12_hours),
                        NotificationPreferences.LEAD_TIME_1_DAY to stringResource(R.string.notifications_lead_time_1_day)
                    )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leadTimeOptions.forEach { (minutes, label) ->
                        LeadTimeChip(
                            label = label,
                            minutes = minutes,
                            selectedMinutes = settings.leadTimeMinutes
                        ) { selectedMinutes ->
                            prefs.setLeadTimeMinutes(selectedMinutes)
                            settings = prefs.readSettings()
                            NotificationScheduler.enqueueNow(context)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.notifications_daily_summary),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.notifications_daily_summary_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.dailySummaryEnabled,
                    onCheckedChange = { enabled ->
                        prefs.setDailySummaryEnabled(enabled)
                        settings = prefs.readSettings()
                        NotificationScheduler.enqueueNow(context)
                    }
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.notifications_workmanager_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeadTimeChip(
    label: String,
    minutes: Int,
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    FilterChip(
        selected = minutes == selectedMinutes,
        onClick = { onSelect(minutes) },
        label = { Text(label) }
    )
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (requiresPermission()) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun requiresPermission(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= 33
}

