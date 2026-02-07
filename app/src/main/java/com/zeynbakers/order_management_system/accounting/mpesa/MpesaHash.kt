package com.zeynbakers.order_management_system.accounting.mpesa

import java.math.BigDecimal
import java.security.MessageDigest

fun computeMpesaHash(
    amount: BigDecimal,
    receivedAt: Long,
    senderPhone: String?,
    transactionCode: String?,
    rawText: String?
): String {
    val last4 =
        senderPhone?.filter { it.isDigit() }?.takeLast(4).orEmpty()
    val normalizedText =
        rawText?.trim()?.replace(Regex("\\s+"), " ")?.lowercase().orEmpty()
    val base =
        buildString {
            append(amount.toPlainString())
            append("|")
            append(receivedAt)
            append("|")
            append(last4)
            append("|")
            append(transactionCode ?: "nocode")
        }
    val input =
        if (transactionCode == null) {
            "$base|$normalizedText"
        } else {
            base
        }
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
