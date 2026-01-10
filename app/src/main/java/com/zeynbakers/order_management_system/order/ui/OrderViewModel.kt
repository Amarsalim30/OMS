package com.zeynbakers.order_management_system.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.order.data.*
import com.zeynbakers.order_management_system.accounting.data.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toEpochDays

class OrderViewModel(
    private val db: DatabaseProvider
) : ViewModel() {

    private val orderDao = db.getDatabase().orderDao()
    private val accountingDao = db.getDatabase().accountingDao()

    /* ---------------- CREATE / UPDATE ORDER ---------------- */

    fun saveOrder(
        order: OrderEntity
    ) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()

            val updatedOrder = order.copy(updatedAt = now)

            val orderId = if (order.id == 0L) {
                orderDao.insert(updatedOrder)
            } else {
                orderDao.update(updatedOrder)
                order.id
            }

            handleAccounting(updatedOrder.copy(id = orderId))
        }
    }

    /* ---------------- ORDER STATUS LOGIC ---------------- */

    private suspend fun handleAccounting(order: OrderEntity) {

        if (order.status != OrderStatus.COMPLETED) return

        val existingEntries =
            accountingDao.getEntriesForOrder(order.id)

        if (existingEntries.isNotEmpty()) return

        val incomeEntry = AccountEntryEntity(
            orderId = order.id,
            type = EntryType.INCOME,
            amount = order.totalAmount,
            date = order.orderDate.toEpochDays().toLong(),
            description = "Order #${order.id}"
        )

        accountingDao.insertAccountEntry(incomeEntry)
    }

    /* ---------------- PAYMENTS ---------------- */

    fun addPayment(
        order: OrderEntity,
        amount: BigDecimal,
        method: PaymentMethod
    ) {
        viewModelScope.launch {

            require(amount > BigDecimal.ZERO)

            val payment = PaymentEntity(
                orderId = order.id,
                amount = amount,
                method = method,
                paidAt = Clock.System.now().toEpochMilliseconds()
            )

            accountingDao.insertPayment(payment)

            val newPaid = order.amountPaid + amount

            val updatedOrder = order.copy(
                amountPaid = newPaid
            )

            orderDao.update(updatedOrder)
        }
    }
    fun loadMonth(
    month: Int,
    year: Int,
    onResult: (List<CalendarDayUi>) -> Unit
) {
    viewModelScope.launch {
        val start = LocalDate(year, month, 1)
        val end = start.plus(1, DateTimeUnit.MONTH)

        val orders = orderDao.getOrdersBetween(
            start.toString(),
            end.toString()
        )

        val grouped = orders.groupBy { it.orderDate }

        val days = (1..start.lengthOfMonth()).map { day ->
            val date = LocalDate(year, month, day)
            val dayOrders = grouped[date] ?: emptyList()

            CalendarDayUi(
                date = date,
                hasOrders = dayOrders.isNotEmpty(),
                orderCount = dayOrders.size
            )
        }

        onResult(days)
    }
}
fun loadOrdersForDate(
    date: LocalDate,
    onResult: (List<OrderEntity>) -> Unit
) {
    viewModelScope.launch {
        val orders = orderDao.getOrdersByDate(date.toString())
        onResult(orders)
    }
}

fun newOrder(date: LocalDate): OrderEntity {
    return OrderEntity(
        orderDate = date,
        notes = "",
        totalAmount = BigDecimal.ZERO
    )
}

}
