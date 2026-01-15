package com.zeynbakers.order_management_system.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class MonthKey(val year: Int, val month: Int)

data class MonthSnapshot(
    val days: List<CalendarDayUi>,
    val total: BigDecimal,
    val badgeCount: Int
)

class OrderViewModel(private val database: AppDatabase) : ViewModel() {

    private val orderDao: OrderDao = database.orderDao()
    private val accountingDao: AccountingDao = database.accountingDao()
    private val customerDao = database.customerDao()

    private val _calendarDays = MutableStateFlow<List<CalendarDayUi>>(emptyList())
    val calendarDays = _calendarDays.asStateFlow()

    private val _ordersForDate = MutableStateFlow<List<OrderEntity>>(emptyList())
    val ordersForDate = _ordersForDate.asStateFlow()

    private val _dayTotal = MutableStateFlow(BigDecimal.ZERO)
    val dayTotal = _dayTotal.asStateFlow()

    private val _monthTotal = MutableStateFlow(BigDecimal.ZERO)
    val monthTotal = _monthTotal.asStateFlow()

    private val _monthBadgeCount = MutableStateFlow(0)
    val monthBadgeCount = _monthBadgeCount.asStateFlow()

    private val _monthSnapshots = MutableStateFlow<Map<MonthKey, MonthSnapshot>>(emptyMap())
    val monthSnapshots = _monthSnapshots.asStateFlow()

    private val _orderCustomerNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val orderCustomerNames = _orderCustomerNames.asStateFlow()

    private val _orderPaidAmounts = MutableStateFlow<Map<Long, BigDecimal>>(emptyMap())
    val orderPaidAmounts = _orderPaidAmounts.asStateFlow()

    private var lastMonth: Int? = null
    private var lastYear: Int? = null

    fun saveOrder(
        date: LocalDate,
        notes: String,
        totalAmount: BigDecimal,
        customerName: String,
        customerPhone: String,
        existingOrderId: Long?
    ) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val cleanNotes = notes.trim()
            val customerId = resolveCustomerId(customerName, customerPhone)
            val existingOrder =
                if (existingOrderId != null && existingOrderId != 0L) {
                    orderDao.getOrderById(existingOrderId)
                } else {
                    null
                }

            val updatedOrder =
                (existingOrder
                        ?: OrderEntity(
                            orderDate = date,
                            notes = cleanNotes,
                            totalAmount = totalAmount,
                            customerId = customerId
                        ))
                    .copy(
                        orderDate = date,
                        notes = cleanNotes,
                        totalAmount = totalAmount,
                        customerId = customerId,
                        updatedAt = now
                    )

            val orderId =
                if (updatedOrder.id == 0L) {
                    orderDao.insert(updatedOrder)
                } else {
                    orderDao.update(updatedOrder)
                    updatedOrder.id
                }

            upsertAccountingEntry(updatedOrder.copy(id = orderId))

