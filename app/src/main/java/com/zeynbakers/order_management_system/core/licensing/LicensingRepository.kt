package com.zeynbakers.order_management_system.core.licensing

import android.os.Build
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.io.IOException
import kotlinx.coroutines.tasks.await

private const val COLLECTION_USERS = "users"
private const val COLLECTION_DEVICES = "devices"

private const val FIELD_ALLOWED = "allowed"
private const val FIELD_MAX_DEVICES = "maxDevices"
private const val FIELD_EXPIRES_AT = "expiresAt"
private const val FIELD_REVOKED = "revoked"
private const val FIELD_REGISTERED_AT = "registeredAt"
private const val FIELD_LAST_SEEN_AT = "lastSeenAt"
private const val FIELD_MODEL = "model"
private const val FIELD_SDK = "sdk"
private const val FIELD_REGISTERED_DEVICE_IDS = "registeredDeviceIds"

private const val DEFAULT_MAX_DEVICES = 1L
private const val OFFLINE_GRACE_WINDOW_MS = 3L * 24L * 60L * 60L * 1000L

private fun com.google.firebase.firestore.DocumentSnapshot.getRegisteredDeviceIds(): Set<String> {
    val values = get(FIELD_REGISTERED_DEVICE_IDS) as? List<*> ?: emptyList<Any>()
    return values.mapNotNull { it as? String }
        .filter { it.isNotBlank() }
        .toSet()
}

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

internal data class LicensingEntitlement(
    val allowed: Boolean,
    val maxDevices: Int,
    val expiresAt: Long,
    val registeredDeviceIds: Set<String>
)

internal data class LicensingDeviceRecord(
    val revoked: Boolean
)

internal enum class LicensingDeviceRegistrationResult {
    Registered,
    AlreadyRegistered,
    DeviceLimitReached,
    DeviceRevoked,
    EntitlementMissing,
    AccessDenied,
    EntitlementExpired
}

internal interface LicensingRemoteStore {
    suspend fun getEntitlement(uid: String): LicensingEntitlement?
    suspend fun getDevice(uid: String, deviceId: String): LicensingDeviceRecord?
    suspend fun touchDevice(uid: String, deviceId: String, nowMillis: Long)
    suspend fun registerDevice(uid: String, deviceId: String, nowMillis: Long): LicensingDeviceRegistrationResult
}

internal class FirestoreLicensingRemoteStore(
    private val firestore: FirebaseFirestore
) : LicensingRemoteStore {
    override suspend fun getEntitlement(uid: String): LicensingEntitlement? {
        val snapshot =
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .get(Source.SERVER)
                .await()
        if (!snapshot.exists()) {
            return null
        }
        return LicensingEntitlement(
            allowed = snapshot.getBoolean(FIELD_ALLOWED) ?: false,
            maxDevices = (snapshot.getLong(FIELD_MAX_DEVICES) ?: DEFAULT_MAX_DEVICES)
                .toInt()
                .coerceAtLeast(1),
            expiresAt = snapshot.getLong(FIELD_EXPIRES_AT) ?: 0L,
            registeredDeviceIds = snapshot.getRegisteredDeviceIds()
        )
    }

    override suspend fun getDevice(uid: String, deviceId: String): LicensingDeviceRecord? {
        val snapshot = deviceRef(uid, deviceId).get(Source.SERVER).await()
        if (!snapshot.exists()) {
            return null
        }
        return LicensingDeviceRecord(
            revoked = snapshot.getBoolean(FIELD_REVOKED) ?: false
        )
    }

    override suspend fun touchDevice(uid: String, deviceId: String, nowMillis: Long) {
        deviceRef(uid, deviceId).set(deviceHeartbeatPayload(nowMillis), SetOptions.merge()).await()
    }

    override suspend fun registerDevice(
        uid: String,
        deviceId: String,
        nowMillis: Long
    ): LicensingDeviceRegistrationResult {
        val userRef = firestore.collection(COLLECTION_USERS).document(uid)
        val deviceRef = deviceRef(uid, deviceId)
        return firestore.runTransaction { transaction ->
            val entitlementSnapshot = transaction.get(userRef)
            if (!entitlementSnapshot.exists()) {
                return@runTransaction LicensingDeviceRegistrationResult.EntitlementMissing
            }

            val allowed = entitlementSnapshot.getBoolean(FIELD_ALLOWED) ?: false
            if (!allowed) {
                return@runTransaction LicensingDeviceRegistrationResult.AccessDenied
            }

            val maxDevices =
                (entitlementSnapshot.getLong(FIELD_MAX_DEVICES) ?: DEFAULT_MAX_DEVICES)
                    .toInt()
                    .coerceAtLeast(1)
            val expiresAt = entitlementSnapshot.getLong(FIELD_EXPIRES_AT) ?: 0L
            if (expiresAt > 0L && nowMillis > expiresAt) {
                return@runTransaction LicensingDeviceRegistrationResult.EntitlementExpired
            }

            val registeredDeviceIds = entitlementSnapshot.getRegisteredDeviceIds()
            val deviceSnapshot = transaction.get(deviceRef)
            if (deviceSnapshot.exists()) {
                val revoked = deviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
                if (revoked) {
                    return@runTransaction LicensingDeviceRegistrationResult.DeviceRevoked
                }
                transaction.set(deviceRef, deviceHeartbeatPayload(nowMillis), SetOptions.merge())
                return@runTransaction LicensingDeviceRegistrationResult.AlreadyRegistered
            }

            if (!registeredDeviceIds.contains(deviceId) && registeredDeviceIds.size >= maxDevices) {
                return@runTransaction LicensingDeviceRegistrationResult.DeviceLimitReached
            }

            if (!registeredDeviceIds.contains(deviceId)) {
                val updatedDeviceIds =
                    registeredDeviceIds.toMutableSet()
                        .apply { add(deviceId) }
                        .toList()
                transaction.update(userRef, FIELD_REGISTERED_DEVICE_IDS, updatedDeviceIds)
            }
            transaction.set(deviceRef, deviceRegistrationPayload(nowMillis))
            LicensingDeviceRegistrationResult.Registered
        }.await()
    }

    private fun deviceRef(uid: String, deviceId: String): DocumentReference {
        return firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_DEVICES)
            .document(deviceId)
    }

    private fun deviceHeartbeatPayload(nowMillis: Long): Map<String, Any> {
        return mapOf(
            FIELD_LAST_SEEN_AT to nowMillis,
            FIELD_MODEL to Build.MODEL.orEmpty().ifBlank { "Unknown" },
            FIELD_SDK to Build.VERSION.SDK_INT
        )
    }

    private fun deviceRegistrationPayload(nowMillis: Long): Map<String, Any> {
        return mapOf(
            FIELD_REVOKED to false,
            FIELD_REGISTERED_AT to nowMillis,
            FIELD_LAST_SEEN_AT to nowMillis,
            FIELD_MODEL to Build.MODEL.orEmpty().ifBlank { "Unknown" },
            FIELD_SDK to Build.VERSION.SDK_INT
        )
    }
}

