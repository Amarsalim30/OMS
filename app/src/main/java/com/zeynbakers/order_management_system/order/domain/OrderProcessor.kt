@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.domain

import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderStatus

class OrderProcessor(
    private val orderDao: OrderDao,
    private val accountingDao: AccountingDao
) {

    suspend fun completeOrder(orderId: Long) {
        val order = orderDao.getOrderById(orderId)
            ?: throw IllegalStateException("Order not found")

        if (order.status == OrderStatus.COMPLETED) return

        orderDao.markCompleted(orderId)
    }

    suspend fun cancelOrder(orderId: Long) {
        val order = orderDao.getOrderById(orderId)
            ?: throw IllegalStateException("Order not found")

        if (order.status == OrderStatus.CANCELLED) return

        orderDao.markCancelled(orderId)
        accountingDao.deleteDebitEntriesForOrder(orderId)
        accountingDao.deleteWriteOffEntriesForOrder(orderId)
        accountingDao.moveOrderCreditsToCustomerLevel(orderId)
    }
}
    
