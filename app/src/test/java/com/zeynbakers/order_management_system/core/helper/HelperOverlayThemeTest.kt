package com.zeynbakers.order_management_system.core.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HelperOverlayThemeTest {
    @Test
    fun fromWireValue_defaultsToBrand() {
        assertEquals(HelperThemePreset.Brand, HelperThemePreset.fromWireValue("missing"))
        assertEquals(HelperThemePreset.Brand, HelperThemePreset.fromWireValue(null))
    }

    @Test
    fun bubbleColorFor_usesStateSpecificColors() {
        val theme = HelperOverlayThemes.resolve(HelperThemePreset.Brand)

        assertNotEquals(
            theme.bubbleColorFor(HelperBubbleVisualState.Idle),
            theme.bubbleColorFor(HelperBubbleVisualState.Error)
        )
        assertNotEquals(
            theme.bubbleColorFor(HelperBubbleVisualState.Listening),
            theme.bubbleColorFor(HelperBubbleVisualState.Result)
        )
    }
}
