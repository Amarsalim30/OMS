package com.zeynbakers.order_management_system.customer.ui

import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal

data class CustomerOrderUi(
    val order: OrderEntity,
    val paidAmount: BigDecimal,
    val lastPaymentAt: Long?,
    val lastActivityAt: Long,
    val paymentState: OrderPaymentState,
    val effectiveStatus: OrderEffectiveStatus,
    val statusOverride: OrderStatusOverride?
)

enum class OrderPaymentState {
    UNPAID,
    PARTIAL,
    PAID,
    OVERPAID
}

enum class OrderEffectiveStatus {
    OPEN,
    CLOSED
}
