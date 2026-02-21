package com.zeynbakers.order_management_system.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.zeynbakers.order_management_system.R

object NotificationChannels {
    const val DUE_REMINDER_CHANNEL = "due_reminder"
    const val DAILY_SUMMARY_CHANNEL = "daily_summary"
    const val BACKUP_ATTENTION_CHANNEL = "backup_attention"
    const val HELPER_CHANNEL = "floating_helper"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dueChannel = NotificationChannel(
            DUE_REMINDER_CHANNEL,
            context.getString(R.string.notification_channel_due),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_due_desc)
        }

        val summaryChannel = NotificationChannel(
            DAILY_SUMMARY_CHANNEL,
            context.getString(R.string.notification_channel_summary),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_summary_desc)
        }

        val backupAttentionChannel = NotificationChannel(
            BACKUP_ATTENTION_CHANNEL,
            context.getString(R.string.notification_channel_backup_attention),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_backup_attention_desc)
        }

        val helperChannel = NotificationChannel(
            HELPER_CHANNEL,
            context.getString(R.string.notification_channel_helper),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_helper_desc)
        }

        manager.createNotificationChannel(dueChannel)
        manager.createNotificationChannel(summaryChannel)
        manager.createNotificationChannel(backupAttentionChannel)
        manager.createNotificationChannel(helperChannel)
    }
}
