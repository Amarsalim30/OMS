package com.zeynbakers.order_management_system.core.helper.data

import java.math.BigDecimal

data class HelperNoteDetection(
    val type: HelperNoteType,
    val displayText: String,
    val detectedPhone: String? = null,
    val detectedPhoneDigits: String? = null,
    val detectedAmountRaw: String? = null,
    val detectedAmountNormalized: String? = null
)

object HelperNoteClassifier {
    private val phoneRegex =
        Regex("""(?:(?:\+?\d[\d\s-]{6,}\d))""")

    private val amountRegex =
        Regex("""(?i)\b\d{1,3}(?:[,\s]\d{3})+(?:\.\d+)?k?\b|\b\d+(?:\.\d+)?k?\b""")

    fun classifyVoiceTranscript(transcript: String): HelperNoteDetection {
        val trimmed = transcript.trim()
        val phone = extractPhone(trimmed)
        val amount = extractAmount(trimmed)
        val type =
            when {
                amount != null -> HelperNoteType.MONEY
                phone != null -> HelperNoteType.PHONE
                else -> HelperNoteType.VOICE
            }
        return HelperNoteDetection(
            type = type,
            displayText = trimmed,
            detectedPhone = phone?.first,
            detectedPhoneDigits = phone?.second,
            detectedAmountRaw = amount?.first,
            detectedAmountNormalized = amount?.second
        )
    }

    fun buildVoiceNote(
        transcript: String,
        sourceApp: String? = null,
        linkedCustomerId: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): HelperNoteEntity {
        val detection = classifyVoiceTranscript(transcript)
        return HelperNoteEntity(
            createdAt = nowMillis,
            updatedAt = nowMillis,
            type = detection.type,
            rawTranscript = transcript,
            displayText = detection.displayText,
            detectedPhone = detection.detectedPhone,
            detectedPhoneDigits = detection.detectedPhoneDigits,
            detectedAmountRaw = detection.detectedAmountRaw,
            detectedAmountNormalized = detection.detectedAmountNormalized,
            linkedCustomerId = linkedCustomerId,
            sourceApp = sourceApp
        )
    }

    fun buildCalculatorNote(
        expression: String,
        result: BigDecimal,
        sourceApp: String? = null,
        linkedCustomerId: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): HelperNoteEntity {
        val resultText = result.stripTrailingZeros().toPlainString()
        return HelperNoteEntity(
            createdAt = nowMillis,
            updatedAt = nowMillis,
            type = HelperNoteType.CALCULATOR,
            rawTranscript = expression,
            displayText = expression.trim(),
            calculatorExpression = expression.trim(),
            calculatorResult = resultText,
            detectedAmountRaw = resultText,
            detectedAmountNormalized = normalizeAmountForSearch(resultText),
            linkedCustomerId = linkedCustomerId,
            sourceApp = sourceApp
        )
    }

    fun normalizeAmountForSearch(raw: String): String? {
        return parseAmountRaw(raw)?.stripTrailingZeros()?.toPlainString()
    }

    fun normalizeDigits(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    private fun extractPhone(text: String): Pair<String, String>? {
        val match = phoneRegex.find(text)?.value?.trim() ?: return null
        val digits = normalizeDigits(match)
        if (digits.length < 7) return null
        return match to digits
    }

    private fun extractAmount(text: String): Pair<String, String>? {
        val match =
            amountRegex.findAll(text)
                .map { it.value.trim() }
                .firstOrNull { candidate ->
                    val digits = normalizeDigits(candidate)
                    candidate.contains(',') ||
                        candidate.contains('.') ||
                        candidate.endsWith("k", ignoreCase = true) ||
                        digits.length <= 6
                } ?: return null
        val normalized = normalizeAmountForSearch(match) ?: return null
        return match to normalized
    }

    private fun parseAmountRaw(raw: String): BigDecimal? {
        val compact = raw.trim().lowercase().replace(" ", "").replace(",", "")
        if (compact.isBlank()) return null
        val multiplier =
            if (compact.endsWith("k")) {
                BigDecimal("1000")
            } else {
                BigDecimal.ONE
            }
        val numericPart = compact.removeSuffix("k")
        val value = numericPart.toBigDecimalOrNull() ?: return null
        return value.multiply(multiplier)
    }
}
