package com.zeynbakers.order_management_system.customer.domain

import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.expandPhoneCandidates
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.ImportContact

data class ContactsSyncResult(
    val processed: Int,
    val added: Int,
    val updated: Int
)

suspend fun syncContactsIntoCustomers(
    database: AppDatabase,
    contacts: List<ImportContact>
): ContactsSyncResult {
    val customerDao = database.customerDao()
    var processed = 0
    var added = 0
    var updated = 0

    for (contact in contacts) {
        val normalizedPhone = normalizePhoneNumberE164(contact.phone) ?: continue
        val cleanName = contact.name.trim().ifBlank { normalizedPhone }
        processed += 1

        val exactMatch = customerDao.getByPhone(normalizedPhone)
        val existing =
            exactMatch ?: customerDao.getByPhones(expandPhoneCandidates(contact.phone))
        if (existing != null) {
            val canUpdatePhone =
                existing.phone != normalizedPhone &&
                    exactMatch == null &&
                    customerDao.getByPhone(normalizedPhone) == null
            val merged = mergeExistingCustomer(
                existing = existing,
                normalizedPhone = normalizedPhone,
                cleanName = cleanName,
                canUpdatePhone = canUpdatePhone
            )
            if (merged != existing) {
                customerDao.update(merged)
                updated += 1
            }
            continue
        }

        val insertedId =
            customerDao.insertIgnore(
                CustomerEntity(
                    name = cleanName,
                    phone = normalizedPhone
                )
            )
        if (insertedId != -1L) {
            added += 1
            continue
        }

        customerDao.getByPhone(normalizedPhone)?.let { concurrent ->
            val shouldUpdate = concurrent.isArchived || concurrent.name != cleanName
            if (shouldUpdate) {
                customerDao.update(
                    concurrent.copy(
                        name = cleanName.ifBlank { concurrent.name },
                        isArchived = false
                    )
                )
                updated += 1
            }
        }
    }

    return ContactsSyncResult(
        processed = processed,
        added = added,
        updated = updated
    )
}

private fun mergeExistingCustomer(
    existing: CustomerEntity,
    normalizedPhone: String,
    cleanName: String,
    canUpdatePhone: Boolean
): CustomerEntity {
    return existing.copy(
        name = if (cleanName.isNotBlank()) cleanName else existing.name,
        phone = if (canUpdatePhone) normalizedPhone else existing.phone,
        isArchived = false
    )
}
