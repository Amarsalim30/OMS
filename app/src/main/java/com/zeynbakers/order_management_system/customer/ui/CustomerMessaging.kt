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
        waCandidates.firstOrNull { context.canHandle(it) }?.let { intent ->
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
    }

    val encoded = Uri.encode(targetNumber)
    val smsCandidates =
        listOf(
            Intent(Intent.ACTION_SENDTO, "smsto:$encoded".toUri()),
            Intent(Intent.ACTION_SENDTO, "sms:$encoded".toUri())
        )
    smsCandidates.firstOrNull { context.canHandle(it) }?.let { intent ->
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

internal fun resolveMessagingNumber(rawPhone: String): String? {
    val normalized = normalizePhoneNumberE164(rawPhone)
    if (!normalized.isNullOrBlank()) return normalized
    val cleaned = normalizePhoneNumber(rawPhone)
    return cleaned.takeIf { it.any(Char::isDigit) }
}

internal fun whatsappDigits(number: String): String = number.filter { it.isDigit() }

private fun Context.canHandle(intent: Intent): Boolean {
    return intent.resolveActivity(packageManager) != null
}
