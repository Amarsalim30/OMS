package com.zeynbakers.order_management_system.core.helper

enum class HelperCaptureMode(val wireValue: String) {
    VoiceNote("voice_note"),
    VoiceCalculator("voice_calculator");

    companion object {
        fun fromWireValue(value: String?): HelperCaptureMode {
            return entries.firstOrNull { it.wireValue == value } ?: VoiceNote
        }
    }
}
