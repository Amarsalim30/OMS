package com.zeynbakers.order_management_system.core.util

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceMathParserTest {

    private fun assertValue(input: String, expected: BigDecimal) {
        val result = parseVoiceMath(input)
        assertNotNull("Expected parse result for \"$input\"", result)
        assertEquals(0, result!!.value.compareTo(expected))
    }

    @Test
    fun `handles subtraction operators and phrases`() {
        assertValue("1000-800", BigDecimal("200"))
        assertValue("1000 minus 800", BigDecimal("200"))
        assertValue("take 800 from 1000", BigDecimal("200"))
        assertValue("1,000 - 800", BigDecimal("200"))
    }

    @Test
    fun `rejects consecutive numbers without an operator`() {
        assertNull(parseVoiceMath("1000 800"))
    }

    @Test
    fun `supports unary minus`() {
        assertValue("minus 200", BigDecimal("-200"))
    }

    @Test
    fun `rejects division by zero`() {
        assertNull(parseVoiceMath("10 / 0"))
    }

    @Test
    fun `supports unicode multiply and divide symbols`() {
        assertValue("7 × 8", BigDecimal("56"))
        assertValue("20 ÷ 5", BigDecimal("4"))
    }
}
