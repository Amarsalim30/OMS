package com.zeynbakers.order_management_system.core.helper.ui

import com.zeynbakers.order_management_system.core.helper.data.HelperNoteClassifier
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteEntity
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteWithCustomer
import com.zeynbakers.order_management_system.core.util.parseVoiceMath
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class NotesTimeRange {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM
}

data class NotesFilterState(
    val types: Set<HelperNoteType> = emptySet(),
    val timeRange: NotesTimeRange = NotesTimeRange.ALL,
    val customFrom: String = "",
    val customTo: String = "",
    val pinnedFirst: Boolean = false,
    val hasPhone: Boolean = false,
    val hasAmount: Boolean = false
)

data class NotesHistoryUiState(
    val items: List<HelperNoteWithCustomer> = emptyList(),
    val query: String = "",
    val filter: NotesFilterState = NotesFilterState()
)

internal fun filterHelperNotes(
    notes: List<HelperNoteWithCustomer>,
    query: String,
    filter: NotesFilterState,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<HelperNoteWithCustomer> {
    val normalizedQuery = query.trim()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val fromDate = parseDateOrNull(filter.customFrom)
    val toDate = parseDateOrNull(filter.customTo)

    val filtered =
        notes.filter { row ->
            val note = row.note
            val noteDate = Instant.ofEpochMilli(note.createdAt).atZone(zoneId).toLocalDate()
            val matchesType = filter.types.isEmpty() || filter.types.contains(note.type)
            val matchesPhone = !filter.hasPhone || !note.detectedPhoneDigits.isNullOrBlank()
            val matchesAmount = !filter.hasAmount || !note.detectedAmountNormalized.isNullOrBlank()
            val matchesTime =
                when (filter.timeRange) {
                    NotesTimeRange.ALL -> true
                    NotesTimeRange.TODAY -> noteDate == today
                    NotesTimeRange.LAST_7_DAYS -> noteDate >= today.minusDays(6)
                    NotesTimeRange.LAST_30_DAYS -> noteDate >= today.minusDays(29)
                    NotesTimeRange.CUSTOM -> {
                        val lowerOk = fromDate == null || noteDate >= fromDate
                        val upperOk = toDate == null || noteDate <= toDate
                        lowerOk && upperOk
                    }
                }
            val matchesQuery = matchesHelperNoteQuery(row, normalizedQuery)
            matchesType && matchesPhone && matchesAmount && matchesTime && matchesQuery
        }

    return if (filter.pinnedFirst) {
        filtered.sortedWith(
            compareByDescending<HelperNoteWithCustomer> { it.note.pinned }
                .thenByDescending { it.note.createdAt }
                .thenByDescending { it.note.id }
        )
    } else {
        filtered.sortedWith(
            compareByDescending<HelperNoteWithCustomer> { it.note.createdAt }
                .thenByDescending { it.note.id }
        )
    }
}

internal fun matchesHelperNoteQuery(row: HelperNoteWithCustomer, query: String): Boolean {
    if (query.isBlank()) return true
    val note = row.note
    val loweredQuery = query.lowercase()
    val digitsQuery = HelperNoteClassifier.normalizeDigits(query)
    val amountQuery = HelperNoteClassifier.normalizeAmountForSearch(query)

    val textFields =
        listOf(
            note.displayText,
            note.rawTranscript,
            note.calculatorExpression,
            note.calculatorResult,
            note.detectedPhone,
            note.detectedAmountRaw,
            note.sourceApp,
            row.customerName
        )

    if (textFields.any { field -> field?.lowercase()?.contains(loweredQuery) == true }) {
        return true
    }
    if (digitsQuery.length >= 2 && note.detectedPhoneDigits?.contains(digitsQuery) == true) {
        return true
    }
    if (!amountQuery.isNullOrBlank()) {
        if (note.detectedAmountNormalized?.contains(amountQuery) == true) return true
        if (note.calculatorResult?.contains(amountQuery) == true) return true
    }
    return false
}

private fun parseDateOrNull(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isBlank()) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

internal fun buildEditedHelperNote(
    existing: HelperNoteEntity,
    editedText: String,
    nowMillis: Long = System.currentTimeMillis()
): HelperNoteEntity? {
    val trimmed = editedText.trim()
    if (trimmed.isBlank()) return null

    if (existing.type == HelperNoteType.CALCULATOR) {
        val parsed = parseVoiceMath(trimmed)
        if (parsed != null) {
            val resultText = parsed.value.stripTrailingZeros().toPlainString()
            return existing.copy(
                updatedAt = nowMillis,
                type = HelperNoteType.CALCULATOR,
                rawTranscript = trimmed,
                displayText = trimmed,
                calculatorExpression = trimmed,
                calculatorResult = resultText,
                detectedPhone = null,
                detectedPhoneDigits = null,
                detectedAmountRaw = resultText,
                detectedAmountNormalized = HelperNoteClassifier.normalizeAmountForSearch(resultText)
            )
        }
    }

    val detection = HelperNoteClassifier.classifyVoiceTranscript(trimmed)
    return existing.copy(
        updatedAt = nowMillis,
        type = detection.type,
        rawTranscript = trimmed,
        displayText = detection.displayText,
        calculatorExpression = null,
        calculatorResult = null,
        detectedPhone = detection.detectedPhone,
        detectedPhoneDigits = detection.detectedPhoneDigits,
        detectedAmountRaw = detection.detectedAmountRaw,
        detectedAmountNormalized = detection.detectedAmountNormalized
    )
}
