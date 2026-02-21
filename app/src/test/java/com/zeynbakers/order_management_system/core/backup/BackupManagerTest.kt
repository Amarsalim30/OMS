package com.zeynbakers.order_management_system.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun `parseBackupDecimal handles common decimal formats`() {
        assertDecimal("1200", "1200")
        assertDecimal("1,200", "1200")
        assertDecimal("1200,50", "1200.50")
        assertDecimal("1.200,50", "1200.50")
        assertDecimal("1,200.50", "1200.50")
    }

    @Test
    fun `parseBackupDecimal returns null for invalid inputs`() {
        assertNull(BackupManager.parseBackupDecimal(null))
        assertNull(BackupManager.parseBackupDecimal(""))
        assertNull(BackupManager.parseBackupDecimal("   "))
        assertNull(BackupManager.parseBackupDecimal("NaN"))
        assertNull(BackupManager.parseBackupDecimal("1,2,3"))
    }

    @Test
    fun `isRetryableBackupException classifies io as retryable`() {
        assertTrue(BackupManager.isRetryableBackupException(IOException("disk busy")))
        assertFalse(BackupManager.isRetryableBackupException(SecurityException("denied")))
        assertFalse(BackupManager.isRetryableBackupException(IllegalArgumentException("bad payload")))
        assertFalse(BackupManager.isRetryableBackupException(IllegalStateException("bad state")))
        assertFalse(BackupManager.isRetryableBackupException(RuntimeException("unknown")))
    }

    @Test
    fun `zip fixture parses orders without legacy amountPaid`() {
        val ordersPayload =
            """
            [
              {
                "id": 1,
                "orderDate": "2026-02-06",
                "createdAt": 100,
                "updatedAt": 120,
                "notes": "Bread",
                "pickupTime": null,
                "status": "PENDING",
                "statusOverride": null,
                "totalAmount": "1,200.50",
                "customerId": 7
              }
            ]
            """.trimIndent()
        val zipBytes = buildZip(mapOf("orders.json" to ordersPayload))
        val payloads = BackupManager.readBackupPayloadsForTest(ByteArrayInputStream(zipBytes))
        val parsedTotal =
            BackupManager.parseBackupDecimal(extractJsonStringValue(payloads.getValue("orders.json"), "totalAmount"))
        val parsedLegacyAmountPaid =
            BackupManager.parseBackupDecimal(extractJsonStringValue(payloads.getValue("orders.json"), "amountPaid"))

        assertNotNull(parsedTotal)
        assertEquals(0, parsedTotal!!.compareTo(BigDecimal("1200.50")))
        assertNull(parsedLegacyAmountPaid)
    }

    @Test
    fun `zip fixture parses legacy amountPaid when present`() {
        val ordersPayload =
            """
            [
              {
                "id": 2,
                "orderDate": "2026-02-06",
                "createdAt": 100,
                "updatedAt": 120,
                "notes": "Cake",
                "pickupTime": null,
                "status": "PENDING",
                "statusOverride": null,
                "totalAmount": "3000",
                "amountPaid": "1.200,50",
                "customerId": 8
              }
            ]
            """.trimIndent()
        val zipBytes = buildZip(mapOf("orders.json" to ordersPayload))
        val payloads = BackupManager.readBackupPayloadsForTest(ByteArrayInputStream(zipBytes))
        val parsedLegacyAmountPaid =
            BackupManager.parseBackupDecimal(extractJsonStringValue(payloads.getValue("orders.json"), "amountPaid"))
        assertNotNull(parsedLegacyAmountPaid)
        assertEquals(0, parsedLegacyAmountPaid!!.compareTo(BigDecimal("1200.50")))
    }

    @Test
    fun `sha256 helper is stable for known payload`() {
        val digest = BackupManager.sha256HexForTest("orders-payload")
        assertEquals(
            "68799d4b0af65bcb3df4f38871cc90eb629b5ca8985623691aab5a26017ea895",
            digest
        )
    }

    @Test
    fun `zip reader rejects oversized single entry`() {
        val zipBytes = buildZip(mapOf("orders.json" to "x".repeat(128)))
        val error =
            runCatching {
                BackupManager.readBackupPayloadsForTest(
                    inputStream = ByteArrayInputStream(zipBytes),
                    maxEntryBytes = 64,
                    maxTotalBytes = 1_024
                )
            }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `zip reader rejects oversized total payload`() {
        val zipBytes =
            buildZip(
                mapOf(
                    "customers.json" to "a".repeat(80),
                    "orders.json" to "b".repeat(80)
                )
            )
        val error =
            runCatching {
                BackupManager.readBackupPayloadsForTest(
                    inputStream = ByteArrayInputStream(zipBytes),
                    maxEntryBytes = 200,
                    maxTotalBytes = 120
                )
            }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is IllegalArgumentException)
    }

    private fun assertDecimal(raw: String, expected: String) {
        val parsed = BackupManager.parseBackupDecimal(raw)
        assertNotNull("Expected decimal parse for '$raw'", parsed)
        assertEquals(0, parsed!!.compareTo(BigDecimal(expected)))
    }

    private fun buildZip(entries: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, payload) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(payload.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun extractJsonStringValue(payload: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(payload)?.groupValues?.get(1)
    }

}
