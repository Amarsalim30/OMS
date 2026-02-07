package com.zeynbakers.order_management_system.order.domain

import java.math.BigDecimal

data class OrderLineItem(
    val name: String,
    val quantity: BigDecimal
)

data class ParsedOrderNotes(
    val items: List<OrderLineItem>,
    val unparsed: List<String>
)

private val splitterRegex = Regex("[\\n,;]+")
private val leadingBulletRegex = Regex("^\\s*[-•*]+\\s*")
private val quantityRegex = Regex("\\d+(?:[\\.,]\\d+)?")

fun parseOrderNotes(notes: String): ParsedOrderNotes {
    val segments =
        notes.replace("\r\n", "\n")
            .split(splitterRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }

    if (segments.isEmpty()) return ParsedOrderNotes(items = emptyList(), unparsed = emptyList())

    val parsed = mutableListOf<OrderLineItem>()
    val unparsed = mutableListOf<String>()

    for (segment in segments) {
        val cleaned = segment.replace(leadingBulletRegex, "").trim()
        if (cleaned.isBlank()) continue

        val lowered = cleaned.lowercase()
        if (lowered.contains("kes") || lowered.contains("ksh") || lowered.contains("total")) {
            unparsed += cleaned
            continue
        }

        val qtyMatch = quantityRegex.find(cleaned)
        if (qtyMatch == null) {
            unparsed += cleaned
            continue
        }

        val quantity = parseQuantity(qtyMatch.value)
        if (quantity == null || quantity <= BigDecimal.ZERO) {
            unparsed += cleaned
            continue
        }

        val name = extractName(cleaned, qtyMatch.range)
        if (name.isBlank()) {
            unparsed += cleaned
            continue
        }

        parsed += OrderLineItem(name = name, quantity = quantity)
    }

    return ParsedOrderNotes(items = parsed, unparsed = unparsed)
}

fun aggregateLineItems(notesList: List<String>): List<OrderLineItem> {
    val items = notesList.flatMap { parseOrderNotes(it).items }
    return aggregateOrderLineItems(items)
}

fun aggregateOrderLineItems(items: List<OrderLineItem>): List<OrderLineItem> {
    if (items.isEmpty()) return emptyList()

    val displayNameByKey = linkedMapOf<String, String>()
    val totalsByKey = linkedMapOf<String, BigDecimal>()

    items.forEach { item ->
        val key = normalizeKey(item.name)
        displayNameByKey.putIfAbsent(key, item.name.trim())
        totalsByKey[key] = (totalsByKey[key] ?: BigDecimal.ZERO) + item.quantity
    }

    return totalsByKey.entries
        .sortedWith(compareBy({ displayNameByKey[it.key].orEmpty().lowercase() }, { it.key }))
        .map { (key, qty) ->
            OrderLineItem(name = displayNameByKey[key].orEmpty(), quantity = qty)
        }
}

fun formatQuantity(quantity: BigDecimal): String =
    quantity.stripTrailingZeros().toPlainString()

private fun parseQuantity(raw: String): BigDecimal? =
    runCatching {
        val normalized = raw.trim().replace(',', '.')
        BigDecimal(normalized)
    }.getOrNull()

private fun extractName(text: String, qtyRange: IntRange): String {
    val before = text.substring(0, qtyRange.first).trim().trimEnd(':', '-', 'x', 'X', '×', '*', '@')
    val after = text.substring(qtyRange.last + 1).trim().trimStart(':', '-', 'x', 'X', '×', '*', '@')

    val nameCandidate =
        when {
            before.isNotBlank() && after.isBlank() -> before
            before.isBlank() && after.isNotBlank() -> after
            before.isNotBlank() && after.isNotBlank() -> "$before $after"
            else -> ""
        }

    return nameCandidate
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimStart(':', '-', 'x', 'X', '×', '*', '@')
        .trimEnd(':', '-', 'x', 'X', '×', '*', '@')
        .trim()
}

private fun normalizeKey(name: String): String =
    name.trim()
        .lowercase()
        .replace(Regex("[\\s\\u00A0]+"), " ")
        .replace(Regex("[,.;]+$"), "")
        .trim()
