package com.zeynbakers.order_management_system.core.backup

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    onImportContacts: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { BackupPreferences(context) }
    var state by remember { mutableStateOf(prefs.readState()) }
    var appBackups by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingRestore by remember { mutableStateOf<RestoreRequest?>(null) }
    val uiEvents = LocalUiEventDispatcher.current
    val scope = rememberCoroutineScope()

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                prefs.setTargetType(BackupTargetType.SafDirectory)
                prefs.setTargetUri(uri.toString())
                state = prefs.readState()
                BackupScheduler.ensureScheduled(context)
            }
        }

    val restorePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                pendingRestore = RestoreRequest(uri.toString())
            }
        }

    LaunchedEffect(Unit) {
        state = prefs.readState()
        appBackups = withContext(Dispatchers.IO) { BackupManager.listAppPrivateBackups(context) }
    }

    val backupWorkInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_MANUAL_WORK)
        .observeAsState(emptyList())
    val restoreWorkInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_RESTORE_WORK)
        .observeAsState(emptyList())

    val backupInfo = backupWorkInfos.firstOrNull()
    val restoreInfo = restoreWorkInfos.firstOrNull()
    val isBackupRunning = backupInfo?.state == WorkInfo.State.RUNNING
    val isRestoreRunning = restoreInfo?.state == WorkInfo.State.RUNNING
    val backupProgress = backupInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val restoreProgress = restoreInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val backupStage = backupInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: "Backing up"
    val restoreStage = restoreInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: "Restoring"

    LaunchedEffect(backupInfo?.state, restoreInfo?.state) {
        state = prefs.readState()
        appBackups = withContext(Dispatchers.IO) { BackupManager.listAppPrivateBackups(context) }
    }

    LaunchedEffect(backupInfo?.state) {
        when (backupInfo?.state) {
            WorkInfo.State.SUCCEEDED ->
                uiEvents.showSnackbar("Backup complete")
            WorkInfo.State.FAILED ->
                uiEvents.showSnackbar("Backup failed")
            else -> Unit
        }
    }

    LaunchedEffect(restoreInfo?.state) {
        when (restoreInfo?.state) {
            WorkInfo.State.SUCCEEDED ->
                uiEvents.showSnackbar("Restore complete. Restart app.")
            WorkInfo.State.FAILED ->
                uiEvents.showSnackbar("Restore failed")
            else -> Unit
        }
    }

    val lastBackupLabel =
        state.lastBackupTime?.let { formatTimestamp(it) } ?: "Never"
    val statusLabel =
        when (state.lastStatus) {
            BackupStatus.Success -> "Success"
            BackupStatus.Failed -> "Failed"
            BackupStatus.Never -> "Never"
        }
    val targetLabel =
        if (state.targetType == BackupTargetType.AppPrivate) {
            "App storage (private)"
        } else {
            val label =
                state.targetUri?.let { uri ->
                    runCatching {
                        DocumentFile.fromTreeUri(context, uri.toUri())?.name
                    }.getOrNull()
                }
            label?.let { "Folder: $it" } ?: "Folder: Not selected"
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Column {
                    Text("Automatic daily backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Runs once a day when battery and storage are OK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.autoEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && state.targetType == BackupTargetType.SafDirectory && state.targetUri == null) {
                            scope.launch { uiEvents.showSnackbar("Pick a backup folder first") }
                            folderPicker.launch(null)
                            return@Switch
                        }
                        BackupScheduler.setAutomaticEnabled(context, enabled)
                        state = prefs.readState()
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Last backup: $lastBackupLabel", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Status: $statusLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        when (state.lastStatus) {
                            BackupStatus.Success -> MaterialTheme.colorScheme.primary
                            BackupStatus.Failed -> MaterialTheme.colorScheme.error
                            BackupStatus.Never -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                )
                state.lastMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Backup location", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.targetType == BackupTargetType.AppPrivate,
                        onClick = {
                            prefs.setTargetType(BackupTargetType.AppPrivate)
                            prefs.setTargetUri(null)
                            state = prefs.readState()
                            BackupScheduler.ensureScheduled(context)
                        },
                        label = { Text("App storage") }
                    )
                    FilterChip(
                        selected = state.targetType == BackupTargetType.SafDirectory,
                        onClick = {
                            folderPicker.launch(null)
                        },
                        label = { Text("Folder") }
                    )
                }
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.targetType == BackupTargetType.SafDirectory) {
                    TextButton(onClick = { folderPicker.launch(null) }) {
                        Text("Change folder")
                    }
                }
            }

            if (isBackupRunning) {
                ProgressRow(
                    title = "Backup in progress",
                    stage = backupStage,
                    progress = backupProgress
                )
            }

            if (isRestoreRunning) {
                ProgressRow(
                    title = "Restore in progress",
                    stage = restoreStage,
                    progress = restoreProgress
                )
            }

            Button(
                onClick = {
                    if (state.targetType == BackupTargetType.SafDirectory && state.targetUri == null) {
                        scope.launch { uiEvents.showSnackbar("Pick a backup folder first") }
                        folderPicker.launch(null)
                        return@Button
                    }
                    BackupScheduler.enqueueManualBackup(context)
                    scope.launch { uiEvents.showSnackbar("Backup started") }
                },
                enabled = !isBackupRunning && !isRestoreRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run backup now")
            }

            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Restore", style = MaterialTheme.typography.titleMedium)
                val latestBackup = appBackups.firstOrNull()
                val latestLabel = latestBackup?.name ?: "None"
                Text(
                    text = "Latest app storage backup: $latestLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (latestBackup == null) {
                                scope.launch { uiEvents.showSnackbar("No app storage backups found") }
                                return@Button
                            }
                            pendingRestore = RestoreRequest(null)
                        },
                        enabled = !isBackupRunning && !isRestoreRunning && latestBackup != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore latest")
                    }
                    Button(
                        onClick = { restorePicker.launch(arrayOf("application/zip")) },
                        enabled = !isBackupRunning && !isRestoreRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore from file")
                    }
                }
                Text(
                    text = "Restore replaces all current data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    val restoreRequest = pendingRestore
    if (restoreRequest != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This will replace all current data.") },
            confirmButton = {
                Button(
                    onClick = {
                        BackupScheduler.enqueueRestore(context, restoreRequest.uriString)
                        scope.launch { uiEvents.showSnackbar("Restore started") }
                        pendingRestore = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun ProgressRow(title: String, stage: String, progress: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { (progress.coerceIn(0, 100)) / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class RestoreRequest(val uriString: String?)

