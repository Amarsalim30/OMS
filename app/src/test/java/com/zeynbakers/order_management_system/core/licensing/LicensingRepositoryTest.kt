package com.zeynbakers.order_management_system.core.licensing

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensingRepositoryTest {

    @Test
    fun `blocks when entitlement is missing`() = runBlocking {
        val remote = FakeLicensingRemoteStore(entitlement = null)
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.EntitlementMissing)
        assertTrue(remote.touchCalls.isEmpty())
        assertTrue(remote.registerCalls.isEmpty())
        assertTrue(cache.validationUpdates.isEmpty())
    }

    @Test
    fun `blocks when entitlement is not allowed`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(allowed = false)
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.AccessDenied)
    }

    @Test
    fun `blocks when entitlement is expired`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(expiresAt = NOW_MILLIS - 1)
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.EntitlementExpired)
    }

    @Test
    fun `allows when current device is revoked but entitlement is allowed`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                device = LicensingDeviceRecord(revoked = true)
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertTrue(remote.touchCalls.isEmpty())
        assertTrue(remote.registerCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows existing active device and refreshes validation heartbeat`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                device = LicensingDeviceRecord(revoked = false)
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.touchCalls)
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows existing active device when heartbeat write fails`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                device = LicensingDeviceRecord(revoked = false),
                touchFailure = IllegalStateException("permission denied")
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.touchCalls)
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `registers new device when entitlement has capacity`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerResult = LicensingDeviceRegistrationResult.Registered
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `falls back to batched claim registration when transaction path is permission denied`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerFailure = LegacyDeviceRegistrationFallbackException(IllegalStateException("permission denied")),
                batchRegisterResult = LicensingDeviceRegistrationResult.Registered
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.batchRegisterCalls)
        assertTrue(remote.legacyRegisterCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `falls back to batched claim registration when compatibility wrapper is nested`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerFailure =
                    IllegalStateException(
                        "transaction failed",
                        LegacyDeviceRegistrationFallbackException(
                            IllegalStateException("permission denied")
                        )
                    ),
                batchRegisterResult = LicensingDeviceRegistrationResult.Registered
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.batchRegisterCalls)
        assertTrue(remote.legacyRegisterCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `falls back to legacy registration when batched claim path is also denied`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerFailure = LegacyDeviceRegistrationFallbackException(IllegalStateException("permission denied")),
                batchRegisterFailure = LegacyDeviceRegistrationFallbackException(IllegalStateException("permission denied")),
                legacyRegisterResult = LicensingDeviceRegistrationResult.Registered
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.batchRegisterCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.legacyRegisterCalls)
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows when batched claim registration has no capacity but entitlement is allowed`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 1),
                device = null,
                registerFailure = LegacyDeviceRegistrationFallbackException(IllegalStateException("permission denied")),
                batchRegisterResult = LicensingDeviceRegistrationResult.DeviceLimitReached
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.batchRegisterCalls)
        assertTrue(remote.legacyRegisterCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows when max devices has been reached but entitlement is allowed`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 1),
                device = null,
                registerResult = LicensingDeviceRegistrationResult.DeviceLimitReached
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows when registration fails for non compatibility reason after entitlement validation`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerFailure = IllegalStateException("unexpected")
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertTrue(remote.batchRegisterCalls.isEmpty())
        assertTrue(remote.legacyRegisterCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows when batched claim fallback fails for non compatibility reason after entitlement validation`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 2),
                device = null,
                registerFailure = LegacyDeviceRegistrationFallbackException(IllegalStateException("permission denied")),
                batchRegisterFailure = IllegalStateException("unexpected batch failure")
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.batchRegisterCalls)
        assertTrue(remote.legacyRegisterCalls.isEmpty())
        assertEquals(listOf(ValidationUpdate(USER_ID, NOW_MILLIS)), cache.validationUpdates)
    }

    @Test
    fun `allows offline within grace when firestore is unavailable`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                failure = IOException("offline")
            )
        val cache = FakeLicensingCacheStore(withinGraceWindow = true)
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertEquals(LicensingValidationResult.Allowed, result)
    }

    @Test
    fun `blocks offline access when grace window has elapsed`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                failure = IOException("network down")
            )
        val cache = FakeLicensingCacheStore(withinGraceWindow = false)
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.OfflineGraceExpired)
    }

    @Test
    fun `blocks fatal validation failures`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                failure = IllegalStateException("denied")
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.ValidationFailed)
    }

    private fun assertBlocked(
        result: LicensingValidationResult,
        expectedReason: LicensingBlockReason
    ) {
        assertTrue(result is LicensingValidationResult.Blocked)
        result as LicensingValidationResult.Blocked
        assertEquals(expectedReason, result.reason)
    }

    private fun allowedEntitlement(
        allowed: Boolean = true,
        maxDevices: Int = 1,
        expiresAt: Long = 0L,
        registeredDeviceIds: Set<String> = emptySet()
    ): LicensingEntitlement {
        return LicensingEntitlement(
            allowed = allowed,
            maxDevices = maxDevices,
            expiresAt = expiresAt,
            registeredDeviceIds = registeredDeviceIds
        )
    }

    private class FakeLicensingRemoteStore(
        private val entitlement: LicensingEntitlement?,
        private val device: LicensingDeviceRecord? = null,
        private val registerResult: LicensingDeviceRegistrationResult = LicensingDeviceRegistrationResult.Registered,
        private val batchRegisterResult: LicensingDeviceRegistrationResult = LicensingDeviceRegistrationResult.Registered,
        private val legacyRegisterResult: LicensingDeviceRegistrationResult = LicensingDeviceRegistrationResult.Registered,
        private val failure: Exception? = null,
        private val touchFailure: Exception? = null,
        private val registerFailure: Exception? = null,
        private val batchRegisterFailure: Exception? = null,
        private val legacyRegisterFailure: Exception? = null
    ) : LicensingRemoteStore {
        val touchCalls = mutableListOf<DeviceWrite>()
        val registerCalls = mutableListOf<DeviceWrite>()
        val batchRegisterCalls = mutableListOf<DeviceWrite>()
        val legacyRegisterCalls = mutableListOf<DeviceWrite>()

        override suspend fun getEntitlement(uid: String): LicensingEntitlement? {
            maybeThrow()
            return entitlement
        }

        override suspend fun getDevice(uid: String, deviceId: String): LicensingDeviceRecord? {
            maybeThrow()
            return device
        }

        override suspend fun touchDevice(uid: String, deviceId: String, nowMillis: Long) {
            maybeThrow()
            touchCalls += DeviceWrite(uid, deviceId, nowMillis)
            maybeThrow(touchFailure)
        }

        override suspend fun registerDevice(
            uid: String,
            deviceId: String,
            nowMillis: Long
        ): LicensingDeviceRegistrationResult {
            registerCalls += DeviceWrite(uid, deviceId, nowMillis)
            maybeThrow(registerFailure)
            return registerResult
        }

        override suspend fun registerDeviceClaimBatch(
            uid: String,
            deviceId: String,
            nowMillis: Long,
            entitlement: LicensingEntitlement
        ): LicensingDeviceRegistrationResult {
            batchRegisterCalls += DeviceWrite(uid, deviceId, nowMillis)
            maybeThrow(batchRegisterFailure)
            return batchRegisterResult
        }

        override suspend fun registerDeviceLegacy(
            uid: String,
            deviceId: String,
            nowMillis: Long,
            maxDevices: Int
        ): LicensingDeviceRegistrationResult {
            legacyRegisterCalls += DeviceWrite(uid, deviceId, nowMillis)
            maybeThrow(legacyRegisterFailure)
            return legacyRegisterResult
        }

        private fun maybeThrow(specificFailure: Exception? = null) {
            specificFailure?.let { throw it }
            failure?.let { throw it }
        }
    }

    private class FakeLicensingCacheStore(
        private val withinGraceWindow: Boolean = false
    ) : LicensingCacheStore {
        val validationUpdates = mutableListOf<ValidationUpdate>()

        override fun getOrCreateInstallId(): String = INSTALL_ID

        override fun updateLastValidated(uid: String, validatedAtMillis: Long) {
            validationUpdates += ValidationUpdate(uid, validatedAtMillis)
        }

        override fun isWithinGraceWindow(uid: String, nowMillis: Long, graceWindowMillis: Long): Boolean {
            return withinGraceWindow
        }
    }

    private data class DeviceWrite(
        val uid: String,
        val deviceId: String,
        val nowMillis: Long
    )

    private data class ValidationUpdate(
        val uid: String,
        val validatedAtMillis: Long
    )

    private companion object {
        const val USER_ID = "user-123"
        const val INSTALL_ID = "install-abc"
        const val NOW_MILLIS = 1_710_000_000_000L
    }
}
