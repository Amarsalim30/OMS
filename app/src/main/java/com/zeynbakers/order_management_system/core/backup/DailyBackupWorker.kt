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
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!force && !prefs.readState().autoEnabled) {
            return Result.success()
        }
        val result = BackupManager.runBackup(applicationContext, force = force)
        return if (result.success) {
            Result.success()
        } else if (result.shouldRetry) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
