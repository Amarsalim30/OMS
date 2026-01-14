package com.zeynbakers.order_management_system.accounting.data

import java.math.BigDecimal

data class CustomerAccountSummary(
    val customerId: Long,
    val name: String,
    val phone: String,
    val billed: BigDecimal,
    val paid: BigDecimal,
    val balance: BigDecimal
)

data class OrderPaymentSummary(
    val orderId: Long,
    val paid: BigDecimal
)
