package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class RestoreWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(BackupScheduler.KEY_RESTORE_URI)
        val result =
            BackupManager.runRestore(
                context = applicationContext,
                uriString = uriString
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
            else ->
                Result.failure(
                    workDataOf(
                        BackupScheduler.KEY_ERROR_MESSAGE to (result.message ?: "Restore failed")
                    )
                )
        }
    }
}
