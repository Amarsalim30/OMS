package com.zeynbakers.order_management_system.core.helper.ui

import com.zeynbakers.order_management_system.core.helper.data.HelperNoteEntity
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteWithCustomer
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesHistoryModelsTest {
    private val zone = ZoneId.of("UTC")
    private val now = 1_000_000_000_000L

    @Test
    fun `search matches text phone amount and customer name`() {
        val items =
            listOf(
                row(
                    id = 1,
                    displayText = "Pay supplier",
                    detectedPhoneDigits = "254712345678",
                    detectedAmountNormalized = "1500",
                    customerName = "Amina"
                )
            )

        assertEquals(1, filterHelperNotes(items, "supplier", NotesFilterState(), now, zone).size)
        assertEquals(1, filterHelperNotes(items, "123456", NotesFilterState(), now, zone).size)
        assertEquals(1, filterHelperNotes(items, "1.5k", NotesFilterState(), now, zone).size)
        assertEquals(1, filterHelperNotes(items, "amina", NotesFilterState(), now, zone).size)
    }

    @Test
    fun `pinned first sorts pinned notes ahead of newer unpinned notes`() {
        val olderPinned = row(id = 1, createdAt = now - 10_000, pinned = true)
        val newerUnpinned = row(id = 2, createdAt = now, pinned = false)
        val filtered =
            filterHelperNotes(
                notes = listOf(newerUnpinned, olderPinned),
                query = "",
                filter = NotesFilterState(pinnedFirst = true),
                nowMillis = now,
                zoneId = zone
            )

        assertEquals(1L, filtered.first().note.id)
    }

    @Test
    fun `today filter excludes older entries`() {
        val todayItem = row(id = 1, createdAt = now)
        val oldItem = row(id = 2, createdAt = now - (9L * 24L * 60L * 60L * 1000L))
        val filtered =
            filterHelperNotes(
                notes = listOf(todayItem, oldItem),
                query = "",
                filter = NotesFilterState(timeRange = NotesTimeRange.TODAY),
                nowMillis = now,
                zoneId = zone
            )

        assertEquals(1, filtered.size)
        assertTrue(filtered.any { it.note.id == 1L })
    }

    private fun row(
        id: Long,
        createdAt: Long = now,
        displayText: String = "sample",
        detectedPhoneDigits: String? = null,
        detectedAmountNormalized: String? = null,
        pinned: Boolean = false,
        customerName: String? = null
    ): HelperNoteWithCustomer {
        return HelperNoteWithCustomer(
            note =
                HelperNoteEntity(
                    id = id,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    type = HelperNoteType.VOICE,
                    rawTranscript = displayText,
                    displayText = displayText,
                    detectedPhoneDigits = detectedPhoneDigits,
                    detectedAmountNormalized = detectedAmountNormalized,
                    pinned = pinned
                ),
            customerName = customerName
        )
    }
}
