package com.zeynbakers.order_management_system.core.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object PendingIntentFactory {
    fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent {
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
