package com.zeynbakers.order_management_system.order.ui.day_detail.models

/**
 * Action to take when importing an order.
 */
data class OrderImportAction(
    val importItem: com.zeynbakers.order_management_system.order.data.OrderExportItem,
    val duplicateOrderId: Long?,
    val action: ImportAction,
    val customerAction: CustomerImportAction
)

enum class ImportAction {
    MERGE,
    CREATE
}

enum class CustomerImportAction {
    MATCH,
    CREATE
}
