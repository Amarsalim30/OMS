package com.zeynbakers.order_management_system.core.util

data class VoiceNotesParseResult(
    val formatted: String,
    val isStructured: Boolean
)

fun parseVoiceNotes(transcript: String): VoiceNotesParseResult? {
    val raw = transcript.trim()
    if (raw.isBlank()) return null

    val normalized =
        raw.lowercase()
            .replace(",", " ")
            .replace(Regex("\\bpieces\\b"), "pcs")
            .replace(Regex("\\bpiece\\b"), "pcs")
            .replace(Regex("\\bpc\\b"), "pcs")
            .replace(Regex("(?<=[a-z])(?=\\d)|(?<=\\d)(?=[a-z])"), " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    if (normalized.isBlank()) {
        return VoiceNotesParseResult(raw, false)
    }

    val tokens = normalized.split(" ")
    val items = mutableListOf<String>()
    val words = mutableListOf<String>()
    var structured = false
    var confident = true
    var i = 0

    fun flushItem(numberToken: String, hasPcs: Boolean) {
        val name = words.joinToString(" ")
        val suffix = if (hasPcs) " pcs" else ""
        items.add("$name $numberToken$suffix")
        words.clear()
        structured = true
    }

    while (i < tokens.size) {
        val token = tokens[i]
        if (token.isBlank() || token == "x") {
            i++
            continue
        }
        val isNumber = token.toBigDecimalOrNull() != null
        if (isNumber) {
            if (words.isEmpty()) {
                confident = false
                break
            }
            var hasPcs = false
            if (i + 1 < tokens.size) {
                val next = tokens[i + 1]
                if (next == "pcs" || next == "pc") {
                    hasPcs = true
                    i++
                }
            }
            flushItem(token, hasPcs)
        } else if (token == "pcs" || token == "pc") {
            // Ignore stray unit tokens.
        } else {
            words.add(token)
        }
        i++
    }

    if (!structured) {
        return VoiceNotesParseResult(raw, false)
    }
    if (words.isNotEmpty()) {
        confident = false
    }
    val formatted = items.joinToString(", ")
    return if (confident) {
        VoiceNotesParseResult(formatted, true)
    } else {
        VoiceNotesParseResult(raw, false)
    }
}
