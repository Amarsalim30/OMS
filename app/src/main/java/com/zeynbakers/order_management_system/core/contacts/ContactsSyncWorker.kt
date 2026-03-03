package com.zeynbakers.order_management_system.core.contacts

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zeynbakers.order_management_system.loadAllContacts
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.customer.domain.syncContactsIntoCustomers

class ContactsSyncWorker(
    appContext: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val contacts = loadAllContacts(applicationContext)
            if (contacts.isNotEmpty()) {
                val database = DatabaseProvider.getDatabase(applicationContext)
                syncContactsIntoCustomers(
                    database = database,
                    contacts = contacts
                )
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
