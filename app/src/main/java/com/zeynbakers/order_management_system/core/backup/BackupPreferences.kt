package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri

class BackupPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readState(): BackupState {
        val enabled = prefs.getBoolean(KEY_AUTO_ENABLED, false)
        val targetType =
            prefs.getString(KEY_TARGET_TYPE, BackupTargetType.AppPrivate.name)
                ?.let { runCatching { BackupTargetType.valueOf(it) }.getOrNull() }
                ?: BackupTargetType.AppPrivate
        val targetUri = prefs.getString(KEY_TARGET_URI, null)
        val targetAuthority =
            prefs.getString(KEY_TARGET_AUTHORITY, null)
                ?: targetUri?.let { runCatching { it.toUri().authority }.getOrNull() }
        val targetDisplayName = prefs.getString(KEY_TARGET_DISPLAY_NAME, null)
        val lastTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L).takeIf { it > 0L }
        val status =
            prefs.getString(KEY_LAST_STATUS, BackupStatus.Never.name)
                ?.let { runCatching { BackupStatus.valueOf(it) }.getOrNull() }
                ?: BackupStatus.Never
        val lastMessage = prefs.getString(KEY_LAST_MESSAGE, null)
        val consecutiveAutoFailures = prefs.getInt(KEY_AUTO_FAILURE_COUNT, 0).coerceAtLeast(0)
        val manifestPolicy =
            prefs.getString(KEY_MANIFEST_POLICY, RestoreManifestPolicy.Strict.name)
                ?.let { runCatching { RestoreManifestPolicy.valueOf(it) }.getOrNull() }
                ?: RestoreManifestPolicy.Strict
        val encryptionEnabled = prefs.getBoolean(KEY_ENCRYPTION_ENABLED, false)
        val encryptionPassphrase =
            prefs.getString(KEY_ENCRYPTION_PASSPHRASE, null)
                ?.takeIf { it.isNotBlank() }
        return BackupState(
            autoEnabled = enabled,
            targetType = targetType,
            targetUri = targetUri,
            targetAuthority = targetAuthority,
            targetDisplayName = targetDisplayName,
            targetHealth = resolveTargetHealth(targetType, targetUri),
            lastBackupTime = lastTime,
            lastStatus = status,
            lastMessage = lastMessage,
            consecutiveAutoFailures = consecutiveAutoFailures,
            manifestPolicy = manifestPolicy,
            encryptionEnabled = encryptionEnabled,
            encryptionConfigured = !encryptionPassphrase.isNullOrBlank()
        )
    }

    fun setAutoEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_ENABLED, enabled) }
    }

    fun setTargetType(type: BackupTargetType) {
        prefs.edit { putString(KEY_TARGET_TYPE, type.name) }
    }

    fun setTargetUri(uri: String?) {
        prefs.edit { putString(KEY_TARGET_URI, uri) }
    }

    fun setTargetSelection(uri: String, displayName: String?, authority: String?) {
        prefs.edit {
            putString(KEY_TARGET_TYPE, BackupTargetType.SafDirectory.name)
            putString(KEY_TARGET_URI, uri)
            putString(KEY_TARGET_DISPLAY_NAME, displayName)
            putString(KEY_TARGET_AUTHORITY, authority)
        }
    }

    fun setFileTargetSelection(uri: String, displayName: String?, authority: String?) {
        prefs.edit {
            putString(KEY_TARGET_TYPE, BackupTargetType.SafFile.name)
            putString(KEY_TARGET_URI, uri)
            putString(KEY_TARGET_DISPLAY_NAME, displayName)
            putString(KEY_TARGET_AUTHORITY, authority)
        }
    }

    fun clearSafTargetSelection() {
        prefs.edit {
            putString(KEY_TARGET_URI, null)
            putString(KEY_TARGET_DISPLAY_NAME, null)
            putString(KEY_TARGET_AUTHORITY, null)
        }
    }

    fun setLastResult(status: BackupStatus, message: String?, time: Long) {
        prefs.edit {
            putString(KEY_LAST_STATUS, status.name)
            putString(KEY_LAST_MESSAGE, message)
            putLong(KEY_LAST_BACKUP_TIME, time)
        }
    }

    fun incrementAutoFailure(): Int {
        val next = prefs.getInt(KEY_AUTO_FAILURE_COUNT, 0).coerceAtLeast(0) + 1
        prefs.edit { putInt(KEY_AUTO_FAILURE_COUNT, next) }
        return next
    }

    fun resetAutoFailureCount() {
        prefs.edit { putInt(KEY_AUTO_FAILURE_COUNT, 0) }
    }

    fun setManifestPolicy(policy: RestoreManifestPolicy) {
        prefs.edit { putString(KEY_MANIFEST_POLICY, policy.name) }
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENCRYPTION_ENABLED, enabled) }
    }

    fun setEncryptionPassphrase(passphrase: String?) {
        val normalized = passphrase?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit { putString(KEY_ENCRYPTION_PASSPHRASE, normalized) }
    }

    fun getEncryptionPassphrase(): String? {
        return prefs.getString(KEY_ENCRYPTION_PASSPHRASE, null)?.takeIf { it.isNotBlank() }
    }

    fun clearEncryptionConfiguration() {
        prefs.edit {
            putBoolean(KEY_ENCRYPTION_ENABLED, false)
            putString(KEY_ENCRYPTION_PASSPHRASE, null)
        }
    }

    private fun resolveTargetHealth(
        targetType: BackupTargetType,
        targetUri: String?
    ): BackupTargetHealth {
        return when (targetType) {
            BackupTargetType.AppPrivate -> BackupTargetHealth.Healthy
            BackupTargetType.SafDirectory ->
                if (targetUri.isNullOrBlank()) BackupTargetHealth.NeedsRelink
                else BackupTargetHealth.Unavailable
            BackupTargetType.SafFile ->
                if (targetUri.isNullOrBlank()) BackupTargetHealth.NeedsRelink
                else BackupTargetHealth.Unavailable
        }
    }

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_AUTO_ENABLED = "auto_enabled"
        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_TARGET_URI = "target_uri"
        private const val KEY_TARGET_DISPLAY_NAME = "target_display_name"
        private const val KEY_TARGET_AUTHORITY = "target_authority"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_LAST_STATUS = "last_status"
        private const val KEY_LAST_MESSAGE = "last_message"
        private const val KEY_AUTO_FAILURE_COUNT = "auto_failure_count"
        private const val KEY_MANIFEST_POLICY = "manifest_policy"
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
        private const val KEY_ENCRYPTION_PASSPHRASE = "encryption_passphrase"
    }
}
