package com.zeynbakers.order_management_system.core.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import java.math.BigDecimal

data class VoiceCalcAccess(
    val hasPermission: Boolean,
    val onRequestPermission: () -> Unit,
    val onApplyAmount: (BigDecimal) -> Unit
)

val LocalVoiceCalcAccess = staticCompositionLocalOf {
    VoiceCalcAccess(
        hasPermission = false,
        onRequestPermission = {},
        onApplyAmount = {}
    )
}

val LocalVoiceOverlaySuppressed = staticCompositionLocalOf<MutableState<Boolean>> {
    mutableStateOf(false)
}
