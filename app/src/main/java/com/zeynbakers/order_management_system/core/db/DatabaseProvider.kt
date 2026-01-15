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

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "order_app_db"
            ).addMigrations(migration1To2, migration2To3, migration3To4).build()
            instance = db
            db
        }
    }
}
