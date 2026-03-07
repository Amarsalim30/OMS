package com.zeynbakers.order_management_system.core.contacts

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zeynbakers.order_management_system.loadAllContacts
import com.zeynbakers.order_management_system.loadAllContactsResult
import com.zeynbakers.order_management_system.ContactsLoadResult
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.customer.domain.syncContactsIntoCustomers
import kotlinx.coroutines.CancellationException

class ContactsSyncWorker(
    appContext: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            when (val contactsResult = loadAllContactsResult(applicationContext)) {
                is ContactsLoadResult.Success -> {
                    if (contactsResult.contacts.isNotEmpty()) {
                        val database = DatabaseProvider.getDatabase(applicationContext)
                        syncContactsIntoCustomers(
                            database = database,
                            contacts = contactsResult.contacts
                        )
                    }
                    Result.success()
                }

                ContactsLoadResult.PermissionMissing -> Result.success()

                is ContactsLoadResult.PermanentFailure -> Result.failure()

                is ContactsLoadResult.TransientFailure -> Result.retry()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SecurityException) {
            Result.success()
        } catch (_: IllegalArgumentException) {
            Result.failure()
        } catch (_: IllegalStateException) {
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