internal class LicensingRepository(
    private val remoteStore: LicensingRemoteStore,
    private val localStore: LicensingCacheStore,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {
    constructor(
        firestore: FirebaseFirestore,
        localStore: LicensingLocalStore,
        nowMillisProvider: () -> Long = { System.currentTimeMillis() }
    ) : this(
        remoteStore = FirestoreLicensingRemoteStore(firestore),
        localStore = localStore,
        nowMillisProvider = nowMillisProvider
    )

    suspend fun validateSignedInUser(uid: String): LicensingValidationResult {
        logInfo("Validation start")

        fun blocked(reason: LicensingBlockReason): LicensingValidationResult.Blocked {
            logWarn("Validation blocked: $reason")
            return LicensingValidationResult.Blocked(reason)
        }

        fun allowed(path: String): LicensingValidationResult.Allowed {
            logInfo("Validation success: $path")
            return LicensingValidationResult.Allowed
        }

        val nowMillis = nowMillisProvider()
        val installId = localStore.getOrCreateInstallId()
        return try {
            val entitlement = remoteStore.getEntitlement(uid)
            if (entitlement == null) {
                return blocked(LicensingBlockReason.EntitlementMissing)
            }

            if (!entitlement.allowed) {
                return blocked(LicensingBlockReason.AccessDenied)
            }

            if (entitlement.expiresAt > 0L && nowMillis > entitlement.expiresAt) {
                return blocked(LicensingBlockReason.EntitlementExpired)
            }

            val device = remoteStore.getDevice(uid, installId)
            if (device != null) {
                if (device.revoked) {
                    return blocked(LicensingBlockReason.DeviceRevoked)
                }
                remoteStore.touchDevice(uid, installId, nowMillis)
                localStore.updateLastValidated(uid, nowMillis)
                return allowed("existing_device")
            }

            val registrationResult = remoteStore.registerDevice(uid, installId, nowMillis)
            return when (registrationResult) {
                LicensingDeviceRegistrationResult.Registered -> {
                    localStore.updateLastValidated(uid, nowMillis)
                    allowed("registered_device")
                }

                LicensingDeviceRegistrationResult.AlreadyRegistered -> {
                    localStore.updateLastValidated(uid, nowMillis)
                    allowed("registered_device_retry")
                }

                LicensingDeviceRegistrationResult.DeviceLimitReached -> {
                    blocked(LicensingBlockReason.DeviceLimitReached)
                }

                LicensingDeviceRegistrationResult.DeviceRevoked -> {
                    blocked(LicensingBlockReason.DeviceRevoked)
                }

                LicensingDeviceRegistrationResult.EntitlementMissing -> {
                    blocked(LicensingBlockReason.EntitlementMissing)
                }

                LicensingDeviceRegistrationResult.AccessDenied -> {
                    blocked(LicensingBlockReason.AccessDenied)
                }

                LicensingDeviceRegistrationResult.EntitlementExpired -> {
                    blocked(LicensingBlockReason.EntitlementExpired)
                }
            }
        } catch (error: Exception) {
            when {
                shouldTreatAsOffline(error) -> {
                    logWarn("Validation retryable failure: offline_or_timeout", error)
                    if (localStore.isWithinGraceWindow(uid, nowMillis, OFFLINE_GRACE_WINDOW_MS)) {
                        allowed("offline_grace")
                    } else {
                        blocked(LicensingBlockReason.OfflineGraceExpired)
                    }
                }

                else -> {
                    logError("Validation fatal failure", error)
                    blocked(LicensingBlockReason.ValidationFailed)
                }
            }
        }
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

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarn(message: String, error: Throwable? = null) {
        runCatching {
            if (error == null) {
                Log.w(TAG, message)
            } else {
                Log.w(TAG, message, error)
            }
        }
    }

    private fun logError(message: String, error: Throwable) {
        runCatching { Log.e(TAG, message, error) }
    }

    private companion object {
        const val TAG = "LicensingRepository"
    }
}
