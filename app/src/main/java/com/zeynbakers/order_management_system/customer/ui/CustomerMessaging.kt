package com.zeynbakers.order_management_system.customer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumber
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164

internal fun launchCustomerMessage(context: Context, phone: String) {
    val targetNumber = resolveMessagingNumber(phone) ?: return
    val waDigits = whatsappDigits(targetNumber)

    if (waDigits.isNotBlank()) {
        val waUri = "https://wa.me/$waDigits".toUri()
        val waCandidates =
            listOf(
                Intent(Intent.ACTION_VIEW, waUri).setPackage("com.whatsapp"),
                Intent(Intent.ACTION_VIEW, waUri).setPackage("com.whatsapp.w4b"),
                Intent(Intent.ACTION_VIEW, waUri)
            )
        waCandidates.forEach { intent ->
            if (context.tryStart(intent)) {
                return
            }
        }
    }

    val smsCandidates =
        listOf(
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", targetNumber, null)),
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", targetNumber, null)),
            Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", targetNumber, null))
        )
    smsCandidates.forEach { intent ->
        if (context.tryStart(intent)) {
            return
        }
    }
}

internal fun resolveMessagingNumber(rawPhone: String): String? {
    val normalized = normalizePhoneNumberE164(rawPhone)
    if (!normalized.isNullOrBlank()) return normalized
    val cleaned = normalizePhoneNumber(rawPhone)
    return cleaned.takeIf { it.any(Char::isDigit) }
}

internal fun whatsappDigits(number: String): String = number.filter { it.isDigit() }

private fun Context.tryStart(intent: Intent): Boolean {
    return runCatching {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }
        .getOrElse { false }
}
