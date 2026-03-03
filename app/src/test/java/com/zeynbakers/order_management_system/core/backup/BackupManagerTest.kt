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
    fun `saf directory backups use rolling single-file name`() {
        val name =
            BackupManager.backupFileNameForTargetForTest(
                targetType = BackupTargetType.SafDirectory,
                timestamp = 1_737_624_000_000L
            )
        assertEquals("backup_latest.oms", name)
    }

    @Test
    fun `saf file backups keep timestamped archive naming`() {
        val name =
            BackupManager.backupFileNameForTargetForTest(
                targetType = BackupTargetType.SafFile,
                timestamp = 1_737_624_000_000L
            )
        assertTrue(name.startsWith("oms-backup-"))
        assertTrue(name.endsWith(".oms"))
    }

    @Test
    fun `encrypted archive roundtrip restores original bytes`() {
        val source = "orders payload 123".toByteArray(Charsets.UTF_8)
        val encrypted = BackupManager.encryptArchiveForTest(source, "StrongPass#2026")
        val decoded = BackupManager.decryptArchiveForTest(encrypted, "StrongPass#2026")

        assertTrue(encrypted.size > source.size)
        assertEquals(source.toList(), decoded.toList())
    }

    @Test
    fun `encrypted archive rejects wrong passphrase`() {
        val source = "customer payload 456".toByteArray(Charsets.UTF_8)
        val encrypted = BackupManager.encryptArchiveForTest(source, "StrongPass#2026")

        val error =
            runCatching {
                BackupManager.decryptArchiveForTest(encrypted, "WrongPass#2026")
            }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is IllegalArgumentException)
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

    @Test
    fun `restore preflight rejects metadata count mismatch`() {
        val payloads = buildValidRestorePayloads()
        payloads["metadata.json"] =
            """
            {
              "exportedAt": 1,
              "appVersionName": "test",
              "appVersionCode": 1,
              "dbVersion": 11,
              "counts": {
                "customers.json": 1,
                "orders.json": 2,
                "order_items.json": 1,
                "account_entries.json": 1,
                "payments.json": 1,
                "payment_receipts.json": 0,
                "payment_allocations.json": 0,
                "helper_notes.json": 0
              }
            }
            """.trimIndent()

        val error =
            runCatching {
                BackupManager.validateRestorePayloadsForTest(
                    payloads = payloads,
                    currentDbVersion = 11
                )
            }.exceptionOrNull()

        assertNotNull(error)
    }

    @Test
    fun `restore preflight rejects missing customer references`() {
        val payloads = buildValidRestorePayloads()
        payloads["orders.json"] =
            """
            [
              {
                "id": 10,
                "orderDate": "2026-02-22",
                "createdAt": 1,
                "updatedAt": 1,
                "notes": "Sample order",
                "pickupTime": null,
                "status": "PENDING",
                "statusOverride": null,
                "totalAmount": "100",
                "customerId": 999
              }
            ]
            """.trimIndent()

        val error =
            runCatching {
                BackupManager.validateRestorePayloadsForTest(
                    payloads = payloads,
                    currentDbVersion = 11
                )
            }.exceptionOrNull()

        assertNotNull(error)
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

    private fun buildValidRestorePayloads(): MutableMap<String, String> {
        return linkedMapOf(
            "metadata.json" to
                """
                {
                  "exportedAt": 1,
                  "appVersionName": "test",
                  "appVersionCode": 1,
                  "dbVersion": 11,
                  "counts": {
                    "customers.json": 1,
                    "orders.json": 1,
                    "order_items.json": 1,
                    "account_entries.json": 1,
                    "payments.json": 1,
                    "payment_receipts.json": 0,
                    "payment_allocations.json": 0,
                    "helper_notes.json": 0
                  }
                }
                """.trimIndent(),
            "customers.json" to
                """
                [
                  {
                    "id": 1,
                    "name": "Customer One",
                    "phone": "0700000000",
                    "isArchived": false
                  }
                ]
                """.trimIndent(),
            "orders.json" to
                """
                [
                  {
                    "id": 10,
                    "orderDate": "2026-02-22",
                    "createdAt": 1,
                    "updatedAt": 1,
                    "notes": "Sample order",
                    "pickupTime": null,
                    "status": "PENDING",
                    "statusOverride": null,
                    "totalAmount": "100",
                    "customerId": 1
                  }
                ]
                """.trimIndent(),
            "order_items.json" to
                """
                [
                  {
                    "id": 100,
                    "orderId": 10,
                    "name": "Bread",
                    "category": "BAKED",
                    "quantity": 1,
                    "unitPrice": "100"
                  }
                ]
                """.trimIndent(),
            "account_entries.json" to
                """
                [
                  {
                    "id": 200,
                    "orderId": 10,
                    "customerId": 1,
                    "type": "DEBIT",
                    "amount": "100",
                    "date": 1,
                    "description": "Charge: Order #10"
                  }
                ]
                """.trimIndent(),
            "payments.json" to
                """
                [
                  {
                    "id": 300,
                    "orderId": 10,
                    "amount": "100",
                    "method": "CASH",
                    "paidAt": 1
                  }
                ]
                """.trimIndent(),
            "payment_receipts.json" to "[]",
            "payment_allocations.json" to "[]",
            "helper_notes.json" to "[]"
        )
    }

}
