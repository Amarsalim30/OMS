package com.zeynbakers.order_management_system.accounting.domain

import java.math.BigDecimal

data class LedgerSummary(
    val customerId: Long,
    val totalBilled: Double,
    val totalPaid: Double,
    val outstanding: Double
)
