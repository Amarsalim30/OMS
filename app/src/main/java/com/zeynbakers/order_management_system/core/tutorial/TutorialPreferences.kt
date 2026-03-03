package com.zeynbakers.order_management_system.core.tutorial

import android.content.Context
import androidx.core.content.edit

class TutorialPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldAutoShowOnFirstLaunch(): Boolean {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_TUTORIAL_SHOWN, false)
    }

    fun markAutoShown() {
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH_TUTORIAL_SHOWN, true) }
    }

    companion object {
        private const val PREFS_NAME = "tutorial_prefs"
        private const val KEY_FIRST_LAUNCH_TUTORIAL_SHOWN = "first_launch_tutorial_shown"
    }
}

