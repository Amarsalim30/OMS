package com.zeynbakers.order_management_system.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNotesParserTest {

    @Test
    fun `formats item notes with quantities`() {
        val result = parseVoiceNotes("meatpie 30 swissroll 40 kabab 40pcs")
        assertNotNull(result)
        assertTrue(result!!.isStructured)
        assertEquals("meatpie 30, swissroll 40, kabab 40 pcs", result.formatted)
    }

    @Test
    fun `falls back to raw transcript on low confidence`() {
        val result = parseVoiceNotes("meatpie 30 today")
        assertNotNull(result)
        assertFalse(result!!.isStructured)
        assertEquals("meatpie 30 today", result.formatted)
    }
}
