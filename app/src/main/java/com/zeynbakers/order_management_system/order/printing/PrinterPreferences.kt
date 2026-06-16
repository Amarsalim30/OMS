package com.zeynbakers.order_management_system.order.printing

import android.content.Context

class PrinterPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPrinterMac(): String? = prefs.getString(KEY_MAC, null)?.takeIf { it.isNotBlank() }

    fun getPrinterName(): String? = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }

    fun savePrinter(macAddress: String, name: String) {
        prefs.edit()
            .putString(KEY_MAC, macAddress.trim())
            .putString(KEY_NAME, name.trim())
            .apply()
    }

    fun clearPrinter() {
        prefs.edit()
            .remove(KEY_MAC)
            .remove(KEY_NAME)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "bluetooth_printer_prefs"
        const val KEY_MAC = "printer_mac"
        const val KEY_NAME = "printer_name"
    }
}
