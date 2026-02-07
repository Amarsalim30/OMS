package com.zeynbakers.order_management_system.core.notifications

import android.content.Context
import androidx.core.content.edit
import kotlinx.datetime.LocalDate

data class NotificationSettings(
    val enabled: Boolean,
    val leadTimeMinutes: Int,
    val dailySummaryEnabled: Boolean
)

class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readSettings(): NotificationSettings {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val leadTime = prefs.getInt(KEY_LEAD_TIME_MINUTES, DEFAULT_LEAD_TIME_MINUTES)
        val dailySummary = prefs.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true)
        return NotificationSettings(enabled, leadTime, dailySummary)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun setLeadTimeMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_LEAD_TIME_MINUTES, minutes) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY_SUMMARY_ENABLED, enabled) }
    }

    fun wasSummarySentFor(date: LocalDate): Boolean {
        return prefs.getString(KEY_LAST_SUMMARY_DATE, null) == date.toString()
    }

    fun markSummarySent(date: LocalDate) {
        prefs.edit { putString(KEY_LAST_SUMMARY_DATE, date.toString()) }
    }

    fun wasOrderReminded(orderId: Long, dueDate: LocalDate, leadTimeMinutes: Int): Boolean {
        val key = buildReminderKey(orderId, dueDate, leadTimeMinutes)
        return prefs.getStringSet(KEY_DUE_REMINDER_HISTORY, emptySet())?.contains(key) == true
    }

    fun markOrderReminded(orderId: Long, dueDate: LocalDate, leadTimeMinutes: Int) {
        val key = buildReminderKey(orderId, dueDate, leadTimeMinutes)
        val updated = (prefs.getStringSet(KEY_DUE_REMINDER_HISTORY, emptySet()) ?: emptySet()).toMutableSet()
        updated.add(key)
        prefs.edit { putStringSet(KEY_DUE_REMINDER_HISTORY, updated) }
    }

    fun pruneReminderHistory(cutoffDate: LocalDate) {
        val entries = prefs.getStringSet(KEY_DUE_REMINDER_HISTORY, emptySet()) ?: emptySet()
        if (entries.isEmpty()) return
        val filtered =
            entries.filter { entry ->
                val parts = entry.split('|')
                if (parts.size != 3) return@filter false
                val date = runCatching { LocalDate.parse(parts[1]) }.getOrNull() ?: return@filter false
                date >= cutoffDate
            }.toSet()
        prefs.edit { putStringSet(KEY_DUE_REMINDER_HISTORY, filtered) }
    }

    private fun buildReminderKey(orderId: Long, dueDate: LocalDate, leadTimeMinutes: Int): String {
        return "$orderId|${dueDate}|$leadTimeMinutes"
    }

    companion object {
        private const val PREFS_NAME = "notification_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LEAD_TIME_MINUTES = "lead_time_minutes"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_LAST_SUMMARY_DATE = "last_summary_date"
        private const val KEY_DUE_REMINDER_HISTORY = "due_reminder_history"

        const val LEAD_TIME_1_HOUR = 60
        const val LEAD_TIME_2_HOURS = 2 * 60
        const val LEAD_TIME_12_HOURS = 12 * 60
        const val LEAD_TIME_1_DAY = 24 * 60
        const val LEAD_TIME_30_MIN = 30
        const val LEAD_TIME_15_MIN = 15
        const val DEFAULT_LEAD_TIME_MINUTES = LEAD_TIME_1_DAY
    }
}
