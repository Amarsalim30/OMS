package com.zeynbakers.order_management_system.core.licensing

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

internal interface LicensingCacheStore {
    fun getOrCreateInstallId(): String
    fun updateLastValidated(uid: String, validatedAtMillis: Long)
    fun isWithinGraceWindow(uid: String, nowMillis: Long, graceWindowMillis: Long): Boolean
}

internal class LicensingLocalStore(context: Context) : LicensingCacheStore {
    private val appContext = context.applicationContext
    private val legacyPrefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val securePrefs: SharedPreferences? by lazy { createSecurePreferences(appContext) }
    private val prefs: SharedPreferences by lazy { securePrefs ?: legacyPrefs }

    init {
        migrateLegacyValuesIfNeeded()
    }

    override
    fun getOrCreateInstallId(): String {
        val existing =
            resolveStoredInstallId(
                secureInstallId = readSecureString(KEY_INSTALL_ID),
                legacyInstallId = legacyPrefs.getString(KEY_INSTALL_ID, null)
            )
        if (!existing.isNullOrBlank()) {
            persistInstallId(existing)
            return existing
        }
        val generated = UUID.randomUUID().toString()
        persistInstallId(generated)
        return generated
    }

    override
    fun updateLastValidated(uid: String, validatedAtMillis: Long) {
        prefs.edit {
            putString(KEY_LAST_VALIDATED_UID, uid)
            putLong(KEY_LAST_VALIDATED_AT, validatedAtMillis)
        }
    }

    override
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

    private fun migrateLegacyValuesIfNeeded() {
        val secure = securePrefs ?: return
        val legacyInstallId = legacyPrefs.getString(KEY_INSTALL_ID, null)
        val legacyUid = legacyPrefs.getString(KEY_LAST_VALIDATED_UID, null)
        val legacyValidatedAt = legacyPrefs.getLong(KEY_LAST_VALIDATED_AT, 0L)
        if (legacyInstallId.isNullOrBlank() && legacyUid.isNullOrBlank() && legacyValidatedAt <= 0L) {
            return
        }
        secure.edit(commit = true) {
            legacyInstallId?.takeIf { it.isNotBlank() }?.let { putString(KEY_INSTALL_ID, it) }
            legacyUid?.takeIf { it.isNotBlank() }?.let { putString(KEY_LAST_VALIDATED_UID, it) }
            if (legacyValidatedAt > 0L) {
                putLong(KEY_LAST_VALIDATED_AT, legacyValidatedAt)
            }
        }
        legacyPrefs.edit(commit = true) {
            remove(KEY_LAST_VALIDATED_UID)
            remove(KEY_LAST_VALIDATED_AT)
        }
    }

    private fun persistInstallId(installId: String) {
        runCatching {
            securePrefs?.edit(commit = true) { putString(KEY_INSTALL_ID, installId) }
        }
        // Keep a backup copy so device binding survives secure-store failures on later launches.
        legacyPrefs.edit(commit = true) { putString(KEY_INSTALL_ID, installId) }
    }

    private fun readSecureString(key: String): String? {
        return runCatching { securePrefs?.getString(key, null) }.getOrNull()
    }

    private fun createSecurePreferences(context: Context): SharedPreferences? {
        return runCatching {
            val masterKey =
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private companion object {
        const val PREFS_NAME = "licensing_local_store"
        const val SECURE_PREFS_NAME = "licensing_local_store_secure"
        const val KEY_INSTALL_ID = "install_id"
        const val KEY_LAST_VALIDATED_UID = "last_validated_uid"
        const val KEY_LAST_VALIDATED_AT = "last_validated_at"
    }
}

internal fun resolveStoredInstallId(
    secureInstallId: String?,
    legacyInstallId: String?
): String? {
    return secureInstallId?.takeIf { it.isNotBlank() }
        ?: legacyInstallId?.takeIf { it.isNotBlank() }
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
