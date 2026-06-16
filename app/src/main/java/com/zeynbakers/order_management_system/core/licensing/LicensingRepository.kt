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

internal class LegacyDeviceRegistrationFallbackException(cause: Throwable) : Exception(cause)

private fun com.google.firebase.firestore.DocumentSnapshot.getRegisteredDeviceIds(): Set<String> {
    val values = get(FIELD_REGISTERED_DEVICE_IDS) as? List<*> ?: emptyList<Any>()
    return values.mapNotNull { it as? String }
        .filter { it.isNotBlank() }
        .toSet()
}

internal fun registeredDeviceClaimPayload(
    registeredDeviceIds: Set<String>
): Map<String, Any> {
    return linkedMapOf(
        FIELD_REGISTERED_DEVICE_IDS to registeredDeviceIds.toList()
    )
}

internal fun exceedsActiveDeviceLimit(
    activeDeviceIds: Set<String>,
    currentDeviceId: String,
    maxDevices: Int
): Boolean {
    return !activeDeviceIds.contains(currentDeviceId) && activeDeviceIds.size >= maxDevices
}

internal fun reconcileRegisteredDeviceIds(
    existingRegisteredDeviceIds: Set<String>,
    activeRegisteredDeviceIds: Set<String>,
    currentDeviceId: String
): Set<String> {
    val reconciled = LinkedHashSet<String>()
    existingRegisteredDeviceIds.forEach { deviceId ->
        if (activeRegisteredDeviceIds.contains(deviceId) && deviceId != currentDeviceId) {
            reconciled += deviceId
        }
    }
    activeRegisteredDeviceIds.forEach { deviceId ->
        if (deviceId != currentDeviceId) {
            reconciled += deviceId
        }
    }
    reconciled += currentDeviceId
    return reconciled
}

