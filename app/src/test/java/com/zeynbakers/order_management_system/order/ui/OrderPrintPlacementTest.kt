package com.zeynbakers.order_management_system.order.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OrderPrintPlacementTest {
    @Test
    fun printReceiptActionIsOnOrderRowNotEditor() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val orderRowSource =
            projectDir.resolve(
                "src/main/java/com/zeynbakers/order_management_system/order/ui/day_detail/components/DayDetailComponents.kt"
            )
        val editorSource =
            projectDir.resolve(
                "src/main/java/com/zeynbakers/order_management_system/order/ui/order_editor/OrderEditorSheet.kt"
            )

        assertTrue(orderRowSource.readText().contains("R.string.order_print_receipt"))
        assertFalse(editorSource.readText().contains("R.string.order_print_receipt"))
    }
}
