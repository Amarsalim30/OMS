package com.zeynbakers.order_management_system.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Order reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Due-date reminders and daily summary.",
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
                    text = "Notification permission is required on Android 13+.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Grant permission")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Reminder lead time", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeadTimeChip(
                        label = "1 hour",
                        minutes = NotificationPreferences.LEAD_TIME_1_HOUR,
                        selectedMinutes = settings.leadTimeMinutes
                    ) { minutes ->
                        prefs.setLeadTimeMinutes(minutes)
                        settings = prefs.readSettings()
                        NotificationScheduler.enqueueNow(context)
                    }
                    LeadTimeChip(
                        label = "1 day",
                        minutes = NotificationPreferences.LEAD_TIME_1_DAY,
                        selectedMinutes = settings.leadTimeMinutes
                    ) { minutes ->
                        prefs.setLeadTimeMinutes(minutes)
                        settings = prefs.readSettings()
                        NotificationScheduler.enqueueNow(context)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Daily summary", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "One notification each morning with totals.",
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
                text = "Reminders run with WorkManager (no exact alarms).",
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

