package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.core.content.edit

class BackupPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readState(): BackupState {
        val enabled = prefs.getBoolean(KEY_AUTO_ENABLED, false)
        val targetType =
            prefs.getString(KEY_TARGET_TYPE, BackupTargetType.AppPrivate.name)
                ?.let { runCatching { BackupTargetType.valueOf(it) }.getOrNull() }
                ?: BackupTargetType.AppPrivate
        val targetUri = prefs.getString(KEY_TARGET_URI, null)
        val lastTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L).takeIf { it > 0L }
        val status =
            prefs.getString(KEY_LAST_STATUS, BackupStatus.Never.name)
                ?.let { runCatching { BackupStatus.valueOf(it) }.getOrNull() }
                ?: BackupStatus.Never
        val lastMessage = prefs.getString(KEY_LAST_MESSAGE, null)
        return BackupState(
            autoEnabled = enabled,
            targetType = targetType,
            targetUri = targetUri,
            lastBackupTime = lastTime,
            lastStatus = status,
            lastMessage = lastMessage
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

    fun setLastResult(status: BackupStatus, message: String?, time: Long) {
        prefs.edit {
            putString(KEY_LAST_STATUS, status.name)
            putString(KEY_LAST_MESSAGE, message)
            putLong(KEY_LAST_BACKUP_TIME, time)
        }
    }

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_AUTO_ENABLED = "auto_enabled"
        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_TARGET_URI = "target_uri"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_LAST_STATUS = "last_status"
        private const val KEY_LAST_MESSAGE = "last_message"
    }
}
