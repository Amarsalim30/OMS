package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri

fun persistSafUriPermission(
    context: Context,
    uri: Uri,
    resultFlags: Int,
    requiredFlags: Int
): Boolean {
    val rwMask = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    val required = requiredFlags and rwMask
    val granted = resultFlags and rwMask
    if (required != 0 && (granted and required) == required) {
        val grantedResult =
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, granted)
                true
            }.getOrDefault(false)
        if (grantedResult) {
            return true
        }
    }

    if (required == 0) return false
    return runCatching {
        context.contentResolver.takePersistableUriPermission(uri, required)
        true
    }.getOrDefault(false)
}
