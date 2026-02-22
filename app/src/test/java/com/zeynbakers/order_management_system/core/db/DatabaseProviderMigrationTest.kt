package com.zeynbakers.order_management_system.core.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseProviderMigrationTest {

    @Test
    fun `migration 9 to 10 orders schema removes amountPaid column`() {
        val createSql = DatabaseProvider.SQL_CREATE_ORDERS_V10
        val copySql = DatabaseProvider.SQL_COPY_ORDERS_TO_V10

        assertFalse(createSql.contains("amountPaid", ignoreCase = true))
        assertFalse(copySql.contains("amountPaid", ignoreCase = true))
        assertTrue(createSql.contains("totalAmount", ignoreCase = true))
        assertTrue(createSql.contains("customerId", ignoreCase = true))
    }

    @Test
    fun `migration 9 to 10 recreates orders indices`() {
        assertTrue(DatabaseProvider.SQL_INDEX_ORDERS_DATE.contains("index_orders_orderDate"))
        assertTrue(DatabaseProvider.SQL_INDEX_ORDERS_CUSTOMER.contains("index_orders_customerId"))
        assertTrue(DatabaseProvider.SQL_RENAME_ORDERS_NEW.contains("RENAME TO orders"))
    }

    @Test
    fun `migration 10 to 11 creates helper notes table`() {
        val createSql = DatabaseProvider.SQL_CREATE_HELPER_NOTES_V11

        assertTrue(createSql.contains("CREATE TABLE IF NOT EXISTS helper_notes", ignoreCase = true))
        assertTrue(createSql.contains("rawTranscript", ignoreCase = true))
        assertTrue(createSql.contains("displayText", ignoreCase = true))
        assertTrue(createSql.contains("detectedPhoneDigits", ignoreCase = true))
        assertTrue(createSql.contains("detectedAmountNormalized", ignoreCase = true))
        assertTrue(createSql.contains("pinned", ignoreCase = true))
    }

    @Test
    fun `migration 10 to 11 creates helper notes indices`() {
        assertTrue(
            DatabaseProvider.SQL_INDEX_HELPER_NOTES_CREATED_AT.contains(
                "index_helper_notes_createdAt",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_INDEX_HELPER_NOTES_PINNED_CREATED_AT.contains(
                "index_helper_notes_pinned_createdAt",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_INDEX_HELPER_NOTES_PHONE_DIGITS.contains(
                "index_helper_notes_detectedPhoneDigits",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_INDEX_HELPER_NOTES_AMOUNT_NORMALIZED.contains(
                "index_helper_notes_detectedAmountNormalized",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_INDEX_HELPER_NOTES_TYPE.contains(
                "index_helper_notes_type",
                ignoreCase = true
            )
        )
    }

    @Test
    fun `migration 11 to 12 normalizes amount columns to cents`() {
        assertTrue(DatabaseProvider.SQL_NORMALIZE_ORDERS_TOTAL_AMOUNT_V12.contains("orders", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_NORMALIZE_PAYMENTS_AMOUNT_V12.contains("payments", ignoreCase = true))
        assertTrue(
            DatabaseProvider.SQL_NORMALIZE_ACCOUNT_ENTRIES_AMOUNT_V12.contains(
                "account_entries",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_NORMALIZE_PAYMENT_RECEIPTS_AMOUNT_V12.contains(
                "payment_receipts",
                ignoreCase = true
            )
        )
        assertTrue(
            DatabaseProvider.SQL_NORMALIZE_PAYMENT_ALLOCATIONS_AMOUNT_V12.contains(
                "payment_allocations",
                ignoreCase = true
            )
        )
    }

    @Test
    fun `migration 11 to 12 creates customer search indexes`() {
        assertTrue(DatabaseProvider.SQL_INDEX_CUSTOMERS_NAME.contains("index_customers_name"))
        assertTrue(DatabaseProvider.SQL_INDEX_CUSTOMERS_ARCHIVED.contains("index_customers_isArchived"))
    }

    @Test
    fun `migration 12 to 13 recreates linked tables with foreign keys`() {
        assertTrue(DatabaseProvider.SQL_CREATE_ORDERS_V13.contains("FOREIGN KEY(customerId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_ACCOUNT_ENTRIES_V13.contains("FOREIGN KEY(orderId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_ACCOUNT_ENTRIES_V13.contains("FOREIGN KEY(customerId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_PAYMENT_RECEIPTS_V13.contains("FOREIGN KEY(customerId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_PAYMENT_ALLOCATIONS_V13.contains("FOREIGN KEY(receiptId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_PAYMENT_ALLOCATIONS_V13.contains("FOREIGN KEY(orderId)", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_CREATE_PAYMENT_ALLOCATIONS_V13.contains("FOREIGN KEY(accountEntryId)", ignoreCase = true))
    }

    @Test
    fun `migration 12 to 13 copies invalid links as null and recreates indexes`() {
        assertTrue(DatabaseProvider.SQL_COPY_ORDERS_TO_V13.contains("ELSE NULL", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_COPY_ACCOUNT_ENTRIES_TO_V13.contains("ELSE NULL", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_COPY_PAYMENT_RECEIPTS_TO_V13.contains("ELSE NULL", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_COPY_PAYMENT_ALLOCATIONS_TO_V13.contains("ELSE NULL", ignoreCase = true))
        assertTrue(DatabaseProvider.SQL_INDEX_PAYMENT_ALLOCATIONS_REVERSAL_ENTRY_ID.contains("reversalEntryId"))
    }
}
