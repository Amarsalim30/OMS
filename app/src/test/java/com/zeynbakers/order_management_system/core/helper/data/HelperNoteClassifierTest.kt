package com.zeynbakers.order_management_system.core.helper.data

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HelperNoteClassifierTest {
    @Test
    fun `classify voice transcript detects phone and amount`() {
        val detection = HelperNoteClassifier.classifyVoiceTranscript("Send 1,500 to +254 712 345 678")

        assertEquals(HelperNoteType.MONEY, detection.type)
        assertEquals("+254 712 345 678", detection.detectedPhone)
        assertEquals("254712345678", detection.detectedPhoneDigits)
        assertEquals("1,500", detection.detectedAmountRaw)
        assertEquals("1500", detection.detectedAmountNormalized)
    }

    @Test
    fun `build calculator note keeps expression and result`() {
        val note =
            HelperNoteClassifier.buildCalculatorNote(
                expression = "12500 + 3200 - 700",
                result = BigDecimal("15000")
            )

        assertEquals(HelperNoteType.CALCULATOR, note.type)
        assertEquals("12500 + 3200 - 700", note.calculatorExpression)
        assertEquals("15000", note.calculatorResult)
        assertNotNull(note.detectedAmountNormalized)
    }
}
