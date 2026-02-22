package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    const val DAILY_INTERVAL_HOURS = 24L
    const val UNIQUE_DAILY_WORK = "daily_backup"
    const val UNIQUE_MANUAL_WORK = "manual_backup"
    const val UNIQUE_RESTORE_WORK = "restore_backup"
    const val KEY_PROGRESS = "progress"
    const val KEY_STAGE = "stage"
    const val KEY_FORCE = "force_backup"
    const val KEY_RESTORE_URI = "restore_uri"
    const val KEY_ERROR_MESSAGE = "error_message"

    fun ensureScheduled(context: Context) {
        val prefs = BackupPreferences(context)
        val state = prefs.readState()
        if (state.autoEnabled) {
            val health = BackupManager.evaluateTargetHealth(context, state)
            if (health == BackupTargetHealth.Healthy) {
                scheduleDaily(context)
            } else {
                cancelDaily(context)
            }
        } else {
            cancelDaily(context)
        }
    }

    fun setAutomaticEnabled(context: Context, enabled: Boolean) {
        BackupPreferences(context).setAutoEnabled(enabled)
        ensureScheduled(context)
    }

    fun enqueueManualBackup(context: Context) {
        val data = Data.Builder().putBoolean(KEY_FORCE, true).build()
        val request =
            OneTimeWorkRequestBuilder<ManualBackupWorker>()
                .setInputData(data)
                .addTag(UNIQUE_MANUAL_WORK)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_MANUAL_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueRestore(context: Context, uriString: String?) {
        val data =
            Data.Builder()
                .putString(KEY_RESTORE_URI, uriString)
                .build()
        val request =
            OneTimeWorkRequestBuilder<RestoreWorker>()
                .setInputData(data)
                .addTag(UNIQUE_RESTORE_WORK)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_RESTORE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun scheduleDaily(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<DailyBackupWorker>(DAILY_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(dailyConstraints())
                .addTag(UNIQUE_DAILY_WORK)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelDaily(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY_WORK)
    }

    private fun dailyConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
}
