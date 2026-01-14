package com.zeynbakers.order_management_system.order.data

import java.math.BigDecimal

data class CustomerTotal(
    val customerId: Long,
    val name: String,
    val phone: String,
    val total: BigDecimal
)
