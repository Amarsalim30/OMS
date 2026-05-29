package com.zeynbakers.order_management_system.order.data

import com.zeynbakers.order_management_system.order.ui.OrderCartParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Serializable
data class OrderExportData(
    val exportDate: String,
    val orderDate: String,
    val orders: List<OrderExportItem>
)

@Serializable
data class OrderExportItem(
    val id: Long,
    val orderDate: String,
    val notes: String,
    val totalAmount: String,
    val customerId: Long?,
    val customerName: String?,
    val customerPhone: String?,
    val pickupTime: String?,
    val status: String,
    val cartItems: List<CartItemExport>
)

@Serializable
data class CartItemExport(
    val emoji: String,
    val name: String,
    val quantity: Int,
    val unitPrice: String
)

object OrderExporter {
    private val json = Json { prettyPrint = true }

    fun exportOrders(
        orders: List<OrderEntity>,
        customerNames: Map<Long, String>,
        customerPhones: Map<Long, String>
    ): String {
        val exportDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val orderDate = orders.firstOrNull()?.orderDate?.toString() ?: ""

        val exportItems = orders.map { order ->
            val cartItems = OrderCartParser.parseNotesToCart(order.notes ?: "").map { cartItem ->
                CartItemExport(
                    emoji = cartItem.emoji,
                    name = cartItem.name,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice.toString()
                )
            }
            OrderExportItem(
                id = order.id,
                orderDate = order.orderDate.toString(),
                notes = order.notes ?: "",
                totalAmount = order.totalAmount.toString(),
                customerId = order.customerId,
                customerName = order.customerId?.let { customerNames[it] },
                customerPhone = order.customerId?.let { customerPhones[it] },
                pickupTime = order.pickupTime,
                status = order.status.name,
                cartItems = cartItems
            )
        }

        val exportData = OrderExportData(
            exportDate = exportDate,
            orderDate = orderDate,
            orders = exportItems
        )

        return json.encodeToString(exportData)
    }

    fun exportOrdersToCsv(
        orders: List<OrderEntity>,
        customerNames: Map<Long, String>,
        customerPhones: Map<Long, String>
    ): String {
        val header = "ID,Date,Notes,Total Amount,Customer Name,Customer Phone,Pickup Time,Status,Cart Items\n"
        val rows = orders.joinToString("\n") { order ->
            val customerName = order.customerId?.let { customerNames[it] } ?: ""
            val customerPhone = order.customerId?.let { customerPhones[it] } ?: ""
            val pickupTime = order.pickupTime ?: ""
            val cartItems = OrderCartParser.parseNotesToCart(order.notes ?: "")
                .joinToString("; ") { "${it.name} x${it.quantity}" }
            val escapedNotes = (order.notes ?: "").replace("\"", "\"\"")
            val escapedCustomerName = customerName.replace("\"", "\"\"")
            val escapedCartItems = cartItems.replace("\"", "\"\"")
            "${order.id},${order.orderDate},\"$escapedNotes\",${order.totalAmount},\"$escapedCustomerName\",\"$customerPhone\",\"$pickupTime\",${order.status.name},\"$escapedCartItems\""
        }
        return header + rows
    }

    fun importOrders(jsonString: String): OrderExportData {
        return json.decodeFromString<OrderExportData>(jsonString)
    }
}