internal fun isCompatibilityRegistrationFailure(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        when (current) {
            is LegacyDeviceRegistrationFallbackException -> return true
            is FirebaseFirestoreException -> {
                return current.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    current.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
            }
        }
        current = current.cause
    }
    return false
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
    suspend fun registerDeviceClaimBatch(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        entitlement: LicensingEntitlement
    ): LicensingDeviceRegistrationResult
    suspend fun registerDeviceLegacy(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        maxDevices: Int
    ): LicensingDeviceRegistrationResult
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
        return try {
            firestore.runTransaction { transaction ->
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
                val activeRegisteredDeviceIds = LinkedHashSet<String>()
                registeredDeviceIds.forEach { registeredDeviceId ->
                    val registeredDeviceSnapshot =
                        if (registeredDeviceId == deviceId) {
                            deviceSnapshot
                        } else {
                            transaction.get(deviceRef(uid, registeredDeviceId))
                        }
                    if (!registeredDeviceSnapshot.exists()) {
                        return@forEach
                    }
                    val revoked = registeredDeviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
                    if (!revoked) {
                        activeRegisteredDeviceIds += registeredDeviceId
                    }
                }
                if (deviceSnapshot.exists()) {
                    val revoked = deviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
                    if (revoked) {
                        return@runTransaction LicensingDeviceRegistrationResult.DeviceRevoked
                    }
                    val reconciledDeviceIds =
                        reconcileRegisteredDeviceIds(
                            existingRegisteredDeviceIds = registeredDeviceIds,
                            activeRegisteredDeviceIds = activeRegisteredDeviceIds,
                            currentDeviceId = deviceId
                        )
                    if (reconciledDeviceIds != registeredDeviceIds) {
                        // Use update instead of merge-set so the rules engine evaluates this as a
                        // concrete document update alongside the device-doc write in the same request.
                        transaction.update(
                            userRef,
                            registeredDeviceClaimPayload(reconciledDeviceIds)
                        )
                    }
                    transaction.set(deviceRef, deviceHeartbeatPayload(nowMillis), SetOptions.merge())
                    return@runTransaction LicensingDeviceRegistrationResult.AlreadyRegistered
                }

                if (exceedsActiveDeviceLimit(activeRegisteredDeviceIds, deviceId, maxDevices)) {
                    return@runTransaction LicensingDeviceRegistrationResult.DeviceLimitReached
                }

                val reconciledDeviceIds =
                    reconcileRegisteredDeviceIds(
                        existingRegisteredDeviceIds = registeredDeviceIds,
                        activeRegisteredDeviceIds = activeRegisteredDeviceIds,
                        currentDeviceId = deviceId
                    )
                if (reconciledDeviceIds != registeredDeviceIds) {
                    transaction.update(
                        userRef,
                        registeredDeviceClaimPayload(reconciledDeviceIds)
                    )
                }
                transaction.set(deviceRef, deviceRegistrationPayload(nowMillis))
                LicensingDeviceRegistrationResult.Registered
            }.await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
            ) {
                throw LegacyDeviceRegistrationFallbackException(error)
            }
            throw error
        }
    }

    override suspend fun registerDeviceClaimBatch(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        entitlement: LicensingEntitlement
    ): LicensingDeviceRegistrationResult {
        val userRef = firestore.collection(COLLECTION_USERS).document(uid)
        val deviceRef = deviceRef(uid, deviceId)
        val deviceSnapshot = deviceRef.get(Source.SERVER).await()
        if (deviceSnapshot.exists()) {
            val revoked = deviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
            if (revoked) {
                return LicensingDeviceRegistrationResult.DeviceRevoked
            }
            val activeRegisteredDeviceIds = getActiveDeviceIds(uid)
            val reconciledDeviceIds =
                reconcileRegisteredDeviceIds(
                    existingRegisteredDeviceIds = entitlement.registeredDeviceIds,
                    activeRegisteredDeviceIds = activeRegisteredDeviceIds,
                    currentDeviceId = deviceId
                )
            val batch = firestore.batch()
            if (reconciledDeviceIds != entitlement.registeredDeviceIds) {
                batch.update(
                    userRef,
                    registeredDeviceClaimPayload(reconciledDeviceIds)
                )
            }
            batch.set(deviceRef, deviceHeartbeatPayload(nowMillis), SetOptions.merge())
            batch.commit().await()
            return LicensingDeviceRegistrationResult.AlreadyRegistered
        }

        val activeRegisteredDeviceIds = getActiveDeviceIds(uid)
        if (exceedsActiveDeviceLimit(activeRegisteredDeviceIds, deviceId, entitlement.maxDevices)) {
            return LicensingDeviceRegistrationResult.DeviceLimitReached
        }

        val reconciledDeviceIds =
            reconcileRegisteredDeviceIds(
                existingRegisteredDeviceIds = entitlement.registeredDeviceIds,
                activeRegisteredDeviceIds = activeRegisteredDeviceIds,
                currentDeviceId = deviceId
            )
        val batch = firestore.batch()
        if (reconciledDeviceIds != entitlement.registeredDeviceIds) {
            batch.update(
                userRef,
                registeredDeviceClaimPayload(reconciledDeviceIds)
            )
        }
        batch.set(deviceRef, deviceRegistrationPayload(nowMillis))
        batch.commit().await()
        return LicensingDeviceRegistrationResult.Registered
    }

    override suspend fun registerDeviceLegacy(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        maxDevices: Int
    ): LicensingDeviceRegistrationResult {
        val deviceRef = deviceRef(uid, deviceId)
        val deviceSnapshot = deviceRef.get(Source.SERVER).await()
        if (deviceSnapshot.exists()) {
            val revoked = deviceSnapshot.getBoolean(FIELD_REVOKED) ?: false
            if (revoked) {
                return LicensingDeviceRegistrationResult.DeviceRevoked
            }
            deviceRef.set(deviceHeartbeatPayload(nowMillis), SetOptions.merge()).await()
            return LicensingDeviceRegistrationResult.AlreadyRegistered
        }

        val activeDevicesCount =
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_DEVICES)
                .whereEqualTo(FIELD_REVOKED, false)
                .get(Source.SERVER)
                .await()
                .size()
        if (activeDevicesCount >= maxDevices) {
            return LicensingDeviceRegistrationResult.DeviceLimitReached
        }

        deviceRef.set(deviceRegistrationPayload(nowMillis)).await()
        return LicensingDeviceRegistrationResult.Registered
    }

    private fun deviceRef(uid: String, deviceId: String): DocumentReference {
        return firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_DEVICES)
            .document(deviceId)
    }

    private suspend fun getActiveDeviceIds(uid: String): Set<String> {
        return firestore.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_DEVICES)
            .whereEqualTo(FIELD_REVOKED, false)
            .get(Source.SERVER)
            .await()
            .documents
            .map { it.id }
            .toSet()
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

            val accessPath =
                validateDeviceAccessBestEffort(
                    uid = uid,
                    deviceId = installId,
                    nowMillis = nowMillis,
                    entitlement = entitlement
                )
            localStore.updateLastValidated(uid, nowMillis)
            return allowed(accessPath)
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

    private suspend fun validateDeviceAccessBestEffort(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        entitlement: LicensingEntitlement
    ): String {
        return try {
            val device = remoteStore.getDevice(uid, deviceId)
            if (device != null) {
                if (device.revoked) {
                    logWarn("Current device is revoked, but entitlement is allowed; bypassing device block")
                    return "allowed_revoked_device_ignored"
                }
                try {
                    remoteStore.touchDevice(uid, deviceId, nowMillis)
                } catch (error: Exception) {
                    logWarn("Heartbeat refresh failed for validated device; allowing access", error)
                }
                return "existing_device"
            }

            when (
                val registrationResult =
                    registerDeviceWithCompatibilityFallback(
                        uid = uid,
                        deviceId = deviceId,
                        nowMillis = nowMillis,
                        entitlement = entitlement
                    )
            ) {
                LicensingDeviceRegistrationResult.Registered -> "registered_device"
                LicensingDeviceRegistrationResult.AlreadyRegistered -> "registered_device_retry"
                LicensingDeviceRegistrationResult.DeviceLimitReached -> {
                    logWarn("Device limit reached, but entitlement is allowed; bypassing device block")
                    "allowed_device_limit_ignored"
                }

                LicensingDeviceRegistrationResult.DeviceRevoked -> {
                    logWarn("Device registration reported revoked, but entitlement is allowed; bypassing device block")
                    "allowed_device_revoked_ignored"
                }

                LicensingDeviceRegistrationResult.EntitlementMissing,
                LicensingDeviceRegistrationResult.AccessDenied,
                LicensingDeviceRegistrationResult.EntitlementExpired -> {
                    logWarn(
                        "Device registration returned $registrationResult after entitlement validation; allowing access"
                    )
                    "allowed_device_registration_ignored"
                }
            }
        } catch (error: Exception) {
            logWarn("Device validation failed after entitlement was confirmed; allowing access", error)
            "allowed_device_validation_ignored"
        }
    }

    private suspend fun registerDeviceWithCompatibilityFallback(
        uid: String,
        deviceId: String,
        nowMillis: Long,
        entitlement: LicensingEntitlement
    ): LicensingDeviceRegistrationResult {
        return try {
            remoteStore.registerDevice(uid, deviceId, nowMillis)
        } catch (error: Exception) {
            if (!shouldRetryCompatibilityRegistration(error)) {
                throw error
            }
            logWarn("Device registration transaction denied; retrying batched claim path", error)
            try {
                remoteStore.registerDeviceClaimBatch(
                    uid = uid,
                    deviceId = deviceId,
                    nowMillis = nowMillis,
                    entitlement = entitlement
                )
            } catch (batchError: Exception) {
                if (!shouldRetryCompatibilityRegistration(batchError)) {
                    throw batchError
                }
                logWarn("Device registration batch denied; retrying legacy compatibility path", batchError)
                remoteStore.registerDeviceLegacy(
                    uid = uid,
                    deviceId = deviceId,
                    nowMillis = nowMillis,
                    maxDevices = entitlement.maxDevices
                )
            }
        }
    }

    private fun shouldRetryCompatibilityRegistration(error: Throwable): Boolean {
        return isCompatibilityRegistrationFailure(error)
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