            loadOrdersForDate(date)
            refreshMonthTotals()
        }
    }

    private suspend fun upsertAccountingEntry(order: OrderEntity) {
        accountingDao.upsertDebitForOrder(
            orderId = order.id,
            customerId = order.customerId,
            amount = order.totalAmount,
            date = order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: Order #${order.id}"
        )
    }

    fun loadOrdersForDate(date: LocalDate) {
        viewModelScope.launch {
            val orders = orderDao.getOrdersByDate(date.toString())
            val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
            _ordersForDate.value = activeOrders
            _dayTotal.value =
                activeOrders.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }

            val customerIds = orders.mapNotNull { it.customerId }.distinct()
            _orderCustomerNames.value =
                if (customerIds.isEmpty()) {
                    emptyMap()
                } else {
                    customerDao.getByIds(customerIds).associate { it.id to it.name }
                }

            val orderIds = activeOrders.map { it.id }.filter { it != 0L }
            _orderPaidAmounts.value =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    accountingDao.getPaidForOrders(orderIds).associate {
                        it.orderId to it.paid
                    }
                }
        }
    }

    fun cancelOrder(orderId: Long, date: LocalDate) {
        viewModelScope.launch {
            orderDao.markCancelled(orderId)
            accountingDao.deleteDebitEntriesForOrder(orderId)
            loadOrdersForDate(date)
            refreshMonthTotals()
        }
    }

    fun loadMonth(month: Int, year: Int) {
        viewModelScope.launch {
            loadMonth(month = month, year = year, setAsCurrent = true)
        }
    }

    fun prefetchAdjacentMonths(year: Int, month: Int) {
        viewModelScope.launch {
            val (prevYear, prevMonth) = shiftMonth(year, month, -1)
            val (nextYear, nextMonth) = shiftMonth(year, month, 1)
            loadMonth(month = prevMonth, year = prevYear, setAsCurrent = false)
            loadMonth(month = nextMonth, year = nextYear, setAsCurrent = false)
        }
    }

    private suspend fun loadMonth(month: Int, year: Int, setAsCurrent: Boolean) {
        if (setAsCurrent) {
            lastMonth = month
            lastYear = year
        }
        val start = LocalDate(year, month, 1)
        val daysInMonth = daysInMonth(year, month)
        val endOfMonth = LocalDate(year, month, daysInMonth)
        val leadingDays = start.dayOfWeek.ordinal
        val trailingDays = 6 - endOfMonth.dayOfWeek.ordinal
        val gridStart = start.plus(-leadingDays, DateTimeUnit.DAY)
        val gridEndExclusive = endOfMonth.plus(trailingDays + 1, DateTimeUnit.DAY)

        val orders = orderDao.getOrdersBetween(gridStart.toString(), gridEndExclusive.toString())
        val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
        val grouped = activeOrders.groupBy { it.orderDate }
        val totals =
            activeOrders.groupBy { it.orderDate }
                .mapValues { entry ->
                    entry.value.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }
                }
        val orderIds = activeOrders.map { it.id }.filter { it != 0L }
        val paidByOrder =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
            }
        val paidTotals =
            activeOrders.groupBy { it.orderDate }
                .mapValues { entry ->
                    entry.value.fold(BigDecimal.ZERO) { acc, order ->
                        acc + (paidByOrder[order.id] ?: BigDecimal.ZERO)
                    }
                }
        val unpaidCount =
            activeOrders.count { order ->
                if (order.orderDate.monthNumber != month || order.orderDate.year != year) {
                    false
                } else {
                    val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                    paid < order.totalAmount
                }
            }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val calendarDays =
            (0 until (leadingDays + daysInMonth + trailingDays)).map { offset ->
                val date = gridStart.plus(offset, DateTimeUnit.DAY)
                val dayOrders = grouped[date] ?: emptyList()
                val dayTotal = totals[date] ?: BigDecimal.ZERO
                val dayPaid = paidTotals[date] ?: BigDecimal.ZERO
                val paymentState =
                    if (dayTotal <= BigDecimal.ZERO) {
                        null
                    } else {
                        val balance = dayTotal - dayPaid
                        when {
                            dayPaid <= BigDecimal.ZERO -> PaymentState.UNPAID
                            balance > BigDecimal.ZERO -> PaymentState.PARTIAL
                            balance == BigDecimal.ZERO -> PaymentState.PAID
                            else -> PaymentState.OVERPAID
                        }
                    }
                CalendarDayUi(
                    date = date,
                    orderCount = dayOrders.size,
                    totalAmount = dayTotal,
                    isToday = date == today,
                    isInCurrentMonth = date.monthNumber == month,
                    paymentState = paymentState
                )
            }

        val monthTotal =
            activeOrders
                .filter { it.orderDate >= start && it.orderDate <= endOfMonth }
                .fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }
        val badgeCount = unpaidCount
        val key = MonthKey(year, month)
        _monthSnapshots.value = _monthSnapshots.value + (key to MonthSnapshot(calendarDays, monthTotal, badgeCount))

        if (setAsCurrent) {
            _calendarDays.value = calendarDays
            _monthTotal.value = monthTotal
            _monthBadgeCount.value = badgeCount
        }
    }

    suspend fun getCustomerById(id: Long): CustomerEntity? {
        return customerDao.getById(id)
    }

    suspend fun searchCustomers(query: String): List<CustomerEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val pattern = "%$trimmed%"
        return customerDao.searchCustomers(pattern)
    }

    private suspend fun resolveCustomerId(name: String, phone: String): Long? {
        if (name.isBlank() && phone.isBlank()) return null
        if (name.isBlank() || phone.isBlank()) return null

        val existing = customerDao.getByPhone(phone)
        return if (existing != null) {
            if (existing.name != name) {
                customerDao.update(existing.copy(name = name))
            }
            existing.id
        } else {
            customerDao.insert(CustomerEntity(name = name, phone = phone))
        }
    }

    private fun refreshMonthTotals() {
        val month = lastMonth ?: return
        val year = lastYear ?: return
        loadMonth(month = month, year = year)
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val total = (year * 12) + (month - 1) + delta
        val newYear = total / 12
        val newMonth = (total % 12) + 1
        return Pair(newYear, newMonth)
    }
}
