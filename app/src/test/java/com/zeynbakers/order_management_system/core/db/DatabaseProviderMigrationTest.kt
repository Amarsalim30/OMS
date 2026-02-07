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
}
