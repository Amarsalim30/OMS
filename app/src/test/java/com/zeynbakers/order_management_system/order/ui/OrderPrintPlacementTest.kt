package com.zeynbakers.order_management_system.order.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPrintPlacementTest {
    @Test
    fun printReceiptActionIsOnOrderRowNotEditor() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val orderRowSource =
            projectDir.resolve(
                "src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailSections.kt"
            )
        val editorSource =
            projectDir.resolve(
                "src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt"
            )

        assertTrue(orderRowSource.readText().contains("R.string.order_print_receipt"))
        assertFalse(editorSource.readText().contains("R.string.order_print_receipt"))
    }
}
