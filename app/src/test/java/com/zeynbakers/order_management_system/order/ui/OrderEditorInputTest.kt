package com.zeynbakers.order_management_system.order.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderEditorInputTest {

    @Test
    fun `sanitizeAmountInput keeps first decimal point and strips other chars`() {
        assertEquals("12.34", sanitizeAmountInput("12.34"))
        assertEquals("12.34", sanitizeAmountInput("12..3a4"))
        assertEquals("12", sanitizeAmountInput("a1b2"))
        assertEquals(".5", sanitizeAmountInput(".5"))
    }

    @Test
    fun `sanitizePickupTimeInput keeps time chars and limits length`() {
        assertEquals("09:30", sanitizePickupTimeInput("09:30"))
        assertEquals("9.30", sanitizePickupTimeInput("9.30pm"))
        assertEquals("12345", sanitizePickupTimeInput("123456"))
    }

    @Test
    fun `sanitizeOrderNotesInput strips chat metadata and duplicate lines`() {
        val raw =
            """
            [3:24 am, 22/02/2026] Amar Salim: Meatpie 100 ,kanzu 100
            [3:24 am, 22/02/2026] Amar Salim: Mess 100
            [3:27 am, 22/02/2026] Amar Salim: [22/02, 3:24 am] Amar Salim: Meatpie 100 ,kanzu 100
            [22/02, 3:24 am] Amar Salim: Mess 100
            [3:30 am, 22/02/2026] Amar Salim: [05/02, 3:14 pm] Mohammed Muftah: Hata sija test
            [05/02, 3:14 pm] Mohammed Muftah: Lakini nime badilisha maneno kiasi
            """.trimIndent()

        val cleaned = sanitizeOrderNotesInput(raw)

        assertEquals(
            """
            Meatpie 100 ,kanzu 100
            Mess 100
            Hata sija test
            Lakini nime badilisha maneno kiasi
            """.trimIndent(),
            cleaned
        )
    }

    @Test
    fun `sanitizeOrderNotesInput keeps normal notes unchanged`() {
        val raw = "Meatpie 3\nKanzu 2\nExtra chilli"
        assertEquals(raw, sanitizeOrderNotesInput(raw))
    }

    @Test
    fun `extractCustomerQueryFromNotes ignores amounts and only reads trailing name words`() {
        assertEquals("", extractCustomerQueryFromNotes("Keki 2 1000"))
        assertEquals("Asha", extractCustomerQueryFromNotes("Keki 2 1000 Asha"))
        assertEquals("Asha Salim", extractCustomerQueryFromNotes("Keki 2 1000 Asha Salim"))
        assertEquals("Asha", extractCustomerQueryFromNotes("Keki 2\nAsha"))
    }

    @Test
    fun `stripTrailingCustomerQueryFromNotes removes only trailing customer suffix`() {
        assertEquals("Keki 2 1000", stripTrailingCustomerQueryFromNotes("Keki 2 1000 Asha"))
        assertEquals("Keki 2 1000", stripTrailingCustomerQueryFromNotes("Keki 2 1000"))
        assertEquals("Keki 2", stripTrailingCustomerQueryFromNotes("Keki 2\nAsha Salim"))
    }
}
