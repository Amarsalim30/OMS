package com.zeynbakers.order_management_system.core.util

import kotlinx.datetime.LocalTime

fun normalizePickupTime(input: String): String? {
    val parts = parseTimeParts(input) ?: return null
    val hour = parts.first.toString().padStart(2, '0')
    val minute = parts.second.toString().padStart(2, '0')
    return "$hour:$minute"
}

fun parsePickupTime(input: String?): LocalTime? {
    val raw = input ?: return null
    val parts = parseTimeParts(raw) ?: return null
    return LocalTime(parts.first, parts.second)
}

private fun parseTimeParts(raw: String): Pair<Int, Int>? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val normalized = trimmed.replace('.', ':')
    val segments = normalized.split(":")
    val (hour, minute) =
        when (segments.size) {
            1 -> {
                val digits = segments[0].filter { it.isDigit() }
                when (digits.length) {
                    1, 2 -> digits.toIntOrNull() to 0
                    3 -> digits.substring(0, 1).toIntOrNull() to digits.substring(1, 3).toIntOrNull()
                    4 -> digits.substring(0, 2).toIntOrNull() to digits.substring(2, 4).toIntOrNull()
                    else -> return null
                }
            }
            2 -> segments[0].toIntOrNull() to segments[1].toIntOrNull()
            else -> return null
        }
    if (hour == null || minute == null) return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return Pair(hour, minute)
}
