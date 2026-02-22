package com.zeynbakers.order_management_system.core.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationReplayTest {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migration12To13_nullsOrphansAndKeepsIntegrity() {
        helper.createDatabase(TEST_DB, 12).apply {
            createV12Schema(this)
            seedV12Data(this)
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DB,
                13,
                true,
                DatabaseProvider.migration12To13
            )

        assertNull(migrated.queryLongOrNull("SELECT customerId FROM orders WHERE id = 10"))
        assertEquals(1L, migrated.queryLongOrNull("SELECT customerId FROM orders WHERE id = 11"))

        assertNull(migrated.queryLongOrNull("SELECT orderId FROM account_entries WHERE id = 20"))
        assertNull(migrated.queryLongOrNull("SELECT customerId FROM account_entries WHERE id = 20"))
        assertEquals(11L, migrated.queryLongOrNull("SELECT orderId FROM account_entries WHERE id = 21"))

        assertNull(migrated.queryLongOrNull("SELECT customerId FROM payment_receipts WHERE id = 30"))
        assertEquals(1L, migrated.queryLongOrNull("SELECT customerId FROM payment_receipts WHERE id = 31"))

        assertNull(migrated.queryLongOrNull("SELECT orderId FROM payment_allocations WHERE id = 40"))
        assertNull(migrated.queryLongOrNull("SELECT customerId FROM payment_allocations WHERE id = 40"))
        assertNull(migrated.queryLongOrNull("SELECT accountEntryId FROM payment_allocations WHERE id = 40"))
        assertEquals(0L, migrated.queryLongOrNull("SELECT COUNT(*) FROM payment_allocations WHERE id = 42"))

        val fkViolations = migrated.queryLongOrNull("SELECT COUNT(*) FROM pragma_foreign_key_check")
        assertEquals(0L, fkViolations)
    }

    private fun createV12Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                isArchived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_customers_phone ON customers(phone)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_name ON customers(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_isArchived ON customers(isArchived)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                orderDate TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                notes TEXT NOT NULL,
                pickupTime TEXT,
                status TEXT NOT NULL,
                statusOverride TEXT,
                totalAmount TEXT NOT NULL,
                customerId INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_orderDate ON orders(orderDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_customerId ON orders(customerId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                orderId INTEGER NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unitPrice TEXT NOT NULL,
                FOREIGN KEY(orderId) REFERENCES orders(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                orderId INTEGER NOT NULL,
                amount TEXT NOT NULL,
                method TEXT NOT NULL,
                paidAt INTEGER NOT NULL,
                FOREIGN KEY(orderId) REFERENCES orders(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_orderId ON payments(orderId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS account_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                orderId INTEGER,
                customerId INTEGER,
                type TEXT NOT NULL,
                amount TEXT NOT NULL,
                date INTEGER NOT NULL,
                description TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_orderId ON account_entries(orderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_customerId ON account_entries(customerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_date ON account_entries(date)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payment_receipts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount TEXT NOT NULL,
                receivedAt INTEGER NOT NULL,
                method TEXT NOT NULL,
                transactionCode TEXT,
                hash TEXT,
                senderName TEXT,
                senderPhone TEXT,
                rawText TEXT,
                customerId INTEGER,
                note TEXT,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                voidedAt INTEGER,
                voidReason TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_transactionCode ON payment_receipts(transactionCode)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_hash ON payment_receipts(hash)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_receipts_customerId ON payment_receipts(customerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_receipts_receivedAt ON payment_receipts(receivedAt)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payment_allocations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                receiptId INTEGER NOT NULL,
                orderId INTEGER,
                customerId INTEGER,
                amount TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                accountEntryId INTEGER,
                reversalEntryId INTEGER,
                createdAt INTEGER NOT NULL,
                voidedAt INTEGER,
                voidReason TEXT,
                FOREIGN KEY(receiptId) REFERENCES payment_receipts(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_receiptId ON payment_allocations(receiptId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_orderId ON payment_allocations(orderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_customerId ON payment_allocations(customerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_accountEntryId ON payment_allocations(accountEntryId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS helper_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                type TEXT NOT NULL,
                rawTranscript TEXT NOT NULL,
                displayText TEXT NOT NULL,
                calculatorExpression TEXT,
                calculatorResult TEXT,
                detectedPhone TEXT,
                detectedPhoneDigits TEXT,
                detectedAmountRaw TEXT,
                detectedAmountNormalized TEXT,
                linkedCustomerId INTEGER,
                sourceApp TEXT,
                pinned INTEGER NOT NULL DEFAULT 0,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_helper_notes_createdAt ON helper_notes(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_helper_notes_pinned_createdAt ON helper_notes(pinned, createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_helper_notes_detectedPhoneDigits ON helper_notes(detectedPhoneDigits)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_helper_notes_detectedAmountNormalized ON helper_notes(detectedAmountNormalized)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_helper_notes_type ON helper_notes(type)")
    }

    private fun seedV12Data(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO customers(id, name, phone, isArchived) VALUES (1, 'Valid Customer', '+111111111', 0)")
        db.execSQL(
            "INSERT INTO orders(id, orderDate, createdAt, updatedAt, notes, pickupTime, status, statusOverride, totalAmount, customerId) " +
                "VALUES (10, '2026-02-01', 100, 100, 'Orphan order', NULL, 'PENDING', NULL, '100.00', 999)"
        )
        db.execSQL(
            "INSERT INTO orders(id, orderDate, createdAt, updatedAt, notes, pickupTime, status, statusOverride, totalAmount, customerId) " +
                "VALUES (11, '2026-02-01', 100, 100, 'Valid order', NULL, 'PENDING', NULL, '120.00', 1)"
        )
        db.execSQL("INSERT INTO payments(id, orderId, amount, method, paidAt) VALUES (1, 11, '20.00', 'CASH', 200)")

        db.execSQL(
            "INSERT INTO account_entries(id, orderId, customerId, type, amount, date, description) " +
                "VALUES (20, 999, 999, 'DEBIT', '10.00', 200, 'orphan links')"
        )
        db.execSQL(
            "INSERT INTO account_entries(id, orderId, customerId, type, amount, date, description) " +
                "VALUES (21, 11, 1, 'DEBIT', '20.00', 200, 'valid links')"
        )

        db.execSQL(
            "INSERT INTO payment_receipts(id, amount, receivedAt, method, transactionCode, hash, senderName, senderPhone, rawText, customerId, note, status, createdAt, voidedAt, voidReason) " +
                "VALUES (30, '30.00', 200, 'CASH', NULL, NULL, NULL, NULL, NULL, 999, NULL, 'APPLIED', 200, NULL, NULL)"
        )
        db.execSQL(
            "INSERT INTO payment_receipts(id, amount, receivedAt, method, transactionCode, hash, senderName, senderPhone, rawText, customerId, note, status, createdAt, voidedAt, voidReason) " +
                "VALUES (31, '40.00', 200, 'CASH', NULL, NULL, NULL, NULL, NULL, 1, NULL, 'APPLIED', 200, NULL, NULL)"
        )

        db.execSQL(
            "INSERT INTO payment_allocations(id, receiptId, orderId, customerId, amount, type, status, accountEntryId, reversalEntryId, createdAt, voidedAt, voidReason) " +
                "VALUES (40, 31, 999, 999, '10.00', 'ORDER', 'APPLIED', 999, 999, 200, NULL, NULL)"
        )
        db.execSQL(
            "INSERT INTO payment_allocations(id, receiptId, orderId, customerId, amount, type, status, accountEntryId, reversalEntryId, createdAt, voidedAt, voidReason) " +
                "VALUES (41, 31, 11, 1, '12.00', 'ORDER', 'APPLIED', 21, NULL, 200, NULL, NULL)"
        )
        db.execSQL(
            "INSERT INTO payment_allocations(id, receiptId, orderId, customerId, amount, type, status, accountEntryId, reversalEntryId, createdAt, voidedAt, voidReason) " +
                "VALUES (42, 999, 11, 1, '5.00', 'ORDER', 'APPLIED', 21, NULL, 200, NULL, NULL)"
        )
    }

    private fun SupportSQLiteDatabase.queryLongOrNull(sql: String): Long? {
        query(sql).use { cursor ->
            if (!cursor.moveToFirst()) return null
            if (cursor.isNull(0)) return null
            return cursor.getLong(0)
        }
    }

    companion object {
        private const val TEST_DB = "oms-migration-test"
    }
}
