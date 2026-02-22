package com.zeynbakers.order_management_system.core.helper

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object HelperOverlayController {
    const val ACTION_START = "com.zeynbakers.order_management_system.helper.START"
    const val ACTION_STOP = "com.zeynbakers.order_management_system.helper.STOP"
    const val ACTION_REFRESH = "com.zeynbakers.order_management_system.helper.REFRESH"
    const val ACTION_CAPTURE_NOTE = "com.zeynbakers.order_management_system.helper.CAPTURE_NOTE"
    const val ACTION_CAPTURE_CALCULATOR = "com.zeynbakers.order_management_system.helper.CAPTURE_CALCULATOR"
    const val ACTION_REVEAL = "com.zeynbakers.order_management_system.helper.REVEAL"

    fun start(context: Context) {
        val intent = Intent(context, HelperOverlayService::class.java).setAction(ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, HelperOverlayService::class.java).setAction(ACTION_STOP)
        runCatching { context.startService(intent) }
    }

    fun refresh(context: Context) {
        val intent = Intent(context, HelperOverlayService::class.java).setAction(ACTION_REFRESH)
        runCatching { context.startService(intent) }
    }

    fun reveal(context: Context) {
        val intent = Intent(context, HelperOverlayService::class.java).setAction(ACTION_REVEAL)
        runCatching { context.startService(intent) }
    }
}
