package com.zeynbakers.order_management_system.core.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayUnpaidWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ALLOWED_WIDGET_ACTIONS) {
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val registeredIds =
            AppWidgetManager.getInstance(appContext)
                .getAppWidgetIds(ComponentName(appContext, TodayUnpaidWidgetProvider::class.java))
                .toSet()
        if (appWidgetIds.none { it in registeredIds }) {
            return
        }
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

    companion object {
        private val ALLOWED_WIDGET_ACTIONS =
            setOf(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                AppWidgetManager.ACTION_APPWIDGET_ENABLED,
                AppWidgetManager.ACTION_APPWIDGET_DISABLED,
                AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
                AppWidgetManager.ACTION_APPWIDGET_DELETED,
                AppWidgetManager.ACTION_APPWIDGET_RESTORED
            )
    }
}
