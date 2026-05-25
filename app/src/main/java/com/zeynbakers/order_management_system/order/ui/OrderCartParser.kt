package com.zeynbakers.order_management_system.order.ui

import com.zeynbakers.order_management_system.product.data.ProductEntity
import java.math.BigDecimal
import java.math.RoundingMode

data class CartItem(
    val emoji: String,
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal = BigDecimal.ZERO
) {
    val lineTotal: BigDecimal
        get() = unitPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
}

object OrderCartParser {
    private val leadingBulletRegex = Regex("^\\s*[-•*]+\\s*")

    fun parseNotesToCart(notes: String): List<CartItem> {
        if (notes.isBlank()) return emptyList()
        return notes
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseLine(line) }
    }

    fun serializeCartToNotes(items: List<CartItem>): String {
        return items.joinToString("\n") { item ->
            val emojiPrefix = if (item.emoji.isNotBlank()) "${item.emoji} " else ""
            val priceSuffix =
                if (item.unitPrice > BigDecimal.ZERO) {
                    " @ ${item.unitPrice.stripTrailingZeros().toPlainString()}"
                } else {
                    ""
                }
            "$emojiPrefix${item.name} x ${item.quantity}$priceSuffix"
        }
    }

    fun cartTotal(items: List<CartItem>): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTotal) }

    fun suggestProducts(query: String, catalog: List<ProductEntity>): List<ProductEntity> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.trim().lowercase()
        return catalog.filter { it.name.lowercase().contains(lowerQuery) }.take(6)
    }

    private fun parseLine(line: String): CartItem? {
        val cleaned = line.replace(leadingBulletRegex, "").trim()
        if (cleaned.isBlank()) return null

        val unitPrice =
            cleaned
                .substringAfter(" @ ", "")
                .trim()
                .takeIf { cleaned.contains(" @ ") }
                ?.let { parsePrice(it) }
                ?: BigDecimal.ZERO
        val mainPart =
            if (cleaned.contains(" @ ")) {
                cleaned.substringBefore(" @ ").trim()
            } else {
                cleaned
            }
        val quantityIndex = mainPart.lastIndexOf(" x ")
        if (quantityIndex < 0) {
            return CartItem(emoji = "", name = mainPart, quantity = 1, unitPrice = unitPrice)
        }
        val left = mainPart.substring(0, quantityIndex).trim()
        val quantity = mainPart.substring(quantityIndex + 3).trim().toIntOrNull() ?: return null
        if (quantity <= 0) return null
        val (emoji, name) = splitEmojiPrefix(left)
        if (name.isBlank()) return null
        return CartItem(emoji = emoji, name = name, quantity = quantity, unitPrice = unitPrice)
    }

    private fun splitEmojiPrefix(left: String): Pair<String, String> {
        if (left.isBlank()) return "" to ""
        val firstSpace = left.indexOf(' ')
        if (firstSpace <= 0) return "" to left
        val prefix = left.substring(0, firstSpace).trim()
        val name = left.substring(firstSpace + 1).trim()
        val prefixIsEmoji = prefix.isNotEmpty() && prefix.none { it.isLetterOrDigit() }
        return if (prefixIsEmoji && name.isNotBlank()) {
            prefix to name
        } else {
            "" to left
        }
    }

    private fun parsePrice(raw: String): BigDecimal? =
        runCatching {
            BigDecimal(raw.trim().replace(',', '.')).setScale(2, RoundingMode.HALF_UP)
        }.getOrNull()
}
