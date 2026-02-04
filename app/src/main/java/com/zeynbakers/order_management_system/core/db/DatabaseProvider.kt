package com.zeynbakers.order_management_system.core.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    private var instance: AppDatabase? = null
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

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "order_app_db"
            ).addMigrations(
                migration1To2,
                migration2To3,
                migration3To4,
                migration4To5,
                migration5To6,
                migration6To7,
                migration7To8,
                migration8To9
            ).build()
            instance = db
            db
        }
    }
}
