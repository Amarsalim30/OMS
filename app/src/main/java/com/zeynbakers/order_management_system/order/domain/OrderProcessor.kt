@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.domain

import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class OrderProcessor(
    private val orderDao: OrderDao,
    private val accountingDao: AccountingDao
) {

    suspend fun completeOrder(orderId: Long) {
        val order = orderDao.getOrderById(orderId)
            ?: throw IllegalStateException("Order not found")

        if (order.status == OrderStatus.COMPLETED) return

        orderDao.markCompleted(orderId)

        accountingDao.insertAccountEntry(
            AccountEntryEntity(
                orderId = orderId,
                customerId = order.customerId,
                amount = order.totalAmount,
                type = EntryType.DEBIT,
                date = order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                description = "Order #$orderId"
            )
        )
    }
}
    
