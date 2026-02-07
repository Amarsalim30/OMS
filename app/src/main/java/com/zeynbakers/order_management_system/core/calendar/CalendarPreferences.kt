package com.zeynbakers.order_management_system.core.calendar

import android.content.Context
import androidx.core.content.edit
import java.util.Calendar

class CalendarPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readWeekStart(): Int? {
        val stored = prefs.getInt(KEY_WEEK_START, 0)
        return if (stored == 0) null else stored
    }

    fun setWeekStart(value: Int?) {
        prefs.edit {
            if (value == null) {
                remove(KEY_WEEK_START)
            } else {
                putInt(KEY_WEEK_START, value)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "calendar_settings"
        private const val KEY_WEEK_START = "week_start"
        val SUPPORTED_WEEK_STARTS = setOf(Calendar.SUNDAY, Calendar.MONDAY)
    }
}
