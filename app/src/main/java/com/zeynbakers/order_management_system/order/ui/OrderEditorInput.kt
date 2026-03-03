package com.zeynbakers.order_management_system.order.ui

private val chatHeaderBracketRegex = Regex("^\\[[^\\]]{3,80}]\\s*[^:\\n]{1,120}:\\s*")
private val chatHeaderDashRegex =
    Regex("^\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?,\\s*\\d{1,2}:\\d{2}\\s*(?:am|pm)?\\s*-\\s*[^:\\n]{1,120}:\\s*", RegexOption.IGNORE_CASE)
private val chatExportLineRegex = Regex("(?m)^\\s*\\[[^\\]]{3,80}]\\s*[^:\\n]{1,120}:\\s*")
private val whitespaceNoiseRegex = Regex("[\\t\\u00A0\\u202F]+")

internal fun sanitizeAmountInput(raw: String): String {
    val builder = StringBuilder(raw.length)
    var hasDot = false
    raw.forEach { ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            ch == '.' && !hasDot -> {
                hasDot = true
                builder.append(ch)
            }
        }
    }
    return builder.toString()
}

internal fun sanitizePickupTimeInput(raw: String): String {
    val filtered = raw.filter { ch -> ch.isDigit() || ch == ':' || ch == '.' }
    return filtered.take(5)
}

internal fun sanitizeOrderNotesInput(raw: String): String {
    val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
    if (!chatExportLineRegex.containsMatchIn(normalized)) {
        return normalized
    }

    val deduped = linkedSetOf<String>()
    normalized
        .lineSequence()
        .map(::stripChatHeaders)
        .map { line -> line.replace(whitespaceNoiseRegex, " ").trim() }
        .filter { it.isNotBlank() }
        .forEach { line -> deduped.add(line) }

    return deduped.joinToString(separator = "\n")
}

private fun stripChatHeaders(rawLine: String): String {
    var line = rawLine.trim()
    repeat(6) {
        val next =
            when {
                chatHeaderBracketRegex.containsMatchIn(line) ->
                    line.replaceFirst(chatHeaderBracketRegex, "")
                chatHeaderDashRegex.containsMatchIn(line) ->
                    line.replaceFirst(chatHeaderDashRegex, "")
                else -> line
            }.trimStart()
        if (next == line) return line
        line = next
    }
    return line
}
