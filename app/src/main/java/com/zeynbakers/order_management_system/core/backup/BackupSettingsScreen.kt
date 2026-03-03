package com.zeynbakers.order_management_system.core.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.LocalUiEventDispatcher
import com.zeynbakers.order_management_system.core.ui.showSnackbar
import com.zeynbakers.order_management_system.core.util.formatHourMinuteAmPm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uiEvents = LocalUiEventDispatcher.current
    val prefs = remember { BackupPreferences(context) }
    val scope = rememberCoroutineScope()

    val backupStageDefault = stringResource(R.string.backup_stage_backing_up)
    val restoreStageDefault = stringResource(R.string.backup_stage_restoring)
    val backupCompleteMessage = stringResource(R.string.backup_complete)
    val backupFailedMessage = stringResource(R.string.backup_failed)
    val restoreCompleteMessage = stringResource(R.string.restore_complete_restart)
    val restoreFailedMessage = stringResource(R.string.restore_failed)
    val backupStartedMessage = stringResource(R.string.backup_started)
    val restoreStartedMessage = stringResource(R.string.restore_started)
    val pickTargetFirstMessage = stringResource(R.string.backup_pick_target_first)
    val filePermissionFailedMessage = stringResource(R.string.backup_file_permission_failed)
    val transientPermissionWarning = stringResource(R.string.backup_permission_transient_warning)
    val testWritePassedMessage = stringResource(R.string.backup_test_write_passed)
    val testWriteFailedMessage = stringResource(R.string.backup_test_write_failed)
    val noBackupsFoundMessage = stringResource(R.string.backup_no_backups_found)
    val restorePreviewFailedMessage = stringResource(R.string.backup_restore_preview_failed)
    val restoreUsingLatestHint = stringResource(R.string.backup_restore_using_latest_hint)
    val restorePreparingMessage = stringResource(R.string.backup_restore_preparing)
    val needsRelinkHint = stringResource(R.string.backup_health_needs_relink_hint)
    val unavailableHint = stringResource(R.string.backup_health_unavailable_hint)
    val encryptionPassphraseTooShort = stringResource(R.string.backup_encryption_passphrase_too_short)
    val encryptionPassphraseMismatch = stringResource(R.string.backup_encryption_passphrase_mismatch)
    val encryptionMissingPassphraseMessage = stringResource(R.string.backup_encryption_missing_passphrase)
    val encryptionEnabledMessage = stringResource(R.string.backup_encryption_enabled)
    val encryptionDisabledMessage = stringResource(R.string.backup_encryption_disabled)
    val encryptionPassphraseUpdated = stringResource(R.string.backup_encryption_passphrase_updated)
    val insecureOverrideRequiredMessage = stringResource(R.string.backup_insecure_override_required)

    var state by remember { mutableStateOf(prefs.readState()) }
    var latestBackupName by remember { mutableStateOf<String?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<String?>(null) }
    var selectedRestoreName by remember { mutableStateOf<String?>(null) }
    var probeStatus by remember { mutableStateOf<ProbeStatus?>(null) }
    var isCheckingProbe by remember { mutableStateOf(false) }
    var isPreparingRestorePreview by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<RestorePreviewRequest?>(null) }
    var lastBackupObserved by remember { mutableStateOf<Pair<String, WorkInfo.State>?>(null) }
    var lastRestoreObserved by remember { mutableStateOf<Pair<String, WorkInfo.State>?>(null) }
    var inlineRestoreRunning by remember { mutableStateOf(false) }
    var inlineRestoreProgress by remember { mutableStateOf(0) }
    var inlineRestoreStage by remember { mutableStateOf(restoreStageDefault) }
    var pendingSaveAction by remember { mutableStateOf(SaveActionAfterPick.None) }
    var encryptionPassphraseInput by remember { mutableStateOf("") }
    var encryptionPassphraseConfirmInput by remember { mutableStateOf("") }
    var showDisableEncryptionConfirm by remember { mutableStateOf(false) }
    val suggestedBackupFileName = "backup_latest.oms"

    val refreshState: suspend () -> Unit = {
        val latestState = prefs.readState()
        state = latestState.copy(targetHealth = BackupManager.evaluateTargetHealth(context, latestState))
        latestBackupName =
            withContext(Dispatchers.IO) {
                BackupManager.findLatestBackupName(
                    context = context,
                    targetType = latestState.targetType,
                    targetUri = latestState.targetUri
                )
            }
    }

    val saveFilePicker =
        rememberLauncherForActivityResult(SaveBackupDocumentContract()) { result ->
            val uri = result.uri ?: return@rememberLauncherForActivityResult
            val requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            scope.launch {
                val actionAfterPick = pendingSaveAction
                val persisted = persistSafUriPermission(context, uri, result.flags, requiredFlags)
                if (!persisted) {
                    pendingSaveAction = SaveActionAfterPick.None
                    uiEvents.showSnackbar(filePermissionFailedMessage)
                    return@launch
                }

                val displayName =
                    runCatching { DocumentFile.fromSingleUri(context, uri)?.name?.takeIf { it.isNotBlank() } }
                        .getOrNull()
                prefs.setFileTargetSelection(
                    uri = uri.toString(),
                    displayName = displayName,
                    authority = uri.authority
                )
                val probe = BackupManager.runStorageProbe(context)
                val probeMessage =
                    probe.message ?: if (probe.success) testWritePassedMessage else testWriteFailedMessage
                probeStatus =
                    ProbeStatus(
                        success = probe.success,
                        message = probeMessage,
                        checkedAt = System.currentTimeMillis()
                    )
                if (!probe.success) {
                    pendingSaveAction = SaveActionAfterPick.None
                    uiEvents.showSnackbar(probeMessage)
                    refreshState.invoke()
                    return@launch
                }

                if (actionAfterPick == SaveActionAfterPick.EnableAuto) {
                    prefs.setAutoEnabled(true)
                }
                BackupScheduler.ensureScheduled(context)
                refreshState.invoke()
                if (actionAfterPick == SaveActionAfterPick.RunBackupNow) {
                    BackupScheduler.enqueueManualBackup(context)
                    uiEvents.showSnackbar(backupStartedMessage)
                }
                pendingSaveAction = SaveActionAfterPick.None
            }
        }

    val directoryPicker =
        rememberLauncherForActivityResult(OpenBackupDirectoryContract()) { result ->
            val uri = result.uri ?: return@rememberLauncherForActivityResult
            val requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            scope.launch {
                val actionAfterPick = pendingSaveAction
                val persisted = persistSafUriPermission(context, uri, result.flags, requiredFlags)
                if (!persisted) {
                    pendingSaveAction = SaveActionAfterPick.None
                    uiEvents.showSnackbar(filePermissionFailedMessage)
                    return@launch
                }
                val displayName =
                    runCatching { DocumentFile.fromTreeUri(context, uri)?.name?.takeIf { it.isNotBlank() } }
                        .getOrNull()
                prefs.setTargetSelection(
                    uri = uri.toString(),
                    displayName = displayName,
                    authority = uri.authority
                )
                val probe = BackupManager.runStorageProbe(context)
                val probeMessage =
                    probe.message ?: if (probe.success) testWritePassedMessage else testWriteFailedMessage
                probeStatus =
                    ProbeStatus(
                        success = probe.success,
                        message = probeMessage,
                        checkedAt = System.currentTimeMillis()
                    )
                if (!probe.success) {
                    pendingSaveAction = SaveActionAfterPick.None
                    uiEvents.showSnackbar(probeMessage)
                    refreshState.invoke()
                    return@launch
                }
                if (actionAfterPick == SaveActionAfterPick.EnableAuto) {
                    prefs.setAutoEnabled(true)
                }
                BackupScheduler.ensureScheduled(context)
                refreshState.invoke()
                if (actionAfterPick == SaveActionAfterPick.RunBackupNow) {
                    BackupScheduler.enqueueManualBackup(context)
                    uiEvents.showSnackbar(backupStartedMessage)
                }
                pendingSaveAction = SaveActionAfterPick.None
            }
        }

    val restorePicker =
        rememberLauncherForActivityResult(OpenBackupDocumentContract()) { result ->
            val uri = result.uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                val persisted =
                    persistSafUriPermission(
                        context = context,
                        uri = uri,
                        resultFlags = result.flags,
                        requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                if (!persisted) {
                    uiEvents.showSnackbar(transientPermissionWarning)
                }
                selectedRestoreUri = uri.toString()
                selectedRestoreName =
                    runCatching {
                        DocumentFile.fromSingleUri(context, uri)?.name?.takeIf { it.isNotBlank() }
                    }.getOrNull()
            }
        }

    LaunchedEffect(Unit) { refreshState.invoke() }

    val backupWorkInfos by
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_MANUAL_WORK)
            .observeAsState(emptyList())
    val restoreWorkInfos by
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_RESTORE_WORK)
            .observeAsState(emptyList())
    val dailyWorkInfos by
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(BackupScheduler.UNIQUE_DAILY_WORK)
            .observeAsState(emptyList())

    val backupInfo = pickRelevantWorkInfo(backupWorkInfos)
    val restoreInfo = pickRelevantWorkInfo(restoreWorkInfos)
    val dailyInfo = pickRelevantWorkInfo(dailyWorkInfos)
    val isBackupRunning = backupInfo?.state == WorkInfo.State.RUNNING
    val isRestoreRunning = inlineRestoreRunning || restoreInfo?.state == WorkInfo.State.RUNNING
    val isBusy = isBackupRunning || isRestoreRunning
    val backupProgress = backupInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val restoreProgress =
        if (inlineRestoreRunning) inlineRestoreProgress
        else restoreInfo?.progress?.getInt(BackupScheduler.KEY_PROGRESS, 0) ?: 0
    val backupStage = backupInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: backupStageDefault
    val restoreStage =
        if (inlineRestoreRunning) inlineRestoreStage
        else restoreInfo?.progress?.getString(BackupScheduler.KEY_STAGE) ?: restoreStageDefault

    LaunchedEffect(backupInfo?.state, restoreInfo?.state, dailyInfo?.state) {
        refreshState.invoke()
    }

    LaunchedEffect(backupInfo?.id, backupInfo?.state) {
        val id = backupInfo?.id?.toString() ?: return@LaunchedEffect
        val state = backupInfo.state
        val previous = lastBackupObserved
        val shouldNotify =
            previous != null &&
                previous != (id to state) &&
                (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED)
        if (shouldNotify) {
            when (state) {
                WorkInfo.State.SUCCEEDED -> uiEvents.showSnackbar(backupCompleteMessage)
                WorkInfo.State.FAILED -> {
                    val details = backupInfo.outputData.getString(BackupScheduler.KEY_ERROR_MESSAGE)
                    uiEvents.showSnackbar(details ?: backupFailedMessage)
                }
                else -> Unit
            }
        }
        lastBackupObserved = id to state
    }

    LaunchedEffect(restoreInfo?.id, restoreInfo?.state) {
        val id = restoreInfo?.id?.toString() ?: return@LaunchedEffect
        val state = restoreInfo.state
        val previous = lastRestoreObserved
        val shouldNotify =
            previous != null &&
                previous != (id to state) &&
                (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED)
        if (shouldNotify) {
            when (state) {
                WorkInfo.State.SUCCEEDED -> uiEvents.showSnackbar(restoreCompleteMessage)
                WorkInfo.State.FAILED -> {
                    val details = restoreInfo.outputData.getString(BackupScheduler.KEY_ERROR_MESSAGE)
                    uiEvents.showSnackbar(details ?: restoreFailedMessage)
                }
                else -> Unit
            }
        }
        lastRestoreObserved = id to state
    }

    val backupFileName =
        state.targetDisplayName
            ?: state.targetUri?.let { uri ->
                runCatching { DocumentFile.fromSingleUri(context, uri.toUri())?.name }
                    .getOrNull()
            }
            ?: stringResource(R.string.backup_target_file_not_selected)
    val targetModeLabel =
        when (state.targetType) {
            BackupTargetType.AppPrivate -> stringResource(R.string.backup_target_mode_app_private)
            BackupTargetType.SafDirectory -> stringResource(R.string.backup_target_mode_directory)
            BackupTargetType.SafFile -> stringResource(R.string.backup_target_mode_file)
        }
    val manifestPolicyLabel =
        when (state.manifestPolicy) {
            RestoreManifestPolicy.Strict -> stringResource(R.string.backup_manifest_policy_strict)
            RestoreManifestPolicy.LegacyCompatible -> stringResource(R.string.backup_manifest_policy_legacy)
        }
    val encryptionModeLabel =
        if (state.encryptionEnabled) {
            stringResource(R.string.backup_encryption_mode_encrypted)
        } else {
            stringResource(R.string.backup_encryption_mode_plain)
        }
    val providerLabel = state.targetAuthority ?: stringResource(R.string.backup_provider_unknown)
    val healthLabel = healthLabelFor(context, state.targetHealth)
    val healthHint =
        when (state.targetHealth) {
            BackupTargetHealth.Healthy -> null
            BackupTargetHealth.NeedsRelink -> needsRelinkHint
            BackupTargetHealth.Unavailable -> unavailableHint
        }
    val encryptionReady =
        (state.encryptionEnabled && state.encryptionConfigured) ||
            (!state.encryptionEnabled && state.insecureOverrideEnabled)

    val targetReady =
        when (state.targetType) {
            BackupTargetType.AppPrivate -> encryptionReady
            BackupTargetType.SafFile,
            BackupTargetType.SafDirectory ->
                !state.targetUri.isNullOrBlank() &&
                    state.targetHealth == BackupTargetHealth.Healthy &&
                    encryptionReady
        }
    val needsTargetSelection =
        when (state.targetType) {
            BackupTargetType.AppPrivate -> false
            BackupTargetType.SafFile,
            BackupTargetType.SafDirectory ->
                state.targetUri.isNullOrBlank() || state.targetHealth != BackupTargetHealth.Healthy
        }
    val lastBackupTime = state.lastBackupTime
    val lastBackupLabel =
        lastBackupTime?.let { formatTimestamp(it) } ?: stringResource(R.string.backup_status_never)
    val nextBackupLabel =
        when {
            !state.autoEnabled -> stringResource(R.string.backup_next_scheduled_off)
            !targetReady -> stringResource(R.string.backup_next_scheduled_waiting)
            lastBackupTime != null ->
                formatTimestamp(
                    lastBackupTime + TimeUnit.HOURS.toMillis(BackupScheduler.DAILY_INTERVAL_HOURS)
                )
            else -> stringResource(R.string.backup_next_scheduled_within_day)
        }
    val latestBackupLabel = latestBackupName ?: stringResource(R.string.backup_none)
    val restoringLatest = selectedRestoreUri.isNullOrBlank() && latestBackupName != null
    val selectedRestoreLabel =
        when {
            !selectedRestoreName.isNullOrBlank() -> selectedRestoreName
            !selectedRestoreUri.isNullOrBlank() -> selectedRestoreUri
            else -> latestBackupLabel
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.backup_title)) },
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_storage_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_storage_section_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_storage_file_label),
                        value = backupFileName
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_storage_mode_label),
                        value = targetModeLabel
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_storage_provider_label),
                        value = providerLabel
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_storage_health_label),
                        value = healthLabel
                    )
                    if (!healthHint.isNullOrBlank()) {
                        Text(
                            text = healthHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    val probeLabel =
                        probeStatus?.let {
                            "${it.message} (${formatTimestamp(it.checkedAt)})"
                        } ?: stringResource(R.string.backup_test_write_not_run)
                    LabeledValue(
                        label = stringResource(R.string.backup_storage_test_label),
                        value = probeLabel
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            pendingSaveAction = SaveActionAfterPick.None
                            saveFilePicker.launch(suggestedBackupFileName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                        ) {
                            Text(
                                text =
                                    if (state.targetType == BackupTargetType.SafFile && targetReady) {
                                    stringResource(R.string.backup_change_file)
                                } else {
                                    stringResource(R.string.backup_choose_file)
                                    }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            pendingSaveAction = SaveActionAfterPick.None
                            directoryPicker.launch(Unit)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                    ) {
                        Text(
                            text =
                                if (state.targetType == BackupTargetType.SafDirectory && targetReady) {
                                    stringResource(R.string.backup_change_folder)
                                } else {
                                    stringResource(R.string.backup_choose_folder)
                                }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val canProbe =
                                (state.targetType == BackupTargetType.SafFile || state.targetType == BackupTargetType.SafDirectory) &&
                                    !state.targetUri.isNullOrBlank()
                            if (!canProbe) {
                                scope.launch { uiEvents.showSnackbar(pickTargetFirstMessage) }
                                return@OutlinedButton
                            }
                            scope.launch {
                                isCheckingProbe = true
                                val probe = BackupManager.runStorageProbe(context)
                                isCheckingProbe = false
                                probeStatus =
                                    ProbeStatus(
                                        success = probe.success,
                                        message =
                                            probe.message
                                                ?: if (probe.success) testWritePassedMessage else testWriteFailedMessage,
                                        checkedAt = System.currentTimeMillis()
                                    )
                                uiEvents.showSnackbar(
                                    probe.message ?: if (probe.success) testWritePassedMessage else testWriteFailedMessage
                                )
                                refreshState.invoke()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy && !isCheckingProbe
                    ) {
                        Text(text = stringResource(R.string.backup_test_write))
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_manifest_policy_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_manifest_policy_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_manifest_policy_current),
                        value = manifestPolicyLabel
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (state.manifestPolicy == RestoreManifestPolicy.Strict) {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.backup_manifest_policy_strict))
                            }
                            OutlinedButton(
                                onClick = {
                                    prefs.setManifestPolicy(RestoreManifestPolicy.LegacyCompatible)
                                    scope.launch { refreshState.invoke() }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.backup_manifest_policy_legacy))
                            }
                        } else {
                            Button(
                                onClick = {
                                    prefs.setManifestPolicy(RestoreManifestPolicy.Strict)
                                    scope.launch { refreshState.invoke() }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.backup_manifest_policy_strict))
                            }
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.backup_manifest_policy_legacy))
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.backup_manifest_policy_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_encryption_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_encryption_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_encryption_current),
                        value = encryptionModeLabel
                    )
                    if (state.encryptionEnabled && !state.encryptionConfigured) {
                        Text(
                            text = stringResource(R.string.backup_encryption_missing_passphrase),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (state.encryptionEnabled) {
                        Text(
                            text = stringResource(R.string.backup_encryption_enabled_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!state.insecureOverrideEnabled) {
                        Text(
                            text = insecureOverrideRequiredMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.backup_encryption_plain_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = encryptionPassphraseInput,
                        onValueChange = { encryptionPassphraseInput = it },
                        label = { Text(stringResource(R.string.backup_encryption_passphrase_label)) },
                        placeholder = { Text(stringResource(R.string.backup_encryption_passphrase_placeholder)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = encryptionPassphraseConfirmInput,
                        onValueChange = { encryptionPassphraseConfirmInput = it },
                        label = { Text(stringResource(R.string.backup_encryption_passphrase_confirm_label)) },
                        placeholder = { Text(stringResource(R.string.backup_encryption_passphrase_placeholder)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val primaryEncryptionActionLabel =
                        if (state.encryptionEnabled) {
                            stringResource(R.string.backup_encryption_update_passphrase)
                        } else {
                            stringResource(R.string.backup_encryption_enable_action)
                        }
                    Button(
                        onClick = {
                            val passphrase = encryptionPassphraseInput.trim()
                            val confirm = encryptionPassphraseConfirmInput.trim()
                            if (passphrase.length < 8) {
                                scope.launch { uiEvents.showSnackbar(encryptionPassphraseTooShort) }
                                return@Button
                            }
                            if (passphrase != confirm) {
                                scope.launch { uiEvents.showSnackbar(encryptionPassphraseMismatch) }
                                return@Button
                            }
                            prefs.setEncryptionPassphrase(passphrase)
                            if (!state.encryptionEnabled) {
                                prefs.setEncryptionEnabled(true)
                                prefs.setInsecureOverrideEnabled(false)
                            }
                            encryptionPassphraseInput = ""
                            encryptionPassphraseConfirmInput = ""
                            scope.launch {
                                BackupScheduler.ensureScheduled(context)
                                refreshState.invoke()
                                val message =
                                    if (state.encryptionEnabled) {
                                        encryptionPassphraseUpdated
                                    } else {
                                        encryptionEnabledMessage
                                    }
                                uiEvents.showSnackbar(message)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                    ) {
                        Text(text = primaryEncryptionActionLabel)
                    }
                    if (state.encryptionEnabled) {
                        OutlinedButton(
                            onClick = { showDisableEncryptionConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy
                        ) {
                            Text(text = stringResource(R.string.backup_encryption_disable_action))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_run_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_run_section_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_run_last_label),
                        value = lastBackupLabel
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_run_next_label),
                        value = nextBackupLabel
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_run_latest_label),
                        value = latestBackupLabel
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.backup_automatic_daily),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = state.autoEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !targetReady) {
                                    if (!encryptionReady) {
                                        val message =
                                            if (state.encryptionEnabled) {
                                                encryptionMissingPassphraseMessage
                                            } else {
                                                insecureOverrideRequiredMessage
                                            }
                                        scope.launch { uiEvents.showSnackbar(message) }
                                    } else if (needsTargetSelection) {
                                        pendingSaveAction = SaveActionAfterPick.EnableAuto
                                        scope.launch { uiEvents.showSnackbar(pickTargetFirstMessage) }
                                        if (state.targetType == BackupTargetType.SafDirectory) {
                                            directoryPicker.launch(Unit)
                                        } else {
                                            saveFilePicker.launch(suggestedBackupFileName)
                                        }
                                    }
                                    return@Switch
                                }
                                pendingSaveAction = SaveActionAfterPick.None
                                prefs.setAutoEnabled(enabled)
                                BackupScheduler.ensureScheduled(context)
                                scope.launch { refreshState.invoke() }
                            },
                            enabled = !isBusy
                        )
                    }
                    if (isBackupRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { backupProgress.coerceIn(0, 100) / 100f }
                        )
                        Text(
                            text = "$backupStage (${backupProgress.coerceIn(0, 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (!targetReady) {
                                if (!encryptionReady) {
                                    val message =
                                        if (state.encryptionEnabled) {
                                            encryptionMissingPassphraseMessage
                                        } else {
                                            insecureOverrideRequiredMessage
                                        }
                                    scope.launch { uiEvents.showSnackbar(message) }
                                } else if (needsTargetSelection) {
                                    pendingSaveAction = SaveActionAfterPick.RunBackupNow
                                    scope.launch { uiEvents.showSnackbar(pickTargetFirstMessage) }
                                    if (state.targetType == BackupTargetType.SafDirectory) {
                                        directoryPicker.launch(Unit)
                                    } else {
                                        saveFilePicker.launch(suggestedBackupFileName)
                                    }
                                }
                                return@Button
                            }
                            pendingSaveAction = SaveActionAfterPick.None
                            BackupScheduler.enqueueManualBackup(context)
                            scope.launch { uiEvents.showSnackbar(backupStartedMessage) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                    ) {
                        Text(
                            text =
                                if (targetReady) {
                                    stringResource(R.string.backup_run_now)
                                } else {
                                    stringResource(R.string.backup_run_choose_then_run)
                                }
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.backup_restore_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.backup_restore_section_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LabeledValue(
                        label = stringResource(R.string.backup_restore_source_label),
                        value = selectedRestoreLabel ?: stringResource(R.string.backup_none)
                    )
                    OutlinedButton(
                        onClick = { restorePicker.launch(Unit) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                    ) {
                        Text(text = stringResource(R.string.backup_restore_choose_file))
                    }
                    Text(
                        text = stringResource(R.string.backup_restore_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (restoringLatest) {
                        Text(
                            text = restoreUsingLatestHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isRestoreRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { restoreProgress.coerceIn(0, 100) / 100f }
                        )
                        Text(
                            text = "$restoreStage (${restoreProgress.coerceIn(0, 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            val restoreUri = selectedRestoreUri
                            if (restoreUri.isNullOrBlank() && latestBackupName == null) {
                                scope.launch { uiEvents.showSnackbar(noBackupsFoundMessage) }
                                return@Button
                            }
                            scope.launch {
                                isPreparingRestorePreview = true
                                val preview =
                                    runCatching { BackupManager.buildRestorePreview(context, restoreUri) }
                                        .getOrElse {
                                            isPreparingRestorePreview = false
                                            uiEvents.showSnackbar(it.message ?: restorePreviewFailedMessage)
                                            return@launch
                                        }
                                isPreparingRestorePreview = false
                                pendingRestore =
                                    RestorePreviewRequest(
                                        uriString = restoreUri,
                                        preview = preview
                                    )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy && !isPreparingRestorePreview
                    ) {
                        Text(
                            text =
                                when {
                                    isPreparingRestorePreview -> restorePreparingMessage
                                    restoringLatest -> stringResource(R.string.restore_latest)
                                    else -> stringResource(R.string.restore_action)
                                }
                        )
                    }
                }
            }
        }
    }

    if (showDisableEncryptionConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableEncryptionConfirm = false },
            title = { Text(text = stringResource(R.string.backup_disable_encryption_confirm_title)) },
            text = { Text(text = stringResource(R.string.backup_disable_encryption_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisableEncryptionConfirm = false
                        prefs.setEncryptionEnabled(false)
                        prefs.setInsecureOverrideEnabled(true)
                        scope.launch {
                            BackupScheduler.ensureScheduled(context)
                            refreshState.invoke()
                            uiEvents.showSnackbar(encryptionDisabledMessage)
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.backup_disable_encryption_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryptionConfirm = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val restoreRequest = pendingRestore
    if (restoreRequest != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(text = stringResource(R.string.backup_restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.backup_restore_confirm_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.backup_restore_confirm_irreversible),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.backup_restore_preview_source,
                                restoreRequest.preview.sourceName
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.backup_restore_preview_exported_at,
                                restoreRequest.preview.exportedAt?.let { formatTimestamp(it) }
                                    ?: stringResource(R.string.backup_none)
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.backup_restore_preview_db_version,
                                restoreRequest.preview.dbVersion?.toString()
                                    ?: stringResource(R.string.backup_none)
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.backup_restore_preview_app_version,
                                restoreRequest.preview.appVersionName ?: stringResource(R.string.backup_none)
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.backup_restore_preview_counts,
                                restoreRequest.preview.customersCount,
                                restoreRequest.preview.ordersCount,
                                restoreRequest.preview.paymentsCount
                            ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestore = null
                        val restoreUri = restoreRequest.uriString
                        if (restoreUri.isNullOrBlank()) {
                            BackupScheduler.enqueueRestore(context, null)
                            scope.launch { uiEvents.showSnackbar(restoreStartedMessage) }
                            return@Button
                        }
                        scope.launch {
                            inlineRestoreRunning = true
                            inlineRestoreProgress = 0
                            inlineRestoreStage = restoreStageDefault
                            uiEvents.showSnackbar(restoreStartedMessage)
                            val result =
                                BackupManager.runRestore(
                                    context = context,
                                    uriString = restoreUri
                                ) { progress, stage ->
                                    inlineRestoreProgress = progress
                                    inlineRestoreStage = stage
                                }
                            inlineRestoreRunning = false
                            inlineRestoreProgress = 0
                            inlineRestoreStage = restoreStageDefault
                            if (result.success) {
                                uiEvents.showSnackbar(restoreCompleteMessage)
                            } else {
                                uiEvents.showSnackbar(result.message ?: restoreFailedMessage)
                            }
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) {
                    Text(text = stringResource(R.string.restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private enum class SaveActionAfterPick {
    None,
    EnableAuto,
    RunBackupNow
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun healthLabelFor(context: Context, health: BackupTargetHealth): String {
    return when (health) {
        BackupTargetHealth.Healthy -> context.getString(R.string.backup_health_healthy)
        BackupTargetHealth.NeedsRelink -> context.getString(R.string.backup_health_needs_relink)
        BackupTargetHealth.Unavailable -> context.getString(R.string.backup_health_unavailable)
    }
}

private data class ProbeStatus(
    val success: Boolean,
    val message: String,
    val checkedAt: Long
)

private data class RestorePreviewRequest(
    val uriString: String?,
    val preview: RestorePreview
)

private data class PickerResult(
    val uri: Uri?,
    val flags: Int
)

private class SaveBackupDocumentContract : ActivityResultContract<String, PickerResult>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, input)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(DocumentsContract.EXTRA_PROMPT, context.getString(R.string.backup_choose_file))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickerResult {
        if (resultCode != Activity.RESULT_OK) return PickerResult(null, 0)
        return PickerResult(uri = intent?.data, flags = intent?.flags ?: 0)
    }
}

private class OpenBackupDocumentContract : ActivityResultContract<Unit, PickerResult>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(DocumentsContract.EXTRA_PROMPT, context.getString(R.string.restore_from_file))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickerResult {
        if (resultCode != Activity.RESULT_OK) return PickerResult(null, 0)
        return PickerResult(uri = intent?.data, flags = intent?.flags ?: 0)
    }
}

private class OpenBackupDirectoryContract : ActivityResultContract<Unit, PickerResult>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra(DocumentsContract.EXTRA_PROMPT, context.getString(R.string.backup_choose_folder))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickerResult {
        if (resultCode != Activity.RESULT_OK) return PickerResult(null, 0)
        return PickerResult(uri = intent?.data, flags = intent?.flags ?: 0)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timePart = formatHourMinuteAmPm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    return "$datePart, $timePart"
}

private fun pickRelevantWorkInfo(items: List<WorkInfo>): WorkInfo? {
    if (items.isEmpty()) return null
    return items.firstOrNull { it.state == WorkInfo.State.RUNNING }
        ?: items.firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
        ?: items.lastOrNull()
}
