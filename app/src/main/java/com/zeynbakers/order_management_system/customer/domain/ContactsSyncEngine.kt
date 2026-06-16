package com.zeynbakers.order_management_system.customer.domain

import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.expandPhoneCandidates
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.ImportContact

data class ContactsSyncResult(
    val processed: Int,
    val added: Int,
    val updated: Int,
    val unchanged: Int
)

enum class ContactImportPreviewStatus {
    New,
    Update,
    Existing
}

internal fun previewContactImportStatuses(
    existingCustomers: List<CustomerEntity>,
    contacts: List<ImportContact>
): Map<String, ContactImportPreviewStatus> {
    val customersByPhone = existingCustomers.associateBy { it.phone }
    return contacts.associate { contact ->
        contact.phone to previewContactImportStatus(customersByPhone, contact)
    }
}

private fun previewContactImportStatus(
    customersByPhone: Map<String, CustomerEntity>,
    contact: ImportContact
): ContactImportPreviewStatus {
    val normalizedPhone = normalizePhoneNumberE164(contact.phone) ?: return ContactImportPreviewStatus.New
    val cleanName = contact.name.trim().ifBlank { normalizedPhone }
    val exactMatch = customersByPhone[normalizedPhone]
    val existing = exactMatch ?: findExistingCustomerByPhoneCandidates(customersByPhone, normalizedPhone)
    if (existing == null) return ContactImportPreviewStatus.New

    val merged =
        mergeExistingCustomer(
            existing = existing,
            normalizedPhone = normalizedPhone,
            cleanName = cleanName,
            canUpdatePhone = canUpdatePhone(existing, exactMatch, normalizedPhone)
        )
    return if (merged != existing) {
        ContactImportPreviewStatus.Update
    } else {
        ContactImportPreviewStatus.Existing
    }
}

suspend fun syncContactsIntoCustomers(
    database: AppDatabase,
    contacts: List<ImportContact>
): ContactsSyncResult {
    val customerDao = database.customerDao()
    var processed = 0
    var added = 0
    var updated = 0
    var unchanged = 0

    for (contact in contacts) {
        val normalizedPhone = normalizePhoneNumberE164(contact.phone) ?: continue
        val cleanName = contact.name.trim().ifBlank { normalizedPhone }
        processed += 1

        val exactMatch = customerDao.getByPhone(normalizedPhone)
        val existing =
            exactMatch ?: customerDao.getByPhones(expandPhoneCandidates(normalizedPhone))
        if (existing != null) {
            val merged = mergeExistingCustomer(
                existing = existing,
                normalizedPhone = normalizedPhone,
                cleanName = cleanName,
                canUpdatePhone = canUpdatePhone(existing, exactMatch, normalizedPhone)
            )
            if (merged != existing) {
                customerDao.update(merged)
                updated += 1
            } else {
                unchanged += 1
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
            } else {
                unchanged += 1
            }
        }
    }

    return ContactsSyncResult(
        processed = processed,
        added = added,
        updated = updated,
        unchanged = unchanged
    )
}

private fun canUpdatePhone(
    existing: CustomerEntity,
    exactMatch: CustomerEntity?,
    normalizedPhone: String
): Boolean {
    return existing.phone != normalizedPhone && exactMatch == null
}

private fun findExistingCustomerByPhoneCandidates(
    customersByPhone: Map<String, CustomerEntity>,
    normalizedPhone: String
): CustomerEntity? {
    return expandPhoneCandidates(normalizedPhone)
        .asSequence()
        .mapNotNull { customersByPhone[it] }
        .firstOrNull()
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
