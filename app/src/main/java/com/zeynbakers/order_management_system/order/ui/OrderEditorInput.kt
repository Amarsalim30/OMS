package com.zeynbakers.order_management_system.order.ui

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
