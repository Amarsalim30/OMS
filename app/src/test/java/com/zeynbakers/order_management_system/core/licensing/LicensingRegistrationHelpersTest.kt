package com.zeynbakers.order_management_system.core.licensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensingRegistrationHelpersTest {

    @Test
    fun `exceedsActiveDeviceLimit ignores stale claimed device ids`() {
        val activeDeviceIds = linkedSetOf("active-device")

        assertFalse(
            exceedsActiveDeviceLimit(
                activeDeviceIds = activeDeviceIds,
                currentDeviceId = "new-device",
                maxDevices = 2
            )
        )
    }

    @Test
    fun `exceedsActiveDeviceLimit blocks when active devices already fill capacity`() {
        val activeDeviceIds = linkedSetOf("device-a")

        assertTrue(
            exceedsActiveDeviceLimit(
                activeDeviceIds = activeDeviceIds,
                currentDeviceId = "device-b",
                maxDevices = 1
            )
        )
    }

    @Test
    fun `reconcileRegisteredDeviceIds prunes inactive ids and keeps active order`() {
        val reconciled =
            reconcileRegisteredDeviceIds(
                existingRegisteredDeviceIds = linkedSetOf("stale-a", "active-b", "revoked-c"),
                activeRegisteredDeviceIds = linkedSetOf("active-b"),
                currentDeviceId = "current-device"
            )

        assertEquals(
            linkedSetOf("active-b", "current-device"),
            reconciled
        )
    }
}
