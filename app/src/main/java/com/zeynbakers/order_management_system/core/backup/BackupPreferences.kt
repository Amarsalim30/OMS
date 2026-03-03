package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class BackupPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences? by lazy { createSecurePreferences(appContext) }

    init {
        migrateLegacyPassphraseIfNeeded()
    }

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
        val encryptionEnabled = prefs.getBoolean(KEY_ENCRYPTION_ENABLED, true)
        val insecureOverrideEnabled = prefs.getBoolean(KEY_ALLOW_INSECURE_BACKUPS, false)
        val encryptionPassphrase =
            getEncryptionPassphrase()
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
            encryptionConfigured = !encryptionPassphrase.isNullOrBlank(),
            insecureOverrideEnabled = insecureOverrideEnabled
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
        prefs.edit {
            putBoolean(KEY_ENCRYPTION_ENABLED, enabled)
            if (enabled) {
                putBoolean(KEY_ALLOW_INSECURE_BACKUPS, false)
            }
        }
    }

    fun setInsecureOverrideEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ALLOW_INSECURE_BACKUPS, enabled) }
    }

    fun setEncryptionPassphrase(passphrase: String?) {
        val normalized = passphrase?.trim()?.takeIf { it.isNotEmpty() }
        securePrefs?.edit {
            if (normalized == null) {
                remove(SECURE_KEY_ENCRYPTION_PASSPHRASE)
            } else {
                putString(SECURE_KEY_ENCRYPTION_PASSPHRASE, normalized)
            }
        }
        prefs.edit { remove(KEY_ENCRYPTION_PASSPHRASE) }
    }

    fun getEncryptionPassphrase(): String? {
        migrateLegacyPassphraseIfNeeded()
        return securePrefs
            ?.getString(SECURE_KEY_ENCRYPTION_PASSPHRASE, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun clearEncryptionConfiguration() {
        securePrefs?.edit {
            remove(SECURE_KEY_ENCRYPTION_PASSPHRASE)
        }
        prefs.edit {
            putBoolean(KEY_ENCRYPTION_ENABLED, false)
            putBoolean(KEY_ALLOW_INSECURE_BACKUPS, false)
            remove(KEY_ENCRYPTION_PASSPHRASE)
        }
    }

    private fun resolveTargetHealth(
        targetType: BackupTargetType,
        targetUri: String?
    ): BackupTargetHealth {
        return when (targetType) {
            BackupTargetType.AppPrivate -> BackupTargetHealth.Healthy
            BackupTargetType.SafDirectory,
            BackupTargetType.SafFile -> resolveSafTargetHealth(targetType, targetUri)
        }
    }

    private fun resolveSafTargetHealth(
        targetType: BackupTargetType,
        targetUri: String?
    ): BackupTargetHealth {
        val uriString = targetUri?.trim().orEmpty()
        if (uriString.isEmpty()) return BackupTargetHealth.NeedsRelink
        if (!hasPersistedReadWritePermission(uriString)) return BackupTargetHealth.NeedsRelink
        return if (isSafTargetAccessible(targetType, uriString)) {
            BackupTargetHealth.Healthy
        } else {
            BackupTargetHealth.Unavailable
        }
    }

    private fun hasPersistedReadWritePermission(uriString: String): Boolean {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return false
        return appContext.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    private fun isSafTargetAccessible(targetType: BackupTargetType, uriString: String): Boolean {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return false
        return runCatching {
            when (targetType) {
                BackupTargetType.SafDirectory -> {
                    val tree = DocumentFile.fromTreeUri(appContext, uri) ?: return@runCatching false
                    tree.exists() && tree.isDirectory && tree.canRead() && tree.canWrite()
                }
                BackupTargetType.SafFile -> {
                    val file = DocumentFile.fromSingleUri(appContext, uri) ?: return@runCatching false
                    file.exists() && file.isFile && file.canRead()
                }
                BackupTargetType.AppPrivate -> true
            }
        }.getOrDefault(false)
    }

    private fun createSecurePreferences(context: Context): SharedPreferences? {
        return runCatching {
            val masterKey =
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private fun migrateLegacyPassphraseIfNeeded() {
        val legacy =
            prefs.getString(KEY_ENCRYPTION_PASSPHRASE, null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        if (legacy != null) {
            securePrefs?.edit {
                putString(SECURE_KEY_ENCRYPTION_PASSPHRASE, legacy)
            }
            prefs.edit { remove(KEY_ENCRYPTION_PASSPHRASE) }
        }
    }

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val SECURE_PREFS_NAME = "backup_secure_prefs"
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
        private const val KEY_ALLOW_INSECURE_BACKUPS = "allow_insecure_backups"
        private const val SECURE_KEY_ENCRYPTION_PASSPHRASE = "encryption_passphrase_secure"
        @Deprecated("Legacy plaintext key retained only for migration cleanup")
        private const val KEY_ENCRYPTION_PASSPHRASE = "encryption_passphrase"
    }
}
