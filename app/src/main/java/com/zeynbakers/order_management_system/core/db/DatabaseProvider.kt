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

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "order_app_db"
            ).addMigrations(migration1To2).build()
            instance = db
            db
        }
    }
}
