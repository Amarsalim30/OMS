package com.zeynbakers.order_management_system.accounting.mpesa

import com.zeynbakers.order_management_system.core.util.normalizePhoneNumber
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.min

data class MpesaParsedTransaction(
    val transactionCode: String?,
    val amount: BigDecimal,
    val senderName: String?,
    val senderPhone: String?,
    val receivedAt: Long?,
    val rawText: String
)

object MpesaParser {
    private val codeCandidateRegex = Regex("\\b[A-Z0-9]{10,12}\\b", RegexOption.IGNORE_CASE)
    private val amountRegex = Regex("(?i)(?:ksh|kes)\\.?\\s*([0-9,]+(?:\\.\\d{1,2})?)")
    private val primaryAmountRegex =
        Regex("(?i)\\b(?:received|paid|sent|withdrawn|deposit(?:ed)?|credited|transfer(?:red)?|reversal(?:led)?|bought)\\b[^0-9]{0,12}(?:ksh|kes)\\.?\\s*([0-9,]+(?:\\.\\d{1,2})?)")
    private val youHaveAmountRegex =
        Regex("(?i)\\byou have\\s+(?:received|paid|sent|withdrawn|deposited|credited|transferred|bought)\\s+(?:ksh|kes)\\.?\\s*([0-9,]+(?:\\.\\d{1,2})?)")
    private val amountKeywordRegex =
        Regex("(?i)\\b(received|paid|sent|withdrawn|deposit|deposited|credited|transfer|transferred|reversed|reversal|bought)\\b")
    private val confirmKeywordRegex = Regex("(?i)\\bconfirmed\\b")
    private val feeKeywordRegex = Regex("(?i)\\b(?:fee|charge|cost|transaction cost|withdrawal charge|airtime|bundle|interest)\\b")
    private val balanceKeywordRegex = Regex("(?i)\\bbalance\\b")
    private val phoneRegex = Regex("(?:\\+?254|0)\\d{9}")
    private val senderRegex = Regex("(?i)\\bfrom\\s+([A-Za-z0-9 .,'-]{2,})(?:\\s+(?:\\+?254|0)\\d{9})?")
    private val dateRegex = Regex("(?i)\\bon\\s+(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})")
    private val timeRegex = Regex("(?i)\\b(\\d{1,2}):(\\d{2})\\s*(AM|PM)?")

    fun parse(rawText: String): List<MpesaParsedTransaction> {
        val cleaned = rawText.replace("\r\n", "\n").trim()
        if (cleaned.isBlank()) return emptyList()
        val segments = splitIntoSegments(cleaned)
        val parsed =
            segments.mapNotNull { segment ->
                runCatching { parseSegment(segment) }.getOrNull()
            }
        val withCode = parsed.filter { it.transactionCode != null }.distinctBy { it.transactionCode }
        val withoutCode = parsed.filter { it.transactionCode == null }
        return withCode + withoutCode
    }

    private fun splitIntoSegments(text: String): List<String> {
        val matches =
            codeCandidateRegex.findAll(text)
                .filter { isValidCode(it.value) }
                .toList()
        if (matches.isEmpty()) {
            return text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        }
        val segments = mutableListOf<String>()
        for (index in matches.indices) {
            val start = matches[index].range.first
            val end = if (index + 1 < matches.size) matches[index + 1].range.first else text.length
            val segment = text.substring(start, end).trim()
            if (segment.isNotBlank()) {
                segments.add(segment)
            }
        }
        return segments
    }

    private fun parseSegment(segment: String): MpesaParsedTransaction? {
        val code = findTransactionCode(segment)
        val amount = parseAmount(segment, code) ?: return null
        val senderPhone = parsePhone(segment)
        val senderName = parseSender(segment)
        val receivedAt = parseDateTime(segment)
        return MpesaParsedTransaction(
            transactionCode = code,
            amount = amount,
            senderName = senderName,
            senderPhone = senderPhone,
            receivedAt = receivedAt,
            rawText = segment
        )
    }

