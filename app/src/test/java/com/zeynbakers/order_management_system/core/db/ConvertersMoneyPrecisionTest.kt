package com.zeynbakers.order_management_system.core.db

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersMoneyPrecisionTest {

    private val converters = Converters()

    @Test
    fun `fromBigDecimal stores cents as integer string`() {
        assertEquals("1500", converters.fromBigDecimal(BigDecimal("15")))
        assertEquals("1505", converters.fromBigDecimal(BigDecimal("15.05")))
        assertEquals("-250", converters.fromBigDecimal(BigDecimal("-2.50")))
    }

    @Test
    fun `toBigDecimal restores two-decimal money value`() {
        assertEquals(BigDecimal("15.00"), converters.toBigDecimal("1500"))
        assertEquals(BigDecimal("15.05"), converters.toBigDecimal("1505"))
        assertEquals(BigDecimal("-2.50"), converters.toBigDecimal("-250"))
    }
}
