package com.zeynbakers.order_management_system.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateOf
import java.math.BigDecimal
import java.math.RoundingMode

class AmountFieldRegistry {
    private val lastSetter = mutableStateOf<((String) -> Unit)?>(null)

    fun update(setter: (String) -> Unit) {
        lastSetter.value = setter
    }

    fun applyAmount(amount: BigDecimal) {
        val formatted = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        lastSetter.value?.invoke(formatted)
    }
}

val LocalAmountFieldRegistry = staticCompositionLocalOf { AmountFieldRegistry() }
