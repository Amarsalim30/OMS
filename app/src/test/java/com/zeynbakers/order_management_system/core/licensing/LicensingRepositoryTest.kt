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
    fun `blocks when current device is revoked`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(),
                device = LicensingDeviceRecord(revoked = true)
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.DeviceRevoked)
        assertTrue(remote.touchCalls.isEmpty())
        assertTrue(remote.registerCalls.isEmpty())
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
    fun `blocks when max devices has been reached`() = runBlocking {
        val remote =
            FakeLicensingRemoteStore(
                entitlement = allowedEntitlement(maxDevices = 1),
                device = null,
                registerResult = LicensingDeviceRegistrationResult.DeviceLimitReached
            )
        val cache = FakeLicensingCacheStore()
        val repository = LicensingRepository(remote, cache) { NOW_MILLIS }

        val result = repository.validateSignedInUser(USER_ID)

        assertBlocked(result, LicensingBlockReason.DeviceLimitReached)
        assertEquals(listOf(DeviceWrite(USER_ID, INSTALL_ID, NOW_MILLIS)), remote.registerCalls)
        assertTrue(cache.validationUpdates.isEmpty())
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
        private val failure: Exception? = null
    ) : LicensingRemoteStore {
        val touchCalls = mutableListOf<DeviceWrite>()
        val registerCalls = mutableListOf<DeviceWrite>()

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
        }

        override suspend fun registerDevice(
            uid: String,
            deviceId: String,
            nowMillis: Long
        ): LicensingDeviceRegistrationResult {
            maybeThrow()
            registerCalls += DeviceWrite(uid, deviceId, nowMillis)
            return registerResult
        }

        private fun maybeThrow() {
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
