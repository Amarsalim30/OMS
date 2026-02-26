package com.zeynbakers.order_management_system.core.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    private var instance: AppDatabase? = null
    internal val SQL_CREATE_ORDERS_V10 =
        """
        CREATE TABLE IF NOT EXISTS orders_new (
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
    internal val SQL_COPY_ORDERS_TO_V10 =
        """
        INSERT INTO orders_new (id, orderDate, createdAt, updatedAt, notes, pickupTime, status, statusOverride, totalAmount, customerId)
        SELECT id, orderDate, createdAt, updatedAt, notes, pickupTime, status, statusOverride, totalAmount, customerId
        FROM orders
        """.trimIndent()
    internal const val SQL_DROP_ORDERS = "DROP TABLE orders"
    internal const val SQL_RENAME_ORDERS_NEW = "ALTER TABLE orders_new RENAME TO orders"
    internal const val SQL_INDEX_ORDERS_DATE = "CREATE INDEX IF NOT EXISTS index_orders_orderDate ON orders(orderDate)"
    internal const val SQL_INDEX_ORDERS_CUSTOMER = "CREATE INDEX IF NOT EXISTS index_orders_customerId ON orders(customerId)"
    internal val SQL_CREATE_HELPER_NOTES_V11 =
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
    internal const val SQL_INDEX_HELPER_NOTES_CREATED_AT =
        "CREATE INDEX IF NOT EXISTS index_helper_notes_createdAt ON helper_notes(createdAt)"
    internal const val SQL_INDEX_HELPER_NOTES_PINNED_CREATED_AT =
        "CREATE INDEX IF NOT EXISTS index_helper_notes_pinned_createdAt ON helper_notes(pinned, createdAt)"
    internal const val SQL_INDEX_HELPER_NOTES_PHONE_DIGITS =
        "CREATE INDEX IF NOT EXISTS index_helper_notes_detectedPhoneDigits ON helper_notes(detectedPhoneDigits)"
    internal const val SQL_INDEX_HELPER_NOTES_AMOUNT_NORMALIZED =
        "CREATE INDEX IF NOT EXISTS index_helper_notes_detectedAmountNormalized ON helper_notes(detectedAmountNormalized)"
    internal const val SQL_INDEX_HELPER_NOTES_TYPE =
        "CREATE INDEX IF NOT EXISTS index_helper_notes_type ON helper_notes(type)"
    internal const val SQL_NORMALIZE_ORDERS_TOTAL_AMOUNT_V12 =
        "UPDATE orders SET totalAmount = CAST(ROUND(CAST(totalAmount AS REAL) * 100.0) AS INTEGER)"
    internal const val SQL_NORMALIZE_PAYMENTS_AMOUNT_V12 =
        "UPDATE payments SET amount = CAST(ROUND(CAST(amount AS REAL) * 100.0) AS INTEGER)"
    internal const val SQL_NORMALIZE_ACCOUNT_ENTRIES_AMOUNT_V12 =
        "UPDATE account_entries SET amount = CAST(ROUND(CAST(amount AS REAL) * 100.0) AS INTEGER)"
    internal const val SQL_NORMALIZE_PAYMENT_RECEIPTS_AMOUNT_V12 =
        "UPDATE payment_receipts SET amount = CAST(ROUND(CAST(amount AS REAL) * 100.0) AS INTEGER)"
    internal const val SQL_NORMALIZE_PAYMENT_ALLOCATIONS_AMOUNT_V12 =
        "UPDATE payment_allocations SET amount = CAST(ROUND(CAST(amount AS REAL) * 100.0) AS INTEGER)"
    internal const val SQL_INDEX_CUSTOMERS_NAME =
        "CREATE INDEX IF NOT EXISTS index_customers_name ON customers(name)"
    internal const val SQL_INDEX_CUSTOMERS_ARCHIVED =
        "CREATE INDEX IF NOT EXISTS index_customers_isArchived ON customers(isArchived)"
    internal val SQL_CREATE_ORDERS_V13 =
        """
        CREATE TABLE IF NOT EXISTS orders_new_v13 (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            orderDate TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            notes TEXT NOT NULL,
            pickupTime TEXT,
            status TEXT NOT NULL,
            statusOverride TEXT,
            totalAmount TEXT NOT NULL,
            customerId INTEGER,
            FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent()
    internal val SQL_COPY_ORDERS_TO_V13 =
        """
        INSERT INTO orders_new_v13 (id, orderDate, createdAt, updatedAt, notes, pickupTime, status, statusOverride, totalAmount, customerId)
        SELECT
            o.id,
            o.orderDate,
            o.createdAt,
            o.updatedAt,
            o.notes,
            o.pickupTime,
            o.status,
            o.statusOverride,
            o.totalAmount,
            CASE
                WHEN o.customerId IS NOT NULL AND EXISTS (SELECT 1 FROM customers c WHERE c.id = o.customerId) THEN o.customerId
                ELSE NULL
            END
        FROM orders o
        """.trimIndent()
    internal const val SQL_DROP_ORDERS_V13 = "DROP TABLE orders"
    internal const val SQL_RENAME_ORDERS_V13 = "ALTER TABLE orders_new_v13 RENAME TO orders"
    internal val SQL_CREATE_ACCOUNT_ENTRIES_V13 =
        """
        CREATE TABLE IF NOT EXISTS account_entries_new_v13 (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            orderId INTEGER,
            customerId INTEGER,
            type TEXT NOT NULL,
            amount TEXT NOT NULL,
            date INTEGER NOT NULL,
            description TEXT NOT NULL,
            FOREIGN KEY(orderId) REFERENCES orders(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent()
    internal val SQL_COPY_ACCOUNT_ENTRIES_TO_V13 =
        """
        INSERT INTO account_entries_new_v13 (id, orderId, customerId, type, amount, date, description)
        SELECT
            a.id,
            CASE
                WHEN a.orderId IS NOT NULL AND EXISTS (SELECT 1 FROM orders o WHERE o.id = a.orderId) THEN a.orderId
                ELSE NULL
            END,
            CASE
                WHEN a.customerId IS NOT NULL AND EXISTS (SELECT 1 FROM customers c WHERE c.id = a.customerId) THEN a.customerId
                ELSE NULL
            END,
            a.type,
            a.amount,
            a.date,
            a.description
        FROM account_entries a
        """.trimIndent()
    internal const val SQL_DROP_ACCOUNT_ENTRIES_V13 = "DROP TABLE account_entries"
    internal const val SQL_RENAME_ACCOUNT_ENTRIES_V13 = "ALTER TABLE account_entries_new_v13 RENAME TO account_entries"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_ORDER_ID =
        "CREATE INDEX IF NOT EXISTS index_account_entries_orderId ON account_entries(orderId)"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_ID =
        "CREATE INDEX IF NOT EXISTS index_account_entries_customerId ON account_entries(customerId)"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_DATE =
        "CREATE INDEX IF NOT EXISTS index_account_entries_date ON account_entries(date)"
    internal val SQL_CREATE_PAYMENT_RECEIPTS_V13 =
        """
        CREATE TABLE IF NOT EXISTS payment_receipts_new_v13 (
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
            voidReason TEXT,
            FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent()
    internal val SQL_COPY_PAYMENT_RECEIPTS_TO_V13 =
        """
        INSERT INTO payment_receipts_new_v13 (
            id,
            amount,
            receivedAt,
            method,
            transactionCode,
            hash,
            senderName,
            senderPhone,
            rawText,
            customerId,
            note,
            status,
            createdAt,
            voidedAt,
            voidReason
        )
        SELECT
            r.id,
            r.amount,
            r.receivedAt,
            r.method,
            r.transactionCode,
            r.hash,
            r.senderName,
            r.senderPhone,
            r.rawText,
            CASE
                WHEN r.customerId IS NOT NULL AND EXISTS (SELECT 1 FROM customers c WHERE c.id = r.customerId) THEN r.customerId
                ELSE NULL
            END,
            r.note,
            r.status,
            r.createdAt,
            r.voidedAt,
            r.voidReason
        FROM payment_receipts r
        """.trimIndent()
    internal const val SQL_DROP_PAYMENT_RECEIPTS_V13 = "DROP TABLE payment_receipts"
    internal const val SQL_RENAME_PAYMENT_RECEIPTS_V13 = "ALTER TABLE payment_receipts_new_v13 RENAME TO payment_receipts"
    internal const val SQL_INDEX_PAYMENT_RECEIPTS_TRANSACTION_CODE =
        "CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_transactionCode ON payment_receipts(transactionCode)"
    internal const val SQL_INDEX_PAYMENT_RECEIPTS_HASH =
        "CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_hash ON payment_receipts(hash)"
    internal const val SQL_INDEX_PAYMENT_RECEIPTS_CUSTOMER_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_receipts_customerId ON payment_receipts(customerId)"
    internal const val SQL_INDEX_PAYMENT_RECEIPTS_RECEIVED_AT =
        "CREATE INDEX IF NOT EXISTS index_payment_receipts_receivedAt ON payment_receipts(receivedAt)"
    internal val SQL_CREATE_PAYMENT_ALLOCATIONS_V13 =
        """
        CREATE TABLE IF NOT EXISTS payment_allocations_new_v13 (
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
            FOREIGN KEY(receiptId) REFERENCES payment_receipts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(orderId) REFERENCES orders(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(accountEntryId) REFERENCES account_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(reversalEntryId) REFERENCES account_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent()
    internal val SQL_COPY_PAYMENT_ALLOCATIONS_TO_V13 =
        """
        INSERT INTO payment_allocations_new_v13 (
            id,
            receiptId,
            orderId,
            customerId,
            amount,
            type,
            status,
            accountEntryId,
            reversalEntryId,
            createdAt,
            voidedAt,
            voidReason
        )
        SELECT
            a.id,
            a.receiptId,
            CASE
                WHEN a.orderId IS NOT NULL AND EXISTS (SELECT 1 FROM orders o WHERE o.id = a.orderId) THEN a.orderId
                ELSE NULL
            END,
            CASE
                WHEN a.customerId IS NOT NULL AND EXISTS (SELECT 1 FROM customers c WHERE c.id = a.customerId) THEN a.customerId
                ELSE NULL
            END,
            a.amount,
            a.type,
            a.status,
            CASE
                WHEN a.accountEntryId IS NOT NULL AND EXISTS (SELECT 1 FROM account_entries e WHERE e.id = a.accountEntryId) THEN a.accountEntryId
                ELSE NULL
            END,
            CASE
                WHEN a.reversalEntryId IS NOT NULL AND EXISTS (SELECT 1 FROM account_entries e WHERE e.id = a.reversalEntryId) THEN a.reversalEntryId
                ELSE NULL
            END,
            a.createdAt,
            a.voidedAt,
            a.voidReason
        FROM payment_allocations a
        INNER JOIN payment_receipts r ON r.id = a.receiptId
        """.trimIndent()
    internal const val SQL_DROP_PAYMENT_ALLOCATIONS_V13 = "DROP TABLE payment_allocations"
    internal const val SQL_RENAME_PAYMENT_ALLOCATIONS_V13 =
        "ALTER TABLE payment_allocations_new_v13 RENAME TO payment_allocations"
    internal const val SQL_INDEX_PAYMENT_ALLOCATIONS_RECEIPT_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_allocations_receiptId ON payment_allocations(receiptId)"
    internal const val SQL_INDEX_PAYMENT_ALLOCATIONS_ORDER_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_allocations_orderId ON payment_allocations(orderId)"
    internal const val SQL_INDEX_PAYMENT_ALLOCATIONS_CUSTOMER_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_allocations_customerId ON payment_allocations(customerId)"
    internal const val SQL_INDEX_PAYMENT_ALLOCATIONS_ACCOUNT_ENTRY_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_allocations_accountEntryId ON payment_allocations(accountEntryId)"
    internal const val SQL_INDEX_PAYMENT_ALLOCATIONS_REVERSAL_ENTRY_ID =
        "CREATE INDEX IF NOT EXISTS index_payment_allocations_reversalEntryId ON payment_allocations(reversalEntryId)"
    internal const val SQL_INDEX_ORDERS_DATE_CREATED_ID =
        "CREATE INDEX IF NOT EXISTS index_orders_orderDate_createdAt_id ON orders(orderDate, createdAt, id)"
    internal const val SQL_INDEX_ORDERS_CUSTOMER_DATE_CREATED_ID =
        "CREATE INDEX IF NOT EXISTS index_orders_customerId_orderDate_createdAt_id ON orders(customerId, orderDate, createdAt, id)"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_ORDER_TYPE_DATE_ID =
        "CREATE INDEX IF NOT EXISTS index_account_entries_orderId_type_date_id ON account_entries(orderId, type, date, id)"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_DATE_ID =
        "CREATE INDEX IF NOT EXISTS index_account_entries_customerId_date_id ON account_entries(customerId, date, id)"
    internal const val SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_ORDER_TYPE_DATE_ID =
        "CREATE INDEX IF NOT EXISTS index_account_entries_customerId_orderId_type_date_id ON account_entries(customerId, orderId, type, date, id)"

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_entries_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    orderId INTEGER,
                    customerId INTEGER,
                    type TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    description TEXT NOT NULL
                )
                """
            )
            db.execSQL(
                """
                INSERT INTO account_entries_new (id, orderId, customerId, type, amount, date, description)
                SELECT id, orderId, customerId, type, amount, date, description FROM account_entries
                """
            )
            db.execSQL("DROP TABLE account_entries")
            db.execSQL("ALTER TABLE account_entries_new RENAME TO account_entries")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_orderId ON account_entries(orderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_customerId ON account_entries(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_entries_date ON account_entries(date)")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE orders ADD COLUMN statusOverride TEXT")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Backfill missing order debits so customer balances (DEBIT - CREDIT/WRITE_OFF) remain correct after upgrades.
            db.execSQL(
                """
                INSERT INTO account_entries (orderId, customerId, type, amount, date, description)
                SELECT o.id, o.customerId, 'DEBIT', o.totalAmount, o.updatedAt, 'Charge: Order #' || o.id
                FROM orders o
                WHERE o.status != 'CANCELLED'
                AND NOT EXISTS (
                    SELECT 1 FROM account_entries a WHERE a.orderId = o.id AND a.type = 'DEBIT'
                )
                """
            )
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE orders ADD COLUMN pickupTime TEXT")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mpesa_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    transactionCode TEXT,
                    hash TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    senderName TEXT,
                    senderPhone TEXT,
                    receivedAt INTEGER NOT NULL,
                    rawText TEXT,
                    customerId INTEGER,
                    orderId INTEGER,
                    accountEntryId INTEGER
                )
                """
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_mpesa_transactions_transactionCode ON mpesa_transactions(transactionCode)"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mpesa_transactions_hash ON mpesa_transactions(hash)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mpesa_transactions_orderId ON mpesa_transactions(orderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mpesa_transactions_customerId ON mpesa_transactions(customerId)")
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
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
                """
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_transactionCode ON payment_receipts(transactionCode)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_payment_receipts_hash ON payment_receipts(hash)"
            )
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
                    FOREIGN KEY(receiptId) REFERENCES payment_receipts(id) ON DELETE CASCADE
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_receiptId ON payment_allocations(receiptId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_orderId ON payment_allocations(orderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_customerId ON payment_allocations(customerId)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_payment_allocations_accountEntryId ON payment_allocations(accountEntryId)"
            )

            db.execSQL(
                """
                INSERT INTO payment_receipts (
                    amount,
                    receivedAt,
                    method,
                    transactionCode,
                    hash,
                    senderName,
                    senderPhone,
                    rawText,
                    customerId,
                    note,
                    status,
                    createdAt,
                    voidedAt,
                    voidReason
                )
                SELECT
                    amount,
                    receivedAt,
                    'MPESA',
                    transactionCode,
                    hash,
                    senderName,
                    senderPhone,
                    rawText,
                    customerId,
                    NULL,
                    'APPLIED',
                    receivedAt,
                    NULL,
                    NULL
                FROM mpesa_transactions
                """
            )

            db.execSQL(
                """
                INSERT INTO payment_allocations (
                    receiptId,
                    orderId,
                    customerId,
                    amount,
                    type,
                    status,
                    accountEntryId,
                    reversalEntryId,
                    createdAt,
                    voidedAt,
                    voidReason
                )
                SELECT
                    pr.id,
                    tx.orderId,
                    tx.customerId,
                    tx.amount,
                    CASE WHEN tx.orderId IS NULL THEN 'CUSTOMER_CREDIT' ELSE 'ORDER' END,
                    'APPLIED',
                    tx.accountEntryId,
                    NULL,
                    tx.receivedAt,
                    NULL,
                    NULL
                FROM mpesa_transactions tx
                INNER JOIN payment_receipts pr ON pr.hash = tx.hash
                """
            )
        }
    }

    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS mpesa_transactions")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE customers ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_CREATE_ORDERS_V10)
            db.execSQL(SQL_COPY_ORDERS_TO_V10)
            db.execSQL(SQL_DROP_ORDERS)
            db.execSQL(SQL_RENAME_ORDERS_NEW)
            db.execSQL(SQL_INDEX_ORDERS_DATE)
            db.execSQL(SQL_INDEX_ORDERS_CUSTOMER)
        }
    }

    private val migration10To11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_CREATE_HELPER_NOTES_V11)
            db.execSQL(SQL_INDEX_HELPER_NOTES_CREATED_AT)
            db.execSQL(SQL_INDEX_HELPER_NOTES_PINNED_CREATED_AT)
            db.execSQL(SQL_INDEX_HELPER_NOTES_PHONE_DIGITS)
            db.execSQL(SQL_INDEX_HELPER_NOTES_AMOUNT_NORMALIZED)
            db.execSQL(SQL_INDEX_HELPER_NOTES_TYPE)
        }
    }

    private val migration11To12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_NORMALIZE_ORDERS_TOTAL_AMOUNT_V12)
            db.execSQL(SQL_NORMALIZE_PAYMENTS_AMOUNT_V12)
            db.execSQL(SQL_NORMALIZE_ACCOUNT_ENTRIES_AMOUNT_V12)
            db.execSQL(SQL_NORMALIZE_PAYMENT_RECEIPTS_AMOUNT_V12)
            db.execSQL(SQL_NORMALIZE_PAYMENT_ALLOCATIONS_AMOUNT_V12)
            db.execSQL(SQL_INDEX_CUSTOMERS_NAME)
            db.execSQL(SQL_INDEX_CUSTOMERS_ARCHIVED)
        }
    }

    internal val migration12To13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_CREATE_ORDERS_V13)
            db.execSQL(SQL_COPY_ORDERS_TO_V13)
            db.execSQL(SQL_DROP_ORDERS_V13)
            db.execSQL(SQL_RENAME_ORDERS_V13)
            db.execSQL(SQL_INDEX_ORDERS_DATE)
            db.execSQL(SQL_INDEX_ORDERS_CUSTOMER)

            db.execSQL(SQL_CREATE_ACCOUNT_ENTRIES_V13)
            db.execSQL(SQL_COPY_ACCOUNT_ENTRIES_TO_V13)
            db.execSQL(SQL_DROP_ACCOUNT_ENTRIES_V13)
            db.execSQL(SQL_RENAME_ACCOUNT_ENTRIES_V13)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_ORDER_ID)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_ID)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_DATE)

            db.execSQL(SQL_CREATE_PAYMENT_RECEIPTS_V13)
            db.execSQL(SQL_COPY_PAYMENT_RECEIPTS_TO_V13)
            db.execSQL(SQL_DROP_PAYMENT_RECEIPTS_V13)
            db.execSQL(SQL_RENAME_PAYMENT_RECEIPTS_V13)
            db.execSQL(SQL_INDEX_PAYMENT_RECEIPTS_TRANSACTION_CODE)
            db.execSQL(SQL_INDEX_PAYMENT_RECEIPTS_HASH)
            db.execSQL(SQL_INDEX_PAYMENT_RECEIPTS_CUSTOMER_ID)
            db.execSQL(SQL_INDEX_PAYMENT_RECEIPTS_RECEIVED_AT)

            db.execSQL(SQL_CREATE_PAYMENT_ALLOCATIONS_V13)
            db.execSQL(SQL_COPY_PAYMENT_ALLOCATIONS_TO_V13)
            db.execSQL(SQL_DROP_PAYMENT_ALLOCATIONS_V13)
            db.execSQL(SQL_RENAME_PAYMENT_ALLOCATIONS_V13)
            db.execSQL(SQL_INDEX_PAYMENT_ALLOCATIONS_RECEIPT_ID)
            db.execSQL(SQL_INDEX_PAYMENT_ALLOCATIONS_ORDER_ID)
            db.execSQL(SQL_INDEX_PAYMENT_ALLOCATIONS_CUSTOMER_ID)
            db.execSQL(SQL_INDEX_PAYMENT_ALLOCATIONS_ACCOUNT_ENTRY_ID)
            db.execSQL(SQL_INDEX_PAYMENT_ALLOCATIONS_REVERSAL_ENTRY_ID)
        }
    }

    internal val migration13To14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SQL_INDEX_ORDERS_DATE_CREATED_ID)
            db.execSQL(SQL_INDEX_ORDERS_CUSTOMER_DATE_CREATED_ID)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_ORDER_TYPE_DATE_ID)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_DATE_ID)
            db.execSQL(SQL_INDEX_ACCOUNT_ENTRIES_CUSTOMER_ORDER_TYPE_DATE_ID)
        }
    }

    internal val ALL_MIGRATIONS = arrayOf(
        migration1To2,
        migration2To3,
        migration3To4,
        migration4To5,
        migration5To6,
        migration6To7,
        migration7To8,
        migration8To9,
        migration9To10,
        migration10To11,
        migration11To12,
        migration12To13,
        migration13To14
    )

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "order_app_db"
            ).addMigrations(*ALL_MIGRATIONS).build()
            instance = db
            db
        }
    }
}
