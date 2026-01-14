package com.zeynbakers.order_management_system.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
        accountingDao.deleteDebitEntriesForOrder(order.id)
        val incomeEntry =
            AccountEntryEntity(
                orderId = order.id,
                type = EntryType.DEBIT,
                amount = order.totalAmount,
                date = order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                customerId = order.customerId,
                description = "Order #${order.id}"
            )

        accountingDao.insertAccountEntry(incomeEntry)
    }

    fun loadOrdersForDate(date: LocalDate) {
        viewModelScope.launch {
            val orders = orderDao.getOrdersByDate(date.toString())
            _ordersForDate.value = orders
            _dayTotal.value = orderDao.getTotalForDate(date.toString())

            val customerIds = orders.mapNotNull { it.customerId }.distinct()
            _orderCustomerNames.value =
                if (customerIds.isEmpty()) {
                    emptyMap()
                } else {
                    customerDao.getByIds(customerIds).associate { it.id to it.name }
                }

            val orderIds = orders.map { it.id }.filter { it != 0L }
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

    fun loadMonth(month: Int, year: Int) {
        viewModelScope.launch {
            lastMonth = month
            lastYear = year
            val start = LocalDate(year, month, 1)
            val daysInMonth = Month(month).length(isLeapYear(year))
            val endOfMonth = LocalDate(year, month, daysInMonth)
            val leadingDays = start.dayOfWeek.ordinal
            val trailingDays = 6 - endOfMonth.dayOfWeek.ordinal
            val gridStart = start.plus(-leadingDays, DateTimeUnit.DAY)
            val gridEndExclusive = endOfMonth.plus(trailingDays + 1, DateTimeUnit.DAY)

            val orders = orderDao.getOrdersBetween(gridStart.toString(), gridEndExclusive.toString())
            val grouped = orders.groupBy { it.orderDate }
            val totals =
                orders.groupBy { it.orderDate }
                    .mapValues { entry ->
                        entry.value.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }
                    }

            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            _calendarDays.value =
                (0 until (leadingDays + daysInMonth + trailingDays)).map { offset ->
                    val date = gridStart.plus(offset, DateTimeUnit.DAY)
                    val dayOrders = grouped[date] ?: emptyList()
                    CalendarDayUi(
                        date = date,
                        orderCount = dayOrders.size,
                        totalAmount = totals[date] ?: BigDecimal.ZERO,
                        isToday = date == today,
                        isInCurrentMonth = date.monthNumber == month
                    )
                }

            _monthTotal.value = orderDao.getTotalBetween(start.toString(), endOfMonth.plus(1, DateTimeUnit.DAY).toString())
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
        loadMonth(month, year)
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
