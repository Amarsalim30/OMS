package com.zeynbakers.order_management_system.core.updates

import android.content.Context

class UpdatePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowUpdate(versionName: String): Boolean {
        val lastSeen = prefs.getString(KEY_LAST_VERSION, null)
        return lastSeen == null || lastSeen != versionName
    }

    fun markVersionSeen(versionName: String) {
        prefs.edit().putString(KEY_LAST_VERSION, versionName).apply()
    }

    companion object {
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_VERSION = "last_version_seen"
    }
}
