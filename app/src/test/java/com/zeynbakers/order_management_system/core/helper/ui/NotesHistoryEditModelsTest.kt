package com.zeynbakers.order_management_system.core.helper.ui

import com.zeynbakers.order_management_system.core.helper.data.HelperNoteEntity
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotesHistoryEditModelsTest {
    @Test
    fun buildEditedHelperNote_blank_returnsNull() {
        val existing = baseNote(type = HelperNoteType.VOICE)
        val updated = buildEditedHelperNote(existing, "   ")
        assertNull(updated)
    }

    @Test
    fun buildEditedHelperNote_voice_reclassifiesPhone() {
        val existing = baseNote(type = HelperNoteType.VOICE)

        val updated = buildEditedHelperNote(existing, "Call +254 712 345 678 now", nowMillis = 20L)

        requireNotNull(updated)
        assertEquals(20L, updated.updatedAt)
        assertEquals(HelperNoteType.PHONE, updated.type)
        assertEquals("254712345678", updated.detectedPhoneDigits)
        assertNull(updated.calculatorResult)
    }

    @Test
    fun buildEditedHelperNote_calculator_recalculatesWhenValid() {
        val existing = baseNote(type = HelperNoteType.CALCULATOR).copy(
            calculatorExpression = "1+1",
            calculatorResult = "2"
        )

        val updated = buildEditedHelperNote(existing, "1200 + 300", nowMillis = 30L)

        requireNotNull(updated)
        assertEquals(30L, updated.updatedAt)
        assertEquals(HelperNoteType.CALCULATOR, updated.type)
        assertEquals("1200 + 300", updated.calculatorExpression)
        assertEquals("1500", updated.calculatorResult)
        assertEquals("1500", updated.detectedAmountNormalized)
    }

    @Test
    fun buildEditedHelperNote_calculator_invalid_becomesVoiceLike() {
        val existing = baseNote(type = HelperNoteType.CALCULATOR).copy(
            calculatorExpression = "1+1",
            calculatorResult = "2"
        )

        val updated = buildEditedHelperNote(existing, "remind me to call supplier", nowMillis = 40L)

        requireNotNull(updated)
        assertEquals(40L, updated.updatedAt)
        assertEquals(HelperNoteType.VOICE, updated.type)
        assertNull(updated.calculatorExpression)
        assertNull(updated.calculatorResult)
    }

    private fun baseNote(type: HelperNoteType): HelperNoteEntity {
        return HelperNoteEntity(
            id = 1L,
            createdAt = 10L,
            updatedAt = 10L,
            type = type,
            rawTranscript = "hello",
            displayText = "hello"
        )
    }
}
