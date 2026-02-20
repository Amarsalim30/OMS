package com.zeynbakers.order_management_system.core.backup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.navigation.PendingIntentFactory
import com.zeynbakers.order_management_system.core.notifications.NotificationChannels

object BackupAttentionNotifier {
    private const val BACKUP_ATTENTION_NOTIFICATION_ID = 42021

    fun notifyNeedsAttention(context: Context, message: String?) {
        NotificationChannels.ensureCreated(context)
        if (!canNotify(context)) return
        val openSettingsIntent =
            Intent(context, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHOW_BACKUP
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent = PendingIntentFactory.activity(context, 42022, openSettingsIntent)
        val body =
            if (message.isNullOrBlank()) {
                context.getString(R.string.backup_attention_body_default)
            } else {
                context.getString(R.string.backup_attention_body_with_reason, message)
            }

        val notification =
            NotificationCompat.Builder(context, NotificationChannels.BACKUP_ATTENTION_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.backup_attention_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(
                    0,
                    context.getString(R.string.backup_attention_action_open),
                    pendingIntent
                )
                .build()

        NotificationManagerCompat.from(context).notify(BACKUP_ATTENTION_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(BACKUP_ATTENTION_NOTIFICATION_ID)
    }

    private fun canNotify(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
