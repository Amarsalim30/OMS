package com.zeynbakers.order_management_system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.OperationCanceledException
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import kotlinx.coroutines.CancellationException

internal suspend fun loadAllContacts(context: Context): List<ImportContact> {
    return when (val result = loadAllContactsResult(context)) {
        is ContactsLoadResult.Success -> result.contacts
        ContactsLoadResult.PermissionMissing -> emptyList()
        is ContactsLoadResult.PermanentFailure -> emptyList()
        is ContactsLoadResult.TransientFailure -> emptyList()
    }
}

internal suspend fun loadAllContactsResult(context: Context): ContactsLoadResult {
    val hasPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return ContactsLoadResult.PermissionMissing

    val resolver = context.contentResolver
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    return try {
        val cursor =
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            ) ?: return ContactsLoadResult.Success(emptyList())

        val results = mutableListOf<ImportContact>()
        val seen = mutableSetOf<String>()
        cursor.use { phones ->
            val nameIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameIndex == -1 || numberIndex == -1) {
                return ContactsLoadResult.PermanentFailure("Contacts provider projection is missing required columns")
            }

            while (phones.moveToNext()) {
                val rawName = phones.getString(nameIndex) ?: ""
                val rawNumber = phones.getString(numberIndex) ?: ""
                val cleanNumber = normalizePhoneNumberE164(rawNumber) ?: continue
                if (!seen.add(cleanNumber)) continue
                val displayName = rawName.trim().ifBlank { cleanNumber }
                results.add(ImportContact(name = displayName, phone = cleanNumber))
            }
        }
        ContactsLoadResult.Success(results)
    } catch (error: CancellationException) {
        throw error
    } catch (_: SecurityException) {
        ContactsLoadResult.PermissionMissing
    } catch (error: Exception) {
        when (classifyContactsLoadFailure(error)) {
            ContactsLoadFailureKind.Transient ->
                ContactsLoadResult.TransientFailure(error.message)
            ContactsLoadFailureKind.Permanent ->
                ContactsLoadResult.PermanentFailure(error.message)
        }
    }
}

internal sealed interface ContactsLoadResult {
    data class Success(val contacts: List<ImportContact>) : ContactsLoadResult
    data object PermissionMissing : ContactsLoadResult
    data class TransientFailure(val message: String?) : ContactsLoadResult
    data class PermanentFailure(val message: String?) : ContactsLoadResult
}

internal enum class ContactsLoadFailureKind {
    Transient,
    Permanent
}

internal fun classifyContactsLoadFailure(error: Exception): ContactsLoadFailureKind {
    return when (error) {
        is OperationCanceledException -> ContactsLoadFailureKind.Transient
        is IllegalArgumentException -> ContactsLoadFailureKind.Permanent
        is IllegalStateException -> ContactsLoadFailureKind.Transient
        is RuntimeException -> ContactsLoadFailureKind.Transient
        else -> ContactsLoadFailureKind.Transient
    }
}
