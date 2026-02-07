package com.zeynbakers.order_management_system.core.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayUnpaidWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { WidgetUpdater.updateAll(context) }
            pendingResult.finish()
        }
    }

    override fun onEnabled(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { WidgetUpdater.updateAll(context) }
            pendingResult.finish()
        }
    }
}
