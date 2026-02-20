package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = BackupPreferences(applicationContext)
        val force = inputData.getBoolean(BackupScheduler.KEY_FORCE, false)
        val state = prefs.readState()
        if (!force && !state.autoEnabled) {
            return Result.success()
        }
        if (!force) {
            val health = BackupManager.evaluateTargetHealth(applicationContext, state)
            if (health != BackupTargetHealth.Healthy) {
                val count = prefs.incrementAutoFailure()
                if (count >= BACKUP_ATTENTION_THRESHOLD) {
                    BackupAttentionNotifier.notifyNeedsAttention(
                        context = applicationContext,
                        message = "Backup location needs re-link"
                    )
                }
                return Result.success()
            }
        }
        val result = BackupManager.runBackup(applicationContext, force = force)
        return if (result.success) {
            prefs.resetAutoFailureCount()
            BackupAttentionNotifier.cancel(applicationContext)
            Result.success()
        } else if (result.shouldRetry) {
            val count = prefs.incrementAutoFailure()
            if (count >= BACKUP_ATTENTION_THRESHOLD) {
                BackupAttentionNotifier.notifyNeedsAttention(applicationContext, result.message)
            }
            Result.retry()
        } else {
            val count = prefs.incrementAutoFailure()
            if (count >= BACKUP_ATTENTION_THRESHOLD) {
                BackupAttentionNotifier.notifyNeedsAttention(applicationContext, result.message)
            }
            Result.success()
        }
    }

    companion object {
        private const val BACKUP_ATTENTION_THRESHOLD = 3
    }
}
