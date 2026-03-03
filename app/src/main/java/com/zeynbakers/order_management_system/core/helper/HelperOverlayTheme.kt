package com.zeynbakers.order_management_system.core.helper

enum class HelperThemePreset(val wireValue: String) {
    Brand("brand"),
    Neutral("neutral");

    companion object {
        fun fromWireValue(value: String?): HelperThemePreset {
            return entries.firstOrNull { it.wireValue == value } ?: Brand
        }
    }
}

enum class HelperBubbleVisualState {
    Idle,
    Listening,
    Result,
    Error
}

data class HelperOverlayTheme(
    val bubbleIdleColor: Int,
    val bubbleListeningColor: Int,
    val bubbleResultColor: Int,
    val bubbleErrorColor: Int,
    val cardBackgroundColor: Int,
    val cardBorderColor: Int,
    val titleColor: Int,
    val bodyColor: Int,
    val primaryButtonColor: Int,
    val primaryButtonTextColor: Int,
    val secondaryButtonColor: Int,
    val secondaryButtonTextColor: Int
) {
    fun bubbleColorFor(state: HelperBubbleVisualState): Int {
        return when (state) {
            HelperBubbleVisualState.Idle -> bubbleIdleColor
            HelperBubbleVisualState.Listening -> bubbleListeningColor
            HelperBubbleVisualState.Result -> bubbleResultColor
            HelperBubbleVisualState.Error -> bubbleErrorColor
        }
    }
}

object HelperOverlayThemes {
    fun resolve(preset: HelperThemePreset): HelperOverlayTheme {
        return when (preset) {
            HelperThemePreset.Brand ->
                HelperOverlayTheme(
                    bubbleIdleColor = 0xFF0A7A4D.toInt(),
                    bubbleListeningColor = 0xFF0A5ED7.toInt(),
                    bubbleResultColor = 0xFF1E8E3E.toInt(),
                    bubbleErrorColor = 0xFFB3261E.toInt(),
                    cardBackgroundColor = 0xFFFFFFFF.toInt(),
                    cardBorderColor = 0xFFD8E2DD.toInt(),
                    titleColor = 0xFF10231B.toInt(),
                    bodyColor = 0xFF21362D.toInt(),
                    primaryButtonColor = 0xFF0A7A4D.toInt(),
                    primaryButtonTextColor = 0xFFFFFFFF.toInt(),
                    secondaryButtonColor = 0xFFEDF6F1.toInt(),
                    secondaryButtonTextColor = 0xFF0A7A4D.toInt()
                )

            HelperThemePreset.Neutral ->
                HelperOverlayTheme(
                    bubbleIdleColor = 0xFF344055.toInt(),
                    bubbleListeningColor = 0xFF2D6CDF.toInt(),
                    bubbleResultColor = 0xFF2A7D5F.toInt(),
                    bubbleErrorColor = 0xFFB04038.toInt(),
                    cardBackgroundColor = 0xFFF8FAFC.toInt(),
                    cardBorderColor = 0xFFD5DCE6.toInt(),
                    titleColor = 0xFF152238.toInt(),
                    bodyColor = 0xFF2F3D4F.toInt(),
                    primaryButtonColor = 0xFF344055.toInt(),
                    primaryButtonTextColor = 0xFFFFFFFF.toInt(),
                    secondaryButtonColor = 0xFFE9EEF5.toInt(),
                    secondaryButtonTextColor = 0xFF243246.toInt()
                )
        }
    }
}
