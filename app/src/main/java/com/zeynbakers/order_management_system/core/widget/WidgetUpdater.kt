package com.zeynbakers.order_management_system.core.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.navigation.PendingIntentFactory
import com.zeynbakers.order_management_system.core.util.formatKes
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { updateAll(appContext) }
        }
    }

    suspend fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, TodayUnpaidWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
        if (appWidgetIds.isEmpty()) return

        val database = DatabaseProvider.getDatabase(appContext)
        val orderDao = database.orderDao()
        val accountingDao = database.accountingDao()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayOrders =
            orderDao.getOrdersByDate(today.toString())
                .filter { it.status != OrderStatus.CANCELLED }
                .sortedByDescending { it.createdAt }
        val allOrders =
            orderDao.getActiveOrders().filter { it.statusOverride != OrderStatusOverride.CLOSED }

        val allOrderIds = allOrders.map { it.id }.filter { it != 0L }
        val paidByOrder =
            if (allOrderIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getPaidForOrders(allOrderIds).associate { it.orderId to it.paid }
            }
        val unpaidCount =
            allOrders.count { order ->
                val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                paid < order.totalAmount
            }

        val views = RemoteViews(appContext.packageName, R.layout.widget_today_unpaid)
        views.setTextViewText(R.id.widget_today_count, "Today: ${todayOrders.size}")
        views.setTextViewText(R.id.widget_unpaid_count, "Unpaid: $unpaidCount")

        val lines = todayOrders.take(3).map { order ->
            val label = order.notes.take(28)
            "$label - ${formatKes(order.totalAmount)}"
        }
        bindLine(views, R.id.widget_line1, lines.getOrNull(0), showIfEmpty = true)
        bindLine(views, R.id.widget_line2, lines.getOrNull(1), showIfEmpty = false)
        bindLine(views, R.id.widget_line3, lines.getOrNull(2), showIfEmpty = false)
        if (lines.isEmpty()) {
            views.setTextViewText(R.id.widget_line1, "No orders today")
        }

        val todayIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHOW_TODAY
            }
        val unpaidIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHOW_UNPAID
            }
        val addIntent =
            Intent(appContext, MainActivity::class.java).apply {
                action = AppIntents.ACTION_NEW_ORDER
            }

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntentFactory.activity(appContext, 2001, todayIntent)
        )
        views.setOnClickPendingIntent(
            R.id.widget_action_unpaid,
            PendingIntentFactory.activity(appContext, 2002, unpaidIntent)
        )
        views.setOnClickPendingIntent(
            R.id.widget_action_add,
            PendingIntentFactory.activity(appContext, 2003, addIntent)
        )

        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun bindLine(views: RemoteViews, viewId: Int, text: String?, showIfEmpty: Boolean) {
        if (text == null) {
            views.setViewVisibility(viewId, if (showIfEmpty) android.view.View.VISIBLE else android.view.View.GONE)
        } else {
            views.setViewVisibility(viewId, android.view.View.VISIBLE)
            views.setTextViewText(viewId, text)
        }
    }
}
