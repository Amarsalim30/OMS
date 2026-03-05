package com.zeynbakers.order_management_system.core.licensing

import android.content.Context
import java.util.UUID

internal class LicensingLocalStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getOrCreateInstallId(): String {
        val existing = prefs.getString(KEY_INSTALL_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_ID, generated).apply()
        return generated
    }

    fun updateLastValidated(uid: String, validatedAtMillis: Long) {
        prefs.edit()
            .putString(KEY_LAST_VALIDATED_UID, uid)
            .putLong(KEY_LAST_VALIDATED_AT, validatedAtMillis)
            .apply()
    }

    fun isWithinGraceWindow(uid: String, nowMillis: Long, graceWindowMillis: Long): Boolean {
        val cachedUid = prefs.getString(KEY_LAST_VALIDATED_UID, null)
        if (cachedUid != uid) {
            return false
        }
        val lastValidatedAt = prefs.getLong(KEY_LAST_VALIDATED_AT, 0L)
        return isWithinOfflineGraceWindow(
            nowMillis = nowMillis,
            lastValidatedAtMillis = lastValidatedAt,
            graceWindowMillis = graceWindowMillis
        )
    }

    private companion object {
        const val PREFS_NAME = "licensing_local_store"
        const val KEY_INSTALL_ID = "install_id"
        const val KEY_LAST_VALIDATED_UID = "last_validated_uid"
        const val KEY_LAST_VALIDATED_AT = "last_validated_at"
    }
}

internal fun isWithinOfflineGraceWindow(
    nowMillis: Long,
    lastValidatedAtMillis: Long,
    graceWindowMillis: Long
): Boolean {
    if (lastValidatedAtMillis <= 0L || graceWindowMillis < 0L) {
        return false
    }
    val elapsedMillis = nowMillis - lastValidatedAtMillis
    return elapsedMillis in 0..graceWindowMillis
}
