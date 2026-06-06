package com.zeynbakers.order_management_system.order.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

sealed class ImportResult {
    data class Success(val data: OrderExportData) : ImportResult()
    data class Error(val message: String) : ImportResult()
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
            val expectedHeaders = listOf("ID", "Date", "Notes", "Total Amount", "Customer Name", "Customer Phone", "Pickup Time", "Status", "Cart Items")
            
            if (!header.map { it.trim() }.containsAll(expectedHeaders)) {
                return ImportResult.Error("CSV headers do not match expected format")
            }

            val orders = lines.drop(1).mapNotNull { line ->
                parseCsvLine(line)
            }

            val exportData = OrderExportData(
                exportDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
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

enum class ImportFormat {
    JSON,
    CSV,
    UNKNOWN
}
