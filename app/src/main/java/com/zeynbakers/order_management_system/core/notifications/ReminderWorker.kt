package com.zeynbakers.order_management_system.core.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.navigation.PendingIntentFactory
import com.zeynbakers.order_management_system.core.navigation.AppIntents
import com.zeynbakers.order_management_system.core.util.parsePickupTime
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = NotificationPreferences(appContext)
        val settings = prefs.readSettings()
        if (!settings.enabled) return Result.success()
        if (!canPostNotifications(appContext)) return Result.success()

        NotificationChannels.ensureCreated(appContext)

        val database = DatabaseProvider.getDatabase(appContext)
        val orderDao = database.orderDao()
        val accountingDao = database.accountingDao()
        val customerDao = database.customerDao()

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = now.toLocalDateTime(timeZone).date
        prefs.pruneReminderHistory(nowDate.plus(-7, DateTimeUnit.DAY))

        val leadTimeMillis = settings.leadTimeMinutes * 60_000L
        val windowStart = now.toEpochMilliseconds()
        val windowEnd = windowStart + leadTimeMillis

        val dateRangeStart = nowDate
        val dateRangeEnd = nowDate.plus(2, DateTimeUnit.DAY)
        val candidateOrders =
            orderDao.getOrdersBetween(dateRangeStart.toString(), dateRangeEnd.toString())
                .filter { it.status != OrderStatus.CANCELLED }

        val orderIds = candidateOrders.map { it.id }.filter { it != 0L }
        val paidByOrder =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
            }

        val dueOrders =
            candidateOrders.mapNotNull { order ->
                if (order.statusOverride == OrderStatusOverride.CLOSED) return@mapNotNull null
                val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                if (paid >= order.totalAmount) return@mapNotNull null
                val dueAtMillis = resolveDueAtMillis(order, timeZone)
                if (dueAtMillis < windowStart || dueAtMillis > windowEnd) return@mapNotNull null
                if (prefs.wasOrderReminded(order.id, order.orderDate, settings.leadTimeMinutes)) return@mapNotNull null
                order to dueAtMillis
            }
                .sortedBy { it.second }

        if (dueOrders.isNotEmpty()) {
            val customerIds = dueOrders.mapNotNull { it.first.customerId }.distinct()
            val customerNames =
                if (customerIds.isEmpty()) emptyMap()
                else customerDao.getByIds(customerIds).associate { it.id to it.name }

            sendDueReminder(
                orders = dueOrders,
                customerNames = customerNames,
                leadTimeMinutes = settings.leadTimeMinutes,
                timeZone = timeZone
            )
            dueOrders.forEach { (order, _) ->
                prefs.markOrderReminded(order.id, order.orderDate, settings.leadTimeMinutes)
            }
        }

        if (settings.dailySummaryEnabled && shouldSendSummary(now, timeZone, prefs)) {
            val todayOrders = orderDao.getOrdersByDate(nowDate.toString())
                .filter { it.status != OrderStatus.CANCELLED }
            val todayOrderIds = todayOrders.map { it.id }.filter { it != 0L }
            val paidToday =
                if (todayOrderIds.isEmpty()) emptyMap()
                else accountingDao.getPaidForOrders(todayOrderIds).associate { it.orderId to it.paid }
            val unpaidCount =
                todayOrders.count { order ->
                    val paid = paidToday[order.id] ?: BigDecimal.ZERO
                    paid < order.totalAmount
                }
            sendDailySummary(
                date = nowDate,
                totalOrders = todayOrders.size,
                unpaidCount = unpaidCount
            )
            prefs.markSummarySent(nowDate)
        }

        return Result.success()
    }

    private fun sendDueReminder(
        orders: List<Pair<OrderEntity, Long>>,
        customerNames: Map<Long, String>,
        leadTimeMinutes: Int,
        timeZone: TimeZone
    ) {
        val count = orders.size
        val title =
            if (leadTimeMinutes >= NotificationPreferences.LEAD_TIME_1_DAY) {
                appContext.getString(R.string.reminder_due_title_day)
            } else {
                appContext.getString(R.string.reminder_due_title_hour)
            }
        val lines =
            orders.take(5).map { (order, dueAtMillis) ->
                val name = order.customerId?.let { customerNames[it] }?.takeIf { it.isNotBlank() }
                val label = name ?: order.notes.take(40)
                val dueTime = formatDueTime(dueAtMillis, timeZone)
                appContext.getString(R.string.reminder_due_line_item, label, dueTime)
            }
        val detailText =
            if (count == 1) {
                lines.firstOrNull() ?: appContext.getString(R.string.reminder_due_single_fallback)
            } else {
                appContext.getString(R.string.reminder_due_multiple_fallback, count)
            }
        val bigText = lines.joinToString("\n")
        val targetDate = orders.first().first.orderDate

        val intent =
            Intent(appContext, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHOW_DAY
                putExtra(AppIntents.EXTRA_TARGET_DATE, targetDate.toString())
            }
        val pending = PendingIntentFactory.activity(appContext, 1001, intent)

        val notification =
            NotificationCompat.Builder(appContext, NotificationChannels.DUE_REMINDER_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(detailText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(DUE_REMINDER_ID, notification) }
    }

    private fun sendDailySummary(date: LocalDate, totalOrders: Int, unpaidCount: Int) {
        val title = appContext.getString(R.string.reminder_daily_title)
        val text = appContext.getString(R.string.reminder_daily_text, totalOrders, unpaidCount)
        val intent =
            Intent(appContext, MainActivity::class.java).apply {
                action = AppIntents.ACTION_SHOW_SUMMARY
                putExtra(AppIntents.EXTRA_TARGET_DATE, date.toString())
            }
        val pending = PendingIntentFactory.activity(appContext, 1002, intent)
        val notification =
            NotificationCompat.Builder(appContext, NotificationChannels.DAILY_SUMMARY_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(DAILY_SUMMARY_ID, notification) }
    }

    private fun resolveDueAtMillis(order: OrderEntity, timeZone: TimeZone): Long {
        val dayStart = order.orderDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val pickup = parsePickupTime(order.pickupTime)
        val hour = pickup?.hour ?: DEFAULT_DUE_HOUR
        val minute = pickup?.minute ?: DEFAULT_DUE_MINUTE
        val offsetMillis = ((hour * 60L) + minute) * 60_000L
        return dayStart + offsetMillis
    }

    private fun formatDueTime(epochMillis: Long, timeZone: TimeZone): String {
        val time = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).time
        val hour = time.hour.toString().padStart(2, '0')
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    private fun shouldSendSummary(
        now: kotlinx.datetime.Instant,
        timeZone: TimeZone,
        prefs: NotificationPreferences
    ): Boolean {
        val nowTime = now.toLocalDateTime(timeZone)
        if (nowTime.hour < SUMMARY_START_HOUR) return false
        return !prefs.wasSummarySentFor(nowTime.date)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < 33) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val SUMMARY_START_HOUR = 7
        private const val DEFAULT_DUE_HOUR = 9
        private const val DEFAULT_DUE_MINUTE = 0
        private const val DUE_REMINDER_ID = 3201
        private const val DAILY_SUMMARY_ID = 3202
    }
}
