@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.domain

import java.math.BigDecimal

data class LedgerSummary(
    val customerId: Long,
    val totalBilled: BigDecimal,
    val totalPaid: BigDecimal,
    val outstanding: BigDecimal
)
