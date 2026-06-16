package com.zeynbakers.order_management_system.order.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Models for exporting and importing order data.
 */

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

/**
 * Handles exporting orders to JSON and CSV formats.
 */
object OrderExporter {
    private val json = Json { prettyPrint = true }

    fun exportOrders(
        orders: List<OrderEntity>,
        orderItemsMap: Map<Long, List<OrderItemEntity>>,
        customerNames: Map<Long, String>,
        customerPhones: Map<Long, String>
    ): String {
        val exportDate =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val orderDate = orders.firstOrNull()?.orderDate?.toString() ?: ""

        val exportItems = orders.map { order ->
            val items = orderItemsMap[order.id] ?: emptyList()
            val cartItems = items.map { item ->
                CartItemExport(
                    emoji = "",
                    name = item.productNameSnapshot,
                    quantity = item.quantity,
                    unitPrice = item.effectivePrice.toString()
                )
            }
            OrderExportItem(
                id = order.id,
                orderDate = order.orderDate.toString(),
                notes = "",
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
        orderItemsMap: Map<Long, List<OrderItemEntity>>,
        customerNames: Map<Long, String>,
        customerPhones: Map<Long, String>
    ): String {
        val header =
            "ID,Date,Notes,Total Amount,Customer Name,Customer Phone,Pickup Time,Status,Cart Items\n"
        val rows = orders.joinToString("\n") { order ->
            val customerName = order.customerId?.let { customerNames[it] } ?: ""
            val customerPhone = order.customerId?.let { customerPhones[it] } ?: ""
            val pickupTime = order.pickupTime ?: ""
            val items = orderItemsMap[order.id] ?: emptyList()
            val cartItems = items.joinToString("; ") { "${it.productNameSnapshot} x${it.quantity}" }
            val escapedCustomerName = customerName.replace("\"", "\"\"")
            val escapedCartItems = cartItems.replace("\"", "\"\"")
            "${order.id},${order.orderDate},\"\",${order.totalAmount},\"$escapedCustomerName\",\"$customerPhone\",\"$pickupTime\",${order.status.name},\"$escapedCartItems\""
        }
        return header + rows
    }

    fun importOrders(jsonString: String): OrderExportData {
        return json.decodeFromString<OrderExportData>(jsonString)
    }
}

/**
 * Handles importing and parsing order data from JSON and CSV strings.
 */
sealed class ImportResult {
    data class Success(val data: OrderExportData) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

enum class ImportFormat {
    JSON,
    CSV,
    UNKNOWN
}

object OrderImportParser {
    fun parseJson(jsonString: String): ImportResult {
        return try {
            val data = OrderExporter.importOrders(jsonString)
            ImportResult.Success(data)
        } catch (e: Exception) {
            ImportResult.Error("Invalid JSON format: ${e.message}")
        }
    }

    fun parseCsv(csvString: String): ImportResult {
        return try {
            val lines = csvString.trim().split("\n")
            if (lines.isEmpty() || lines.size < 2) {
                return ImportResult.Error("CSV file is empty or has no data")
            }

            val header = lines[0].split(",")
            val expectedHeaders = listOf(
                "ID",
                "Date",
                "Notes",
                "Total Amount",
                "Customer Name",
                "Customer Phone",
                "Pickup Time",
                "Status",
                "Cart Items"
            )

            if (!header.map { it.trim() }.containsAll(expectedHeaders)) {
                return ImportResult.Error("CSV headers do not match expected format")
            }

            val orders = lines.drop(1).mapNotNull { line ->
                parseCsvLine(line)
            }

            val exportData = OrderExportData(
                exportDate = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                orderDate = orders.firstOrNull()?.orderDate ?: "",
                orders = orders
            )

            ImportResult.Success(exportData)
        } catch (e: Exception) {
            ImportResult.Error("Invalid CSV format: ${e.message}")
        }
    }

    private fun parseCsvLine(line: String): OrderExportItem? {
        return try {
            val fields = parseCsvFields(line)
            if (fields.size < 9) return null

            val id = fields[0].toLongOrNull() ?: 0L
            val orderDate = fields[1]
            val notes = fields[2]
            val totalAmount = fields[3]
            val customerName = fields[4].takeIf { it.isNotBlank() }
            val customerPhone = fields[5].takeIf { it.isNotBlank() }
            val pickupTime = fields[6].takeIf { it.isNotBlank() }
            val status = fields[7]
            val cartItemsRaw = fields[8]

            val cartItems = parseCartItems(cartItemsRaw)

            OrderExportItem(
                id = id,
                orderDate = orderDate,
                notes = notes,
                totalAmount = totalAmount,
                customerId = null,
                customerName = customerName,
                customerPhone = customerPhone,
                pickupTime = pickupTime,
                status = status,
                cartItems = cartItems
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCsvFields(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }

                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }

                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString())

        return result.map { it.replace("\"\"", "\"") }
    }

    private fun parseCartItems(cartItemsRaw: String): List<CartItemExport> {
        if (cartItemsRaw.isBlank()) return emptyList()

        return cartItemsRaw.split(";").mapNotNull { itemRaw ->
            val trimmed = itemRaw.trim()
            val match = Regex("(.+)\\s+x(\\d+)").find(trimmed)
            if (match != null) {
                val name = match.groupValues[1].trim()
                val quantity = match.groupValues[2].toIntOrNull() ?: 1
                CartItemExport(
                    emoji = "",
                    name = name,
                    quantity = quantity,
                    unitPrice = "0"
                )
            } else {
                null
            }
        }
    }

    fun detectFormat(content: String): ImportFormat {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> ImportFormat.JSON
            trimmed.contains(",") && trimmed.contains("\n") -> ImportFormat.CSV
            else -> ImportFormat.UNKNOWN
        }
    }

    fun parse(content: String): ImportResult {
        return when (detectFormat(content)) {
            ImportFormat.JSON -> parseJson(content)
            ImportFormat.CSV -> parseCsv(content)
            ImportFormat.UNKNOWN -> ImportResult.Error("Unknown file format")
        }
    }
}
