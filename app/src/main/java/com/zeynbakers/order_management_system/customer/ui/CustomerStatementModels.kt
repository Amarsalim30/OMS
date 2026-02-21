package com.zeynbakers.order_management_system.customer.ui

import java.math.BigDecimal

data class CustomerStatementRowUi(
    val key: String,
    val timestamp: Long,
    val type: CustomerStatementRowType,
    val title: String,
    val subtitle: String?,
    val amount: BigDecimal,
    val signedAmount: BigDecimal,
    val runningBalance: BigDecimal,
    val paymentDetails: CustomerPaymentAllocationDetailsUi? = null
)

enum class CustomerStatementRowType {
    ORDER,
    PAYMENT,
    BAD_DEBT,
    ADJUSTMENT
}

data class CustomerPaymentAllocationDetailsUi(
    val receiptId: Long?,
    val methodLabel: String,
    val allocations: List<CustomerPaymentAllocationTargetUi>,
    val unallocatedCredit: BigDecimal
)

data class CustomerPaymentAllocationTargetUi(
    val orderId: Long,
    val orderLabel: String,
    val amount: BigDecimal
)
