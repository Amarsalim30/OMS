package com.zeynbakers.order_management_system.order.data

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
    val status: String
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
            OrderExportItem(
                id = order.id,
                orderDate = order.orderDate.toString(),
                notes = order.notes,
                totalAmount = order.totalAmount.toString(),
                customerId = order.customerId,
                customerName = order.customerId?.let { customerNames[it] },
                customerPhone = order.customerId?.let { customerPhones[it] },
                pickupTime = order.pickupTime,
                status = order.status.name
            )
        }

        val exportData = OrderExportData(
            exportDate = exportDate,
            orderDate = orderDate,
            orders = exportItems
        )

        return json.encodeToString(exportData)
    }

    fun importOrders(jsonString: String): OrderExportData {
        return json.decodeFromString<OrderExportData>(jsonString)
    }
}
