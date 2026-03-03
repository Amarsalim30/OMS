package com.zeynbakers.order_management_system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.customer.ui.ImportContact

internal suspend fun loadAllContacts(context: Context): List<ImportContact> {
    val hasPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return emptyList()

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
            ) ?: return emptyList()

        val results = mutableListOf<ImportContact>()
        val seen = mutableSetOf<String>()
        cursor.use { phones ->
            val nameIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameIndex == -1 || numberIndex == -1) return emptyList()

            while (phones.moveToNext()) {
                val rawName = phones.getString(nameIndex) ?: ""
                val rawNumber = phones.getString(numberIndex) ?: ""
                val cleanNumber = normalizePhoneNumberE164(rawNumber) ?: continue
                if (!seen.add(cleanNumber)) continue
                val displayName = rawName.trim().ifBlank { cleanNumber }
                results.add(ImportContact(name = displayName, phone = cleanNumber))
            }
        }
        results
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}
