package com.zeynbakers.order_management_system.accounting.ui

fun MpesaTransactionUi.canApply(): Boolean {
    return when (allocationMode) {
        AllocationMode.ORDER -> selectedOrderId != null
        AllocationMode.CUSTOMER_CREDIT,
        AllocationMode.OLDEST_ORDERS -> selectedCustomerId != null
    }
}

fun MpesaTransactionUi.isReadySelectable(): Boolean {
    return duplicateState == DuplicateState.NONE && canApply()
}
