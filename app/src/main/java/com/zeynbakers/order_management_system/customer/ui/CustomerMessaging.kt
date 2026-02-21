package com.zeynbakers.order_management_system.customer.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164

internal fun launchCustomerMessage(context: Context, phone: String) {
    val normalizedPhone = normalizePhoneNumberE164(phone) ?: return
    val waDigits = normalizedPhone.filter { it.isDigit() }
    val waIntent =
        Intent(Intent.ACTION_VIEW, "https://wa.me/$waDigits".toUri())
            .setPackage("com.whatsapp")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val smsIntent =
        Intent(Intent.ACTION_VIEW, "sms:$normalizedPhone".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val packageManager = context.packageManager
    when {
        waIntent.resolveActivity(packageManager) != null -> context.startActivity(waIntent)
        smsIntent.resolveActivity(packageManager) != null -> context.startActivity(smsIntent)
    }
}
