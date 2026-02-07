package com.zeynbakers.order_management_system.core.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object PendingIntentFactory {
    fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
