package com.zeynbakers.order_management_system.core.licensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensingLocalStoreTest {

    @Test
    fun `resolveStoredInstallId prefers secure value`() {
        assertEquals(
            "secure-id",
            resolveStoredInstallId(
                secureInstallId = "secure-id",
                legacyInstallId = "legacy-id"
            )
        )
    }

    @Test
    fun `resolveStoredInstallId falls back to legacy value`() {
        assertEquals(
            "legacy-id",
            resolveStoredInstallId(
                secureInstallId = null,
                legacyInstallId = "legacy-id"
            )
        )
    }

    @Test
    fun `resolveStoredInstallId ignores blank values`() {
        assertNull(
            resolveStoredInstallId(
                secureInstallId = "  ",
                legacyInstallId = ""
            )
        )
    }

    @Test
    fun `isWithinOfflineGraceWindow returns true within grace window`() {
        assertTrue(
            isWithinOfflineGraceWindow(
                nowMillis = 1_700_000_000_000,
                lastValidatedAtMillis = 1_699_999_000_000,
                graceWindowMillis = 1_500_000
            )
        )
    }

    @Test
    fun `isWithinOfflineGraceWindow returns false when grace window elapsed`() {
        assertFalse(
            isWithinOfflineGraceWindow(
                nowMillis = 1_700_000_000_000,
                lastValidatedAtMillis = 1_699_000_000_000,
                graceWindowMillis = 100_000
            )
        )
    }

    @Test
    fun `isWithinOfflineGraceWindow returns false when device time moves backwards`() {
        assertFalse(
            isWithinOfflineGraceWindow(
                nowMillis = 1_699_000_000_000,
                lastValidatedAtMillis = 1_700_000_000_000,
                graceWindowMillis = 3 * 24 * 60 * 60 * 1000L
            )
        )
    }
}
