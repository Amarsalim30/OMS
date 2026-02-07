package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class ManualBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(BackupScheduler.KEY_FORCE, false)
        val result =
            BackupManager.runBackup(
                context = applicationContext,
                force = force
            ) { progress, stage ->
                setProgressAsync(
                    workDataOf(
                        BackupScheduler.KEY_PROGRESS to progress,
                        BackupScheduler.KEY_STAGE to stage
                    )
                )
            }
        return when {
            result.success -> Result.success()
            result.shouldRetry -> Result.retry()
            else -> Result.failure()
        }
    }
}
