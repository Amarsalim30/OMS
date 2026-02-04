package com.zeynbakers.order_management_system.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val UNIQUE_REMINDER_WORK = "notification_reminders"
    private const val UNIQUE_REMINDER_NOW = "notification_reminders_now"

    fun ensureScheduled(context: Context) {
        val settings = NotificationPreferences(context).readSettings()
        if (settings.enabled) {
            schedulePeriodic(context)
            enqueueNow(context)
        } else {
            cancel(context)
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        NotificationPreferences(context).setEnabled(enabled)
        ensureScheduled(context)
        if (enabled) {
            enqueueNow(context)
        }
    }

    fun enqueueNow(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(UNIQUE_REMINDER_NOW)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_REMINDER_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    private fun schedulePeriodic(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .addTag(UNIQUE_REMINDER_WORK)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_REMINDER_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_REMINDER_NOW)
    }

}
