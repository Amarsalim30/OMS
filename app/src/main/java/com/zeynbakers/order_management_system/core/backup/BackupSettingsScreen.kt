package com.zeynbakers.order_management_system.core.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val backupStageDefault = stringResource(R.string.backup_stage_backing_up)
    val restoreStageDefault = stringResource(R.string.backup_stage_restoring)
    val backupCompleteMessage = stringResource(R.string.backup_complete)
    val backupFailedMessage = stringResource(R.string.backup_failed)
    val restoreCompleteMessage = stringResource(R.string.restore_complete_restart)
    val restoreFailedMessage = stringResource(R.string.restore_failed)
    val pickFolderFirstMessage = stringResource(R.string.backup_pick_folder_first)
    val pickFileFirstMessage = stringResource(R.string.backup_pick_file_first)
    val backupStartedMessage = stringResource(R.string.backup_started)
    val noBackupFoundMessage = stringResource(R.string.backup_no_backups_found)
    val restoreStartedMessage = stringResource(R.string.restore_started)
    val browseBackupsUnavailableMessage = stringResource(R.string.backup_browse_backups_unavailable)
    val testWritePassedMessage = stringResource(R.string.backup_test_write_passed)
    val testWriteFailedMessage = stringResource(R.string.backup_test_write_failed)
    val targetPermissionFailedMessage = stringResource(R.string.backup_folder_permission_failed)
    val targetUnavailableMessage = stringResource(R.string.backup_folder_unavailable)
    val cloudFolderInfo = stringResource(R.string.backup_cloud_folder_mode_info)
    val cloudFileInfo = stringResource(R.string.backup_cloud_file_mode_info)
    val chooseLocationLabel = stringResource(R.string.backup_choose_location)
    val changeLocationLabel = stringResource(R.string.backup_change_location)
    val locationRecommendedLabel = stringResource(R.string.backup_location_recommended)
    val useAppStorageQuickLabel = stringResource(R.string.backup_use_app_storage_quick)
    val currentModeLabelTemplate = stringResource(R.string.backup_current_mode)
    val driveBrowseHint = stringResource(R.string.backup_drive_picker_hint)
    val driveSystemPickerHint = stringResource(R.string.backup_drive_picker_system_hint)
    val troubleshootingTitle = stringResource(R.string.backup_troubleshoot_drive_title)
    val manifestPolicyStrictLabel = stringResource(R.string.backup_manifest_policy_strict)
    val manifestPolicyLegacyLabel = stringResource(R.string.backup_manifest_policy_legacy)
    val legacyModeWarning = stringResource(R.string.backup_manifest_legacy_warning)
    val strictModeInfo = stringResource(R.string.backup_manifest_strict_info)
    val manifestPolicyTitle = stringResource(R.string.backup_manifest_policy_title)
    val healthAttentionTitle = stringResource(R.string.backup_health_attention_title)
    val needsRelinkHint = stringResource(R.string.backup_health_needs_relink_hint)
    val unavailableHint = stringResource(R.string.backup_health_unavailable_hint)
    val troubleshootDriveInstalled = stringResource(R.string.backup_troubleshoot_drive_installed)
    val troubleshootDriveEnabled = stringResource(R.string.backup_troubleshoot_drive_enabled)
    val troubleshootDocumentsUiEnabled =
            stringResource(R.string.backup_troubleshoot_documents_ui_enabled)
    val troubleshootSelectedAuthority =
            stringResource(R.string.backup_troubleshoot_selected_authority)
    val troubleshootPersistedPermission =
            stringResource(R.string.backup_troubleshoot_persisted_permission)
    val troubleshootManagedProfile = stringResource(R.string.backup_troubleshoot_managed_profile)
    val troubleshootGuidance = stringResource(R.string.backup_troubleshoot_guidance)
    val valueYes = stringResource(R.string.backup_value_yes)
    val valueNo = stringResource(R.string.backup_value_no)
    val valueUnknown = stringResource(R.string.backup_value_unknown)
    val prefs = remember { BackupPreferences(context) }
    var state by remember { mutableStateOf(prefs.readState()) }
    var latestBackupName by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<RestoreRequest?>(null) }
    var browseBackups by remember { mutableStateOf<List<BackupBrowseEntry>?>(null) }
    var troubleshootReport by remember { mutableStateOf<DriveTroubleshootReport?>(null) }
    val uiEvents = LocalUiEventDispatcher.current
    val scope = rememberCoroutineScope()

    val refreshStateAndLatest: suspend () -> Unit = {
        val baseState = prefs.readState()
        state =
                baseState.copy(
                        targetHealth = BackupManager.evaluateTargetHealth(context, baseState)
                )
        latestBackupName =
                withContext(Dispatchers.IO) {
                    BackupManager.findLatestBackupName(
                            context = context,
                            targetType = baseState.targetType,
                            targetUri = baseState.targetUri
                    )
                }
    }

    val folderPicker =
            rememberLauncherForActivityResult(OpenDocumentTreeWithGrantContract()) { result ->
                val uri = result.uri ?: return@rememberLauncherForActivityResult
                val rwFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val grantedFlags = result.flags and rwFlags
                val persistFlags = if (grantedFlags != 0) grantedFlags else rwFlags
                scope.launch {
                    val grantSucceeded =
                            runCatching {
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                persistFlags
                                        )
                                    }
                                    .isSuccess
                    if (!grantSucceeded) {
                        uiEvents.showSnackbar(targetPermissionFailedMessage)
                        return@launch
                    }
                    val targetUri = uri.toString()
                    val label =
                            runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
                    prefs.setTargetSelection(
                            uri = targetUri,
                            displayName = label,
                            authority = uri.authority
                    )
                    BackupScheduler.ensureScheduled(context)
                    refreshStateAndLatest.invoke()
                    if (BackupManager.evaluateTargetHealth(context, prefs.readState()) !=
                                    BackupTargetHealth.Healthy
                    ) {
                        uiEvents.showSnackbar(targetUnavailableMessage)
                    }
                }
            }
    val launchFolderPicker: () -> Unit = {
        folderPicker.launch(null)
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
    val filePicker =
            rememberLauncherForActivityResult(
                    CreateBackupDocumentContract()
            ) { result ->
                val uri = result.uri
                if (uri != null) {
                    val rwFlags =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    val grantedFlags = result.flags and rwFlags
                    val persistFlags = if (grantedFlags != 0) grantedFlags else rwFlags
                    scope.launch {
                        val grantSucceeded =
                                runCatching {
                                            context.contentResolver.takePersistableUriPermission(
                                                    uri,
                                                    persistFlags
                                            )
                                        }
                                        .isSuccess
                        if (!grantSucceeded) {
                            uiEvents.showSnackbar(targetPermissionFailedMessage)
                            return@launch
                        }
                        val targetUri = uri.toString()
                        val label =
                                runCatching { DocumentFile.fromSingleUri(context, uri)?.name }
                                        .getOrNull()
                        prefs.setFileTargetSelection(
                                uri = targetUri,
                                displayName = label,
                                authority = uri.authority
                        )
                        BackupScheduler.ensureScheduled(context)
                        refreshStateAndLatest.invoke()
                        if (BackupManager.evaluateTargetHealth(context, prefs.readState()) !=
                                        BackupTargetHealth.Healthy
                        ) {
                            uiEvents.showSnackbar(targetUnavailableMessage)
                        }
                    }
                }
            }
    val launchFilePicker: () -> Unit = {
        val fileName = "oms_backup_${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.zip"
        filePicker.launch(fileName)
    }
    val launchBrowseBackups: () -> Unit = {
        val treeUri = state.targetUri
        if (state.targetType != BackupTargetType.SafDirectory || treeUri.isNullOrBlank()) {
            scope.launch { uiEvents.showSnackbar(browseBackupsUnavailableMessage) }
        } else {
            scope.launch {
                val backups =
                        withContext(Dispatchers.IO) {
                            val tree =
                                    DocumentFile.fromTreeUri(context, treeUri.toUri())
                                            ?: return@withContext emptyList()
                            tree.listFiles()
                                    .asSequence()
                                    .filter {
                                        it.isFile &&
                                                (it.name?.endsWith(".zip", ignoreCase = true) ==
                                                        true)
                                    }
                                    .sortedWith(
                                            compareByDescending<DocumentFile> { it.lastModified() }
                                                    .thenByDescending { it.name ?: "" }
                                    )
                                    .mapNotNull { file ->
                                        val name = file.name?.trim().orEmpty()
                                        if (name.isBlank()) null
                                        else
                                                BackupBrowseEntry(
                                                        name = name,
                                                        uriString = file.uri.toString()
                                                )
                                    }
                                    .toList()
                        }
                if (backups.isEmpty()) {
                    uiEvents.showSnackbar(noBackupFoundMessage)
                } else {
                    browseBackups = backups
                }
            }
        }
    }

    LaunchedEffect(Unit) { refreshStateAndLatest.invoke() }

    val backupWorkInfos by
            WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_MANUAL_WORK)
                    .observeAsState(emptyList())
    val restoreWorkInfos by
            WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_RESTORE_WORK)
                    .observeAsState(emptyList())

    val backupInfo = backupWorkInfos.firstOrNull()
    val restoreInfo = restoreWorkInfos.firstOrNull()
    val isBackupRunning = backupInfo?.state == WorkInfo.State.RUNNING
    val isRestoreRunning = restoreInfo?.state == WorkInfo.State.RUNNING
    val backupProgress = backupInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val restoreProgress = restoreInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val backupStage =
            backupInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: backupStageDefault
    val restoreStage =
            restoreInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: restoreStageDefault

    LaunchedEffect(backupInfo?.state, restoreInfo?.state) { refreshStateAndLatest.invoke() }

    LaunchedEffect(backupInfo?.state) {
        when (backupInfo?.state) {
            WorkInfo.State.SUCCEEDED -> uiEvents.showSnackbar(backupCompleteMessage)
            WorkInfo.State.FAILED -> uiEvents.showSnackbar(backupFailedMessage)
            else -> Unit
        }
    }

    LaunchedEffect(restoreInfo?.state) {
        when (restoreInfo?.state) {
            WorkInfo.State.SUCCEEDED -> uiEvents.showSnackbar(restoreCompleteMessage)
            WorkInfo.State.FAILED -> uiEvents.showSnackbar(restoreFailedMessage)
            else -> Unit
        }
    }

    val lastBackupLabel =
            state.lastBackupTime?.let { formatTimestamp(it) }
                    ?: stringResource(R.string.backup_status_never)
    val statusLabel =
            when (state.lastStatus) {
                BackupStatus.Success -> stringResource(R.string.backup_status_success)
                BackupStatus.Failed -> stringResource(R.string.backup_status_failed)
                BackupStatus.Never -> stringResource(R.string.backup_status_never)
            }
    val targetLabel =
            when (state.targetType) {
                BackupTargetType.AppPrivate -> stringResource(R.string.backup_target_app_storage_private)
                BackupTargetType.SafDirectory -> {
                    val label =
                            state.targetDisplayName
                                    ?: state.targetUri?.let { uri ->
                                        runCatching {
                                                    DocumentFile.fromTreeUri(context, uri.toUri())?.name
                                                }
                                                .getOrNull()
                                    }
                    label?.let { stringResource(R.string.backup_target_folder_named, it) }
                            ?: stringResource(R.string.backup_target_folder_not_selected)
                }
                BackupTargetType.SafFile -> {
                    val label =
                            state.targetDisplayName
                                    ?: state.targetUri?.let { uri ->
                                        runCatching {
                                                    DocumentFile.fromSingleUri(context, uri.toUri())
                                                            ?.name
                                                }
                                                .getOrNull()
                                    }
                    label?.let { stringResource(R.string.backup_target_file_named, it) }
                            ?: stringResource(R.string.backup_target_file_not_selected)
                }
            }
    val providerLabel = state.targetAuthority ?: stringResource(R.string.backup_provider_unknown)
    val targetModeLabel =
            when (state.targetType) {
                BackupTargetType.AppPrivate -> stringResource(R.string.backup_target_app_storage)
                BackupTargetType.SafDirectory -> stringResource(R.string.backup_target_folder)
                BackupTargetType.SafFile -> stringResource(R.string.backup_target_file)
            }
    val primaryLocationActionLabel =
            if (state.targetType == BackupTargetType.SafFile && !state.targetUri.isNullOrBlank()) {
                changeLocationLabel
            } else {
                chooseLocationLabel
            }
    val healthText =
            when (state.targetHealth) {
                BackupTargetHealth.Healthy -> stringResource(R.string.backup_health_healthy)
                BackupTargetHealth.NeedsRelink ->
                        stringResource(R.string.backup_health_needs_relink)
                BackupTargetHealth.Unavailable -> stringResource(R.string.backup_health_unavailable)
            }
    val healthHint =
            when (state.targetHealth) {
                BackupTargetHealth.Healthy -> null
                BackupTargetHealth.NeedsRelink -> needsRelinkHint
                BackupTargetHealth.Unavailable -> unavailableHint
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
                        Modifier.padding(padding)
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isLocationConfigured = state.targetType == BackupTargetType.AppPrivate || !state.targetUri.isNullOrBlank()
            val isProtected = state.autoEnabled && state.targetHealth == BackupTargetHealth.Healthy && latestBackupName != null

            Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                            containerColor = if (isProtected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isProtected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                imageVector = if (isProtected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                    text = if (isProtected) "Fully Protected" else "Action Required",
                                    style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                    text = if (isProtected) "Your data is backed up and safe." else "Complete setup to protect your data.",
                                    style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (!isProtected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Setup Checklist:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    imageVector = if (isLocationConfigured) Icons.Default.Check else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Choose a backup location", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    imageVector = if (state.autoEnabled) Icons.Default.Check else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enable automatic backups", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    imageVector = if (latestBackupName != null) Icons.Default.Check else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Run your first backup", style = MaterialTheme.typography.bodySmall)
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
                            scope.launch {
                                val health = BackupManager.evaluateTargetHealth(context, state)
                                if (enabled &&
                                                state.targetType != BackupTargetType.AppPrivate &&
                                                health != BackupTargetHealth.Healthy
                                ) {
                                    if (state.targetType == BackupTargetType.SafFile) {
                                        uiEvents.showSnackbar(pickFileFirstMessage)
                                        launchFilePicker()
                                    } else {
                                        uiEvents.showSnackbar(pickFolderFirstMessage)
                                        launchFolderPicker()
                                    }
                                    return@launch
                                }
                                BackupScheduler.setAutomaticEnabled(context, enabled)
                                refreshStateAndLatest.invoke()
                            }
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
                Button(
                        onClick = { launchFilePicker() },
                        modifier = Modifier.fillMaxWidth()
                ) { Text(primaryLocationActionLabel) }
                Text(
                        text = locationRecommendedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                        colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                                text = "Current Settings:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                                text = currentModeLabelTemplate.format(targetModeLabel),
                                style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                                text = targetLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.targetType != BackupTargetType.AppPrivate) {
                            Text(
                                    text = "Provider: $providerLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val explanation = when (state.targetType) {
                            BackupTargetType.AppPrivate -> "Data is kept in an isolated folder inside the app. If the app is uninstalled or device is wiped, backups will be lost."
                            BackupTargetType.SafDirectory -> "Backups are automatically written to your selected folder. If you chose a cloud provider like Google Drive, it may take some time to sync to the cloud after completion."
                            BackupTargetType.SafFile -> "Data overwrites the single file you selected. This saves space, but you only keep the most recent backup."
                        }
                        Text(
                                text = "How this works: $explanation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.targetType != BackupTargetType.AppPrivate) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                    onClick = {
                                        prefs.setTargetType(BackupTargetType.AppPrivate)
                                        BackupScheduler.ensureScheduled(context)
                                        scope.launch { refreshStateAndLatest.invoke() }
                                    }
                            ) { Text(useAppStorageQuickLabel) }
                        }
                    }
                }
                Text(
                        text = stringResource(R.string.backup_health_label, healthText),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                                when (state.targetHealth) {
                                    BackupTargetHealth.Healthy -> MaterialTheme.colorScheme.primary
                                    BackupTargetHealth.NeedsRelink ->
                                            MaterialTheme.colorScheme.error
                                    BackupTargetHealth.Unavailable ->
                                            MaterialTheme.colorScheme.error
                                }
                )
                Text(
                        text = if (state.targetType == BackupTargetType.SafFile) cloudFileInfo else cloudFolderInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.targetType == BackupTargetType.SafDirectory) {
                    Text(
                            text = driveBrowseHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (state.targetType == BackupTargetType.SafFile) {
                    Text(
                            text = driveSystemPickerHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.targetType != BackupTargetType.AppPrivate &&
                                state.targetHealth != BackupTargetHealth.Healthy
                ) {
                    Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                    text = healthAttentionTitle,
                                    style = MaterialTheme.typography.titleSmall
                            )
                            healthHint?.let { hint ->
                                Text(text = hint, style = MaterialTheme.typography.bodySmall)
                            }
                            if (state.consecutiveAutoFailures > 0) {
                                Text(
                                        text =
                                                stringResource(
                                                        R.string.backup_consecutive_failures_label,
                                                        state.consecutiveAutoFailures
                                                ),
                                        style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (state.targetType == BackupTargetType.SafFile) {
                                TextButton(onClick = { launchFilePicker() }) {
                                    Text(primaryLocationActionLabel)
                                }
                            } else {
                                TextButton(onClick = { launchFolderPicker() }) {
                                    Text(stringResource(R.string.backup_change_folder))
                                }
                            }
                        }
                    }
                }
                TextButton(
                        onClick = {
                            scope.launch {
                                val result = BackupManager.runStorageProbe(context)
                                if (result.success) {
                                    uiEvents.showSnackbar(testWritePassedMessage)
                                } else {
                                    uiEvents.showSnackbar(
                                            buildString {
                                                append(testWriteFailedMessage)
                                                result.message?.takeIf { it.isNotBlank() }?.let {
                                                        detail ->
                                                    append(": ")
                                                    append(detail)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                ) { Text(stringResource(R.string.backup_test_write)) }
                TextButton(
                        onClick = {
                            troubleshootReport =
                                    BackupManager.buildDriveTroubleshootReport(context, state)
                        }
                ) { Text(stringResource(R.string.backup_troubleshoot_drive)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = manifestPolicyTitle, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                            selected = state.manifestPolicy == RestoreManifestPolicy.Strict,
                            onClick = {
                                prefs.setManifestPolicy(RestoreManifestPolicy.Strict)
                                scope.launch { refreshStateAndLatest.invoke() }
                            },
                            label = { Text(manifestPolicyStrictLabel) }
                    )
                    FilterChip(
                            selected =
                                    state.manifestPolicy == RestoreManifestPolicy.LegacyCompatible,
                            onClick = {
                                prefs.setManifestPolicy(RestoreManifestPolicy.LegacyCompatible)
                                scope.launch { refreshStateAndLatest.invoke() }
                            },
                            label = { Text(manifestPolicyLegacyLabel) }
                    )
                }
                Text(
                        text =
                                if (state.manifestPolicy == RestoreManifestPolicy.Strict) {
                                    strictModeInfo
                                } else {
                                    legacyModeWarning
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                                if (state.manifestPolicy == RestoreManifestPolicy.Strict) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                )
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
                        val health = BackupManager.evaluateTargetHealth(context, state)
                        if (state.targetType != BackupTargetType.AppPrivate &&
                                        (state.targetUri == null ||
                                                health != BackupTargetHealth.Healthy)
                        ) {
                            if (state.targetType == BackupTargetType.SafFile) {
                                scope.launch { uiEvents.showSnackbar(pickFileFirstMessage) }
                                launchFilePicker()
                            } else {
                                scope.launch { uiEvents.showSnackbar(pickFolderFirstMessage) }
                                launchFolderPicker()
                            }
                            return@Button
                        }
                        BackupScheduler.enqueueManualBackup(context)
                        scope.launch { uiEvents.showSnackbar(backupStartedMessage) }
                    },
                    enabled = !isBackupRunning && !isRestoreRunning,
                    modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_run_now)) }

            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                        text = stringResource(R.string.restore_title),
                        style = MaterialTheme.typography.titleMedium
                )
                val latestLabel = latestBackupName ?: stringResource(R.string.backup_none)
                Text(
                        text = stringResource(R.string.backup_latest_selected_backup, latestLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                            onClick = {
                                if (latestBackupName == null) {
                                    scope.launch { uiEvents.showSnackbar(noBackupFoundMessage) }
                                    return@Button
                                }
                                pendingRestore = RestoreRequest(null)
                            },
                            enabled =
                                    !isBackupRunning &&
                                            !isRestoreRunning &&
                                            latestBackupName != null,
                            modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.restore_latest)) }
                    Button(
                            onClick = { launchBrowseBackups() },
                            enabled =
                                    !isBackupRunning &&
                                            !isRestoreRunning &&
                                            state.targetType == BackupTargetType.SafDirectory &&
                                            !state.targetUri.isNullOrBlank(),
                            modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.backup_browse_backups)) }
                }
                Button(
                        onClick = { restorePicker.launch(arrayOf("application/zip")) },
                        enabled = !isBackupRunning && !isRestoreRunning,
                        modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.restore_from_file)) }
                if (state.targetType == BackupTargetType.SafDirectory &&
                                state.targetUri.isNullOrBlank()
                ) {
                    Text(
                            text = browseBackupsUnavailableMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                        text = stringResource(R.string.restore_replaces_data),
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
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(stringResource(R.string.restore_confirm_title), color = MaterialTheme.colorScheme.error) },
                text = {
                    Column {
                        Text(
                                stringResource(R.string.restore_confirm_body),
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                    text = "WARNING: This will permanently overwrite all your current data with the selected backup. Any changes made since the backup will be lost forever. The app will restart after the restore.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                            onClick = {
                                BackupScheduler.enqueueRestore(context, restoreRequest.uriString)
                                scope.launch { uiEvents.showSnackbar(restoreStartedMessage) }
                                pendingRestore = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                            )
                    ) { Text(stringResource(R.string.restore_action)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestore = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
        )
    }

    val backupChoices = browseBackups
    if (backupChoices != null) {
        AlertDialog(
                onDismissRequest = { browseBackups = null },
                title = { Text(stringResource(R.string.backup_browse_backups)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        backupChoices.forEach { entry ->
                            TextButton(
                                    onClick = {
                                        pendingRestore = RestoreRequest(entry.uriString)
                                        browseBackups = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                            ) { Text(text = entry.name) }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { browseBackups = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                confirmButton = {}
        )
    }

    val report = troubleshootReport
    if (report != null) {
        val reportBody = buildString {
            fun boolLabel(value: Boolean): String = if (value) valueYes else valueNo
            appendLine("$troubleshootDriveInstalled: ${boolLabel(report.driveAppInstalled)}")
            appendLine("$troubleshootDriveEnabled: ${boolLabel(report.driveAppEnabled)}")
            appendLine("$troubleshootDocumentsUiEnabled: ${boolLabel(report.documentsUiEnabled)}")
            appendLine(
                    "$troubleshootPersistedPermission: ${boolLabel(report.persistedPermissionValid)}"
            )
            appendLine("$troubleshootManagedProfile: ${boolLabel(report.managedProfile)}")
            appendLine(
                    "$troubleshootSelectedAuthority: ${
                        report.selectedAuthority?.takeIf { it.isNotBlank() } ?: valueUnknown
                    }"
            )
            if (report.guidance.isNotEmpty()) {
                appendLine()
                appendLine("$troubleshootGuidance:")
                report.guidance.forEach { line -> appendLine("- $line") }
            }
        }
        AlertDialog(
                onDismissRequest = { troubleshootReport = null },
                title = { Text(troubleshootingTitle) },
                text = { Text(reportBody) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                troubleshootReport = null
                                if (state.targetType == BackupTargetType.SafFile) {
                                    launchFilePicker()
                                } else {
                                    launchFolderPicker()
                                }
                            }
                    ) {
                        Text(
                                stringResource(
                                        if (state.targetType == BackupTargetType.SafFile)
                                                R.string.backup_change_file
                                        else R.string.backup_change_folder
                                )
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { troubleshootReport = null }) {
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LinearProgressIndicator(
                    progress = { clampedProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )
            Text(
                    text = stringResource(R.string.backup_progress_percent, clampedProgress),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

private data class RestoreRequest(val uriString: String?)

private data class BackupBrowseEntry(val name: String, val uriString: String)

private data class OpenTreeResult(val uri: Uri?, val flags: Int)
private data class CreateBackupDocumentResult(val uri: Uri?, val flags: Int)

private class OpenDocumentTreeWithGrantContract : ActivityResultContract<Uri?, OpenTreeResult>() {
    override fun createIntent(context: android.content.Context, input: Uri?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            if (input != null) {
                putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, input)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): OpenTreeResult {
        if (resultCode != Activity.RESULT_OK) return OpenTreeResult(uri = null, flags = 0)
        return OpenTreeResult(
                uri = intent?.data,
                flags = intent?.flags ?: 0
        )
    }
}

private class CreateBackupDocumentContract :
        ActivityResultContract<String, CreateBackupDocumentResult>() {
    override fun createIntent(context: android.content.Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, input)
            addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CreateBackupDocumentResult {
        if (resultCode != Activity.RESULT_OK) return CreateBackupDocumentResult(uri = null, flags = 0)
        return CreateBackupDocumentResult(
                uri = intent?.data,
                flags = intent?.flags ?: 0
        )
    }
}