    private fun parseAmount(text: String, transactionCode: String?): BigDecimal? {
        primaryAmountRegex.find(text)?.let { return parseAmountMatch(it) }
        youHaveAmountRegex.find(text)?.let { return parseAmountMatch(it) }
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null
        val codeIndex =
            transactionCode?.let { code ->
                text.indexOf(code, ignoreCase = true).takeIf { it >= 0 }
            }
        val scored =
            matches.mapIndexed { index, match ->
                val windowStart = (match.range.first - 30).coerceAtLeast(0)
                val windowEnd = (match.range.last + 30).coerceAtMost(text.length - 1)
                val window = text.substring(windowStart, windowEnd + 1)
                var score = 0
                if (amountKeywordRegex.containsMatchIn(window)) score += 2
                if (confirmKeywordRegex.containsMatchIn(window)) score += 2
                if (window.contains("you have", ignoreCase = true)) score += 2
                if (balanceKeywordRegex.containsMatchIn(window)) score -= 5
                if (feeKeywordRegex.containsMatchIn(window)) score -= 4
                if (codeIndex != null) {
                    val distance = match.range.first - codeIndex
                    if (distance in 0..80) score += 2
                }
                Triple(index, score, match)
            }
        val best =
            scored.maxWithOrNull(compareBy<Triple<Int, Int, MatchResult>>({ it.second }, { -it.first }))
                ?.third
                ?: matches.first()
        return parseAmountMatch(best)
    }

    private fun parseAmountMatch(match: MatchResult): BigDecimal? {
        val raw = match.groupValues[1].replace(",", "")
        return runCatching { BigDecimal(raw) }.getOrNull()
    }

    private fun parsePhone(text: String): String? {
        val match = phoneRegex.find(text) ?: return null
        return normalizePhoneNumber(match.value)
    }

    private fun parseSender(text: String): String? {
        val match = senderRegex.find(text) ?: return null
        return match.groupValues[1].trim().ifBlank { null }
    }

    private fun findTransactionCode(text: String): String? {
        val match =
            codeCandidateRegex.findAll(text)
                .map { it.value }
                .firstOrNull { isValidCode(it) }
        return match?.uppercase()
    }

    private fun isValidCode(value: String): Boolean {
        if (value.isBlank()) return false
        val trimmed = value.trim()
        if (trimmed.isEmpty() || !trimmed.first().isLetter()) return false
        var hasLetter = false
        var hasDigit = false
        trimmed.forEach { ch ->
            if (ch.isLetter()) hasLetter = true
            if (ch.isDigit()) hasDigit = true
        }
        return hasLetter && hasDigit
    }

    private fun parseDateTime(text: String): Long? {
        val dateMatch = dateRegex.find(text) ?: return null
        val day = dateMatch.groupValues[1].toIntOrNull() ?: return null
        val month = dateMatch.groupValues[2].toIntOrNull() ?: return null
        val yearRaw = dateMatch.groupValues[3].toIntOrNull() ?: return null
        if (day !in 1..31 || month !in 1..12) return null
        val year = if (yearRaw < 100) 2000 + min(yearRaw, 99) else yearRaw
        val timeMatch = timeRegex.find(text)
        val (hour, minute) = if (timeMatch != null) {
            val hourRaw = timeMatch.groupValues[1].toIntOrNull() ?: 0
            val minuteRaw = timeMatch.groupValues[2].toIntOrNull() ?: 0
            if (minuteRaw !in 0..59) return null
            val ampm = timeMatch.groupValues[3].uppercase()
            val adjustedHour =
                when (ampm) {
                    "AM" -> {
                        if (hourRaw !in 1..12) return null
                        if (hourRaw == 12) 0 else hourRaw
                    }
                    "PM" -> {
                        if (hourRaw !in 1..12) return null
                        if (hourRaw < 12) hourRaw + 12 else hourRaw
                    }
                    else -> {
                        if (hourRaw !in 0..23) return null
                        hourRaw
                    }
                }
            adjustedHour to minuteRaw
        } else {
            12 to 0
        }
        val dateTime =
            runCatching { LocalDateTime.of(year, month, day, hour, minute) }
                .getOrNull()
                ?: return null
        return runCatching {
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }
}
