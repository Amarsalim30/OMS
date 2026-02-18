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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.components.AppScreenHeaderCard
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
    val backupStageDefault = stringResource(R.string.backup_stage_backing_up)
    val restoreStageDefault = stringResource(R.string.backup_stage_restoring)
    val backupCompleteMessage = stringResource(R.string.backup_complete)
    val backupFailedMessage = stringResource(R.string.backup_failed)
    val restoreCompleteMessage = stringResource(R.string.restore_complete_restart)
    val restoreFailedMessage = stringResource(R.string.restore_failed)
    val pickFolderFirstMessage = stringResource(R.string.backup_pick_folder_first)
    val backupStartedMessage = stringResource(R.string.backup_started)
    val noBackupFoundMessage = stringResource(R.string.backup_no_app_storage_backups)
    val restoreStartedMessage = stringResource(R.string.restore_started)
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
    val backupStage = backupInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: backupStageDefault
    val restoreStage = restoreInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: restoreStageDefault

    LaunchedEffect(backupInfo?.state, restoreInfo?.state) {
        state = prefs.readState()
        appBackups = withContext(Dispatchers.IO) { BackupManager.listAppPrivateBackups(context) }
    }

    LaunchedEffect(backupInfo?.state) {
        when (backupInfo?.state) {
            WorkInfo.State.SUCCEEDED ->
                uiEvents.showSnackbar(backupCompleteMessage)
            WorkInfo.State.FAILED ->
                uiEvents.showSnackbar(backupFailedMessage)
            else -> Unit
        }
    }

    LaunchedEffect(restoreInfo?.state) {
        when (restoreInfo?.state) {
            WorkInfo.State.SUCCEEDED ->
                uiEvents.showSnackbar(restoreCompleteMessage)
            WorkInfo.State.FAILED ->
                uiEvents.showSnackbar(restoreFailedMessage)
            else -> Unit
        }
    }

    val lastBackupLabel =
        state.lastBackupTime?.let { formatTimestamp(it) } ?: stringResource(R.string.backup_status_never)
    val statusLabel =
        when (state.lastStatus) {
            BackupStatus.Success -> stringResource(R.string.backup_status_success)
            BackupStatus.Failed -> stringResource(R.string.backup_status_failed)
            BackupStatus.Never -> stringResource(R.string.backup_status_never)
        }
    val targetLabel =
        if (state.targetType == BackupTargetType.AppPrivate) {
            stringResource(R.string.backup_target_app_storage_private)
        } else {
            val label =
                state.targetUri?.let { uri ->
                    runCatching {
                        DocumentFile.fromTreeUri(context, uri.toUri())?.name
                    }.getOrNull()
                }
            label?.let { stringResource(R.string.backup_target_folder_named, it) }
                ?: stringResource(R.string.backup_target_folder_not_selected)
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
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
                title = stringResource(R.string.backup_owner_title),
                subtitle = stringResource(R.string.backup_owner_subtitle),
                leadingIcon = Icons.Filled.Settings,
                highlight = statusLabel
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.backup_automatic_daily),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.backup_automatic_daily_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.autoEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && state.targetType == BackupTargetType.SafDirectory && state.targetUri == null) {
                            scope.launch { uiEvents.showSnackbar(pickFolderFirstMessage) }
                            folderPicker.launch(null)
                            return@Switch
                        }
                        BackupScheduler.setAutomaticEnabled(context, enabled)
                        state = prefs.readState()
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.backup_last_backup, lastBackupLabel),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.backup_status, statusLabel),
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
                Text(
                    text = stringResource(R.string.backup_location),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.targetType == BackupTargetType.AppPrivate,
                        onClick = {
                            prefs.setTargetType(BackupTargetType.AppPrivate)
                            prefs.setTargetUri(null)
                            state = prefs.readState()
                            BackupScheduler.ensureScheduled(context)
                        },
                        label = { Text(stringResource(R.string.backup_target_app_storage)) }
                    )
                    FilterChip(
                        selected = state.targetType == BackupTargetType.SafDirectory,
                        onClick = {
                            folderPicker.launch(null)
                        },
                        label = { Text(stringResource(R.string.backup_target_folder)) }
                    )
                }
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.targetType == BackupTargetType.SafDirectory) {
                    TextButton(onClick = { folderPicker.launch(null) }) {
                        Text(stringResource(R.string.backup_change_folder))
                    }
                }
            }

            if (isBackupRunning) {
                ProgressRow(
                    title = stringResource(R.string.backup_in_progress),
                    stage = backupStage,
                    progress = backupProgress
                )
            }

            if (isRestoreRunning) {
                ProgressRow(
                    title = stringResource(R.string.restore_in_progress),
                    stage = restoreStage,
                    progress = restoreProgress
                )
            }

            Button(
                onClick = {
                    if (state.targetType == BackupTargetType.SafDirectory && state.targetUri == null) {
                        scope.launch { uiEvents.showSnackbar(pickFolderFirstMessage) }
                        folderPicker.launch(null)
                        return@Button
                    }
                    BackupScheduler.enqueueManualBackup(context)
                    scope.launch { uiEvents.showSnackbar(backupStartedMessage) }
                },
                enabled = !isBackupRunning && !isRestoreRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_run_now))
            }

            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.restore_title), style = MaterialTheme.typography.titleMedium)
                val latestBackup = appBackups.firstOrNull()
                val latestLabel = latestBackup?.name ?: stringResource(R.string.backup_none)
                Text(
                    text = stringResource(R.string.backup_latest_app_storage_backup, latestLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (latestBackup == null) {
                                scope.launch { uiEvents.showSnackbar(noBackupFoundMessage) }
                                return@Button
                            }
                            pendingRestore = RestoreRequest(null)
                        },
                        enabled = !isBackupRunning && !isRestoreRunning && latestBackup != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.restore_latest))
                    }
                    Button(
                        onClick = { restorePicker.launch(arrayOf("application/zip")) },
                        enabled = !isBackupRunning && !isRestoreRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.restore_from_file))
                    }
                }
                Text(
                    text = stringResource(R.string.restore_replaces_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onImportContacts, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.more_import_contacts))
            }
        }
    }

    val restoreRequest = pendingRestore
    if (restoreRequest != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        BackupScheduler.enqueueRestore(context, restoreRequest.uriString)
                        scope.launch { uiEvents.showSnackbar(restoreStartedMessage) }
                        pendingRestore = null
                    }
                ) {
                    Text(stringResource(R.string.restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text(stringResource(R.string.action_cancel))
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
    val clampedProgress = progress.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { clampedProgress / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.backup_progress_percent, clampedProgress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class RestoreRequest(val uriString: String?)

