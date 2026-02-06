package com.zeynbakers.order_management_system.core.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentDateStateTest {

    @Test
    fun `nextDateRefreshDelayMillis returns seconds until midnight in utc`() {
        val now = Instant.parse("2026-02-06T23:59:59Z").toEpochMilliseconds()
        val delay = nextDateRefreshDelayMillis(now, TimeZone.UTC)
        assertEquals(1_000L, delay)
    }

    @Test
    fun `nextDateRefreshDelayMillis handles midday correctly`() {
        val now = Instant.parse("2026-02-06T12:00:00Z").toEpochMilliseconds()
        val delay = nextDateRefreshDelayMillis(now, TimeZone.UTC)
        assertEquals(43_200_000L, delay)
    }

    @Test
    fun `nextDateRefreshDelayMillis is always positive`() {
        val now = Instant.parse("2026-02-06T00:00:00Z").toEpochMilliseconds()
        val delay = nextDateRefreshDelayMillis(now, TimeZone.UTC)
        assertTrue(delay > 0L)
    }
}
