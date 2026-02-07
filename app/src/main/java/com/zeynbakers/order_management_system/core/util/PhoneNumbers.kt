package com.zeynbakers.order_management_system.core.util

fun normalizePhoneNumber(raw: String): String {
    return raw.filter { it.isDigit() || it == '+' }
}

fun normalizePhoneNumberE164(raw: String, defaultCountryCode: String = "+254"): String? {
    val cleaned = normalizePhoneNumber(raw).trim()
    if (cleaned.isBlank()) return null
    val digitsOnly = cleaned.filter { it.isDigit() }
    if (digitsOnly.isBlank()) return null
    val defaultDigits = defaultCountryCode.filter { it.isDigit() }
    if (defaultDigits.isBlank()) return null

    fun normalizeDefaultCountry(digits: String): String {
        if (!digits.startsWith(defaultDigits)) return digits
        val rest = digits.drop(defaultDigits.length)
        val withoutLeadingZero = rest.dropWhile { it == '0' }
        val last = if (withoutLeadingZero.length >= 9) withoutLeadingZero.takeLast(9) else withoutLeadingZero
        return defaultDigits + last
    }

    val normalizedDigits =
        when {
            cleaned.startsWith("+") -> normalizeDefaultCountry(digitsOnly)
            digitsOnly.startsWith("00") -> normalizeDefaultCountry(digitsOnly.drop(2))
            digitsOnly.startsWith(defaultDigits) -> normalizeDefaultCountry(digitsOnly)
            else -> {
                val local = digitsOnly.dropWhile { it == '0' }
                if (local.isBlank()) return null
                val last = if (local.length >= 9) local.takeLast(9) else local
                defaultDigits + last
            }
        }
    return "+$normalizedDigits"
}

fun expandPhoneCandidates(raw: String): List<String> {
    val normalized = normalizePhoneNumber(raw)
    if (normalized.isBlank()) return emptyList()
    val digits = normalized.filter { it.isDigit() }
    if (digits.isBlank()) return listOf(normalized)
    val last9 = if (digits.length >= 9) digits.takeLast(9) else digits
    val candidates = linkedSetOf<String>()
    candidates.add(normalized)
    normalizePhoneNumberE164(raw)?.let { candidates.add(it) }
    if (normalized.startsWith("+254")) {
        candidates.add("0$last9")
    }
    if (normalized.startsWith("0")) {
        candidates.add("+254$last9")
    }
    if (digits.length == 9) {
        candidates.add("0$digits")
        candidates.add("+254$digits")
    }
    candidates.add("254$last9")
    return candidates.toList()
}
