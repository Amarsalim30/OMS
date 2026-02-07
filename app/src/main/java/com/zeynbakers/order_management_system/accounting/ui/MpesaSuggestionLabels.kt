package com.zeynbakers.order_management_system.accounting.ui

import com.zeynbakers.order_management_system.core.util.formatOrderLabel

fun suggestionLabel(suggestion: MpesaOrderSuggestion, maxNotes: Int): String {
    return formatOrderLabel(
        date = suggestion.orderDate,
        customerName = suggestion.customerName,
        notes = suggestion.notes,
        totalAmount = null,
        maxNotes = maxNotes
    )
}
