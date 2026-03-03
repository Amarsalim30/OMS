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
    fun `supports shorthand amounts and currency noise`() {
        assertValue("1.5k + 200", BigDecimal("1700"))
        assertValue("ksh 1200 plus 300", BigDecimal("1500"))
        assertValue("2m minus 500k", BigDecimal("1500000"))
    }

    @Test
    fun `supports percentage phrasing`() {
        assertValue("10 percent of 500", BigDecimal("50"))
        assertValue("100 + 10%", BigDecimal("110"))
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
        assertValue("7 \u00D7 8", BigDecimal("56"))
        assertValue("20 \u00F7 5", BigDecimal("4"))
    }
}

