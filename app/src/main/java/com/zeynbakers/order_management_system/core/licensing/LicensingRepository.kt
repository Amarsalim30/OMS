package com.zeynbakers.order_management_system.core.licensing

import android.os.Build
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.io.IOException
import kotlinx.coroutines.tasks.await

internal enum class LicensingBlockReason {
    EntitlementMissing,
    AccessDenied,
    DeviceLimitReached,
    DeviceRevoked,
    EntitlementExpired,
    OfflineGraceExpired,
    ValidationFailed
}

internal sealed interface LicensingValidationResult {
    data object Allowed : LicensingValidationResult

    data class Blocked(val reason: LicensingBlockReason) : LicensingValidationResult
}

internal class LicensingRepository(
    private val firestore: FirebaseFirestore,
    private val localStore: LicensingLocalStore,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun validateSignedInUser(uid: String): LicensingValidationResult {
        val nowMillis = nowMillisProvider()
        val installId = localStore.getOrCreateInstallId()
        val userRef = firestore.collection(COLLECTION_USERS).document(uid)
        return try {
            val entitlementSnapshot = userRef.get(Source.SERVER).await()
            if (!entitlementSnapshot.exists()) {
                return LicensingValidationResult.Blocked(LicensingBlockReason.EntitlementMissing)
            }

            val allowed = entitlementSnapshot.getBoolean(FIELD_ALLOWED) ?: false
            if (!allowed) {
                return LicensingValidationResult.Blocked(LicensingBlockReason.AccessDenied)
            }

            val maxDevices = (entitlementSnapshot.getLong(FIELD_MAX_DEVICES) ?: DEFAULT_MAX_DEVICES)
                .toInt()
                .coerceAtLeast(1)
            val expiresAt = entitlementSnapshot.getLong(FIELD_EXPIRES_AT) ?: 0L
            if (expiresAt > 0L && nowMillis > expiresAt) {
                return LicensingValidationResult.Blocked(LicensingBlockReason.EntitlementExpired)
            }

            val deviceRef = userRef.collection(COLLECTION_DEVICES).document(installId)
            val deviceSnapshot = deviceRef.get(Source.SERVER).await()
            if (deviceSnapshot.exists()) {
                val revoked = deviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
                if (revoked) {
                    return LicensingValidationResult.Blocked(LicensingBlockReason.DeviceRevoked)
                }
                touchDevice(deviceRef, nowMillis)
                localStore.updateLastValidated(uid, nowMillis)
                return LicensingValidationResult.Allowed
            }

            val activeDevicesCount = userRef.collection(COLLECTION_DEVICES)
                .whereEqualTo(FIELD_REVOKED, false)
                .get(Source.SERVER)
                .await()
                .size()
            if (activeDevicesCount >= maxDevices) {
                return LicensingValidationResult.Blocked(LicensingBlockReason.DeviceLimitReached)
            }

            registerDevice(deviceRef, nowMillis)
            localStore.updateLastValidated(uid, nowMillis)
            LicensingValidationResult.Allowed
        } catch (error: Exception) {
            when {
                shouldTreatAsOffline(error) ->
                    if (localStore.isWithinGraceWindow(uid, nowMillis, OFFLINE_GRACE_WINDOW_MS)) {
                        LicensingValidationResult.Allowed
                    } else {
                        LicensingValidationResult.Blocked(LicensingBlockReason.OfflineGraceExpired)
                    }

                else -> LicensingValidationResult.Blocked(LicensingBlockReason.ValidationFailed)
            }
        }
    }

    private suspend fun touchDevice(deviceRef: DocumentReference, nowMillis: Long) {
        val payload = mapOf(
            FIELD_LAST_SEEN_AT to nowMillis,
            FIELD_MODEL to Build.MODEL.orEmpty().ifBlank { "Unknown" },
            FIELD_SDK to Build.VERSION.SDK_INT
        )
        deviceRef.set(payload, SetOptions.merge()).await()
    }

    private suspend fun registerDevice(deviceRef: DocumentReference, nowMillis: Long) {
        val payload = mapOf(
            FIELD_REVOKED to false,
            FIELD_REGISTERED_AT to nowMillis,
            FIELD_LAST_SEEN_AT to nowMillis,
            FIELD_MODEL to Build.MODEL.orEmpty().ifBlank { "Unknown" },
            FIELD_SDK to Build.VERSION.SDK_INT
        )
        deviceRef.set(payload).await()
    }

    private fun shouldTreatAsOffline(error: Exception): Boolean {
        if (error is IOException) {
            return true
        }
        if (error !is FirebaseFirestoreException) {
            return false
        }
        return error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_DEVICES = "devices"

        const val FIELD_ALLOWED = "allowed"
        const val FIELD_MAX_DEVICES = "maxDevices"
        const val FIELD_EXPIRES_AT = "expiresAt"
        const val FIELD_REVOKED = "revoked"
        const val FIELD_REGISTERED_AT = "registeredAt"
        const val FIELD_LAST_SEEN_AT = "lastSeenAt"
        const val FIELD_MODEL = "model"
        const val FIELD_SDK = "sdk"

        const val DEFAULT_MAX_DEVICES = 1L
        const val OFFLINE_GRACE_WINDOW_MS = 3L * 24L * 60L * 60L * 1000L
    }
}
