package com.zeynbakers.order_management_system.accounting.mpesa

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaParserTest {

    @Test
    fun `invalid calendar date keeps transaction but drops timestamp`() {
        val parsed =
            MpesaParser.parse(
                """
                QAB123XYZ9 Confirmed. You have received Ksh 1,200 from Alice on 32/13/26 at 10:30 AM.
                """.trimIndent()
            )

        assertEquals(1, parsed.size)
        assertEquals("QAB123XYZ9", parsed.single().transactionCode)
        assertDecimalEquals(BigDecimal("1200"), parsed.single().amount)
        assertNull(parsed.single().receivedAt)
    }

    @Test
    fun `invalid time keeps transaction but drops timestamp`() {
        val parsed =
            MpesaParser.parse(
                """
                QAC123XYZ8 Confirmed. You have received Ksh 980 from Bob on 05/03/26 at 25:61 PM.
                """.trimIndent()
            )

        assertEquals(1, parsed.size)
        assertEquals("QAC123XYZ8", parsed.single().transactionCode)
        assertDecimalEquals(BigDecimal("980"), parsed.single().amount)
        assertNull(parsed.single().receivedAt)
    }

    @Test
    fun `malformed date segment does not block other valid segments`() {
        val parsed =
            MpesaParser.parse(
                """
                QAD123XYZ7 Confirmed. You have received Ksh 1,100 from Carol on 32/03/26 at 10:30 AM.

                QAE123XYZ6 Confirmed. You have received Ksh 750 from Dan on 05/03/26 at 10:30 AM.
                """.trimIndent()
            )

        assertEquals(2, parsed.size)
        assertNull(parsed.first { it.transactionCode == "QAD123XYZ7" }.receivedAt)
        assertNotNull(parsed.first { it.transactionCode == "QAE123XYZ6" }.receivedAt)
    }

    private fun assertDecimalEquals(expected: BigDecimal, actual: BigDecimal) {
        assertEquals(0, expected.compareTo(actual))
    }
}
