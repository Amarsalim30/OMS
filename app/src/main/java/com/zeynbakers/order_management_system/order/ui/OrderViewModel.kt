package com.zeynbakers.order_management_system.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
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
    private val allocationDao = database.paymentAllocationDao()
    private val receiptDao = database.paymentReceiptDao()
    private val receiptProcessor = PaymentReceiptProcessor(database)

    private val _calendarDays = MutableStateFlow<List<CalendarDayUi>>(emptyList())
    val calendarDays = _calendarDays.asStateFlow()

    private val _ordersForDate = MutableStateFlow<List<OrderEntity>>(emptyList())
    val ordersForDate = _ordersForDate.asStateFlow()

    private val _dayTotal = MutableStateFlow(BigDecimal.ZERO)
    val dayTotal = _dayTotal.asStateFlow()

    private val _summaryOrders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val summaryOrders = _summaryOrders.asStateFlow()

    private val _summaryTotal = MutableStateFlow(BigDecimal.ZERO)
    val summaryTotal = _summaryTotal.asStateFlow()

    private val _summaryCustomerNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val summaryCustomerNames = _summaryCustomerNames.asStateFlow()

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

    private val _unpaidOrders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val unpaidOrders = _unpaidOrders.asStateFlow()

    private val _unpaidPaidAmounts = MutableStateFlow<Map<Long, BigDecimal>>(emptyMap())
    val unpaidPaidAmounts = _unpaidPaidAmounts.asStateFlow()

    private val _unpaidCustomerNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val unpaidCustomerNames = _unpaidCustomerNames.asStateFlow()

    private val _creditPrompt = MutableStateFlow<OrderCreditPrompt?>(null)
    val creditPrompt = _creditPrompt.asStateFlow()

    private var lastMonth: Int? = null
    private var lastYear: Int? = null

    fun saveOrder(
        date: LocalDate,
        notes: String,
        totalAmount: BigDecimal,
        customerName: String,
        customerPhone: String,
        pickupTime: String?,
        existingOrderId: Long?
    ) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val cleanNotes = notes.trim()
            val normalizedPickupTime = pickupTime?.trim()?.takeIf { it.isNotBlank() }
            val existingOrder =
                if (existingOrderId != null && existingOrderId != 0L) {
                    orderDao.getOrderById(existingOrderId)
                } else {
                    null
                }
            val isNewOrder = existingOrder == null
            val resolvedCustomerId = resolveCustomerId(customerName.trim(), customerPhone.trim())
            val customerId =
                if (existingOrder != null && resolvedCustomerId == null) {
                    // Avoid corrupting existing ledgers by unassigning the customer on edit.
                    existingOrder.customerId
                } else {
                    resolvedCustomerId
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
                        pickupTime = normalizedPickupTime,
                        updatedAt = now
                    )

            val orderId =
                if (updatedOrder.id == 0L) {
                    orderDao.insert(updatedOrder)
                } else {
                    orderDao.update(updatedOrder)
                    updatedOrder.id
                }

            if (existingOrder?.customerId != null && customerId != null && existingOrder.customerId != customerId) {
                accountingDao.updateCustomerIdForOrderEntries(orderId = orderId, customerId = customerId)
            } else if (existingOrder?.customerId == null && customerId != null) {
                accountingDao.updateCustomerIdForOrderEntries(orderId = orderId, customerId = customerId)
            }

            val savedOrder = updatedOrder.copy(id = orderId)
            upsertAccountingEntry(savedOrder)
            accountingDao.reconcileOrderSettlementToTotal(
                orderId = orderId,
                customerId = customerId,
                orderTotal = savedOrder.totalAmount,
                now = now
            )
            if (isNewOrder && customerId != null) {
                val financeTotals = accountingDao.getCustomerFinanceTotals(customerId)
                val availableCredit = financeTotals.extraCredit ?: BigDecimal.ZERO
                if (availableCredit > BigDecimal.ZERO) {
                    val customerName = customerId?.let { customerDao.getById(it)?.name }
                    val orderLabel =
                        formatOrderLabelWithId(
                            orderId = orderId,
                            date = savedOrder.orderDate,
                            customerName = customerName,
                            notes = savedOrder.notes,
                            totalAmount = savedOrder.totalAmount
                        )
                    _creditPrompt.value =
                        OrderCreditPrompt(
                            orderId = orderId,
                            customerId = customerId,
                            availableCredit = availableCredit,
                            orderLabel = orderLabel
                        )
                }
            }

            loadOrdersForDate(date)
            refreshMonthTotals()
        }
    }

    fun applyAvailableCreditToOrder(orderId: Long, customerId: Long) {
        viewModelScope.launch {
            val order = orderDao.getOrderById(orderId) ?: return@launch
            val now = Clock.System.now().toEpochMilliseconds()
            accountingDao.applyAvailableCustomerCreditToOrder(
                orderId = orderId,
                customerId = customerId,
                orderTotal = order.totalAmount,
                now = now
            )
            loadOrdersForDate(order.orderDate)
            refreshMonthTotals()
            _creditPrompt.value = null
        }
    }

    fun clearCreditPrompt() {
        _creditPrompt.value = null
    }

    private suspend fun upsertAccountingEntry(order: OrderEntity) {
        val customerName = order.customerId?.let { customerDao.getById(it)?.name }
        val orderLabel =
            formatOrderLabelWithId(
                orderId = order.id,
                date = order.orderDate,
                customerName = customerName,
                notes = order.notes,
                totalAmount = order.totalAmount
            )
        accountingDao.upsertDebitForOrder(
            orderId = order.id,
            customerId = order.customerId,
            amount = order.totalAmount,
            date = order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: $orderLabel"
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

    fun loadSummaryRange(startInclusive: LocalDate, endExclusive: LocalDate) {
        viewModelScope.launch {
            val orders = orderDao.getOrdersBetween(startInclusive.toString(), endExclusive.toString())
            val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
            _summaryOrders.value =
                activeOrders.sortedWith(
                    compareByDescending<OrderEntity> { it.orderDate }.thenByDescending { it.createdAt }
                )
            _summaryTotal.value =
                activeOrders.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }

            val customerIds = activeOrders.mapNotNull { it.customerId }.distinct()
            _summaryCustomerNames.value =
                if (customerIds.isEmpty()) {
                    emptyMap()
                } else {
                    customerDao.getByIds(customerIds).associate { it.id to it.name }
                }
        }
    }

    fun cancelOrder(orderId: Long, date: LocalDate) {
        viewModelScope.launch {
            orderDao.markCancelled(orderId)
            accountingDao.deleteDebitEntriesForOrder(orderId)
            accountingDao.deleteWriteOffEntriesForOrder(orderId)
            accountingDao.moveOrderCreditsToCustomerLevel(orderId)
            loadOrdersForDate(date)
            refreshMonthTotals()
        }
    }

    suspend fun loadOrderPaymentAllocations(orderId: Long): List<OrderPaymentAllocationUi> {
        val allocations =
            allocationDao.getByOrderId(orderId)
                .filter { it.status == PaymentAllocationStatus.APPLIED }
        if (allocations.isEmpty()) return emptyList()
        val receiptIds = allocations.map { it.receiptId }.distinct()
        val receiptsById = receiptDao.getByIds(receiptIds).associateBy { it.id }
        return allocations.mapNotNull { allocation ->
            val receipt = receiptsById[allocation.receiptId] ?: return@mapNotNull null
            OrderPaymentAllocationUi(
                allocationId = allocation.id,
                receiptId = allocation.receiptId,
                amount = allocation.amount,
                receivedAt = receipt.receivedAt,
                method = receipt.method,
                transactionCode = receipt.transactionCode,
                senderName = receipt.senderName,
                senderPhone = receipt.senderPhone
            )
        }
    }

    suspend fun loadMoveOrderOptions(
        customerId: Long?,
        excludeOrderId: Long
    ): List<OrderMoveOption> {
        val orders =
            if (customerId == null) {
                orderDao.getActiveOrders()
            } else {
                orderDao.getOrdersByCustomer(customerId)
            }
        val customerIds = orders.mapNotNull { it.customerId }.distinct()
        val customerNames =
            if (customerIds.isEmpty()) emptyMap()
            else customerDao.getByIds(customerIds).associate { it.id to it.name }
        return orders
            .filter { it.id != excludeOrderId }
            .filter { it.status != OrderStatus.CANCELLED && it.statusOverride != OrderStatusOverride.CLOSED }
            .sortedWith(
                compareBy<OrderEntity> { it.orderDate }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )
            .map { order ->
                val label =
                    formatOrderLabelWithId(
                        orderId = order.id,
                        date = order.orderDate,
                        customerName = order.customerId?.let { customerNames[it] },
                        notes = order.notes,
                        totalAmount = order.totalAmount
                    )
                OrderMoveOption(order.id, label)
            }
    }

    suspend fun deleteOrderWithPayments(
        orderId: Long,
        date: LocalDate,
        allocationIds: List<Long>,
        action: OrderPaymentAction,
        target: ReceiptAllocation?,
        moveFullReceipts: Boolean
    ): Boolean {
        val order = orderDao.getOrderById(orderId)
        val orderLabel =
            order?.let {
                val customerName = it.customerId?.let { id -> customerDao.getById(id)?.name }
                formatOrderLabelWithId(
                    orderId = it.id,
                    date = it.orderDate,
                    customerName = customerName,
                    notes = it.notes,
                    totalAmount = it.totalAmount
                )
            } ?: "Order ID $orderId"
        val description = "$orderLabel deleted"
        when (action) {
            OrderPaymentAction.MOVE -> {
                if (allocationIds.isNotEmpty() && target != null) {
                    receiptProcessor.moveAllocations(
                        allocationIds = allocationIds,
                        target = target,
                        descriptionBase = description,
                        moveFullReceipts = moveFullReceipts
                    )
                }
            }
            OrderPaymentAction.VOID -> {
                if (allocationIds.isNotEmpty()) {
                    receiptProcessor.voidAllocations(
                        allocationIds = allocationIds,
                        reason = "Order deleted"
                    )
                }
            }
        }
        orderDao.markCancelled(orderId)
        accountingDao.deleteDebitEntriesForOrder(orderId)
        accountingDao.deleteWriteOffEntriesForOrder(orderId)
        accountingDao.moveOrderCreditsToCustomerLevel(orderId)
        loadOrdersForDate(date)
        refreshMonthTotals()
        return true
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
                val orderStates =
                    dayOrders.map { order ->
                        val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                        resolveOrderPaymentState(order.totalAmount, paid)
                    }
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
                    paymentState = paymentState,
                    orderStates = orderStates
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
        if (phone.isBlank()) return null

        val cleanName = name.ifBlank { phone }
        val existing = customerDao.getByPhone(phone)
        return if (existing != null) {
            if (name.isNotBlank() && existing.name != name) {
                customerDao.update(existing.copy(name = name))
            }
            existing.id
        } else {
            customerDao.insert(CustomerEntity(name = cleanName, phone = phone))
        }
    }

    private fun refreshMonthTotals() {
        val month = lastMonth ?: return
        val year = lastYear ?: return
        loadMonth(month = month, year = year)
    }

    fun loadUnpaidOrders() {
        viewModelScope.launch {
            val orders =
                orderDao.getActiveOrders()
                    .filter { it.statusOverride != OrderStatusOverride.CLOSED }
            val orderIds = orders.map { it.id }.filter { it != 0L }
            val paidByOrder =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
                }
            val unpaid =
                orders.filter { order ->
                    val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                    paid < order.totalAmount
                }
            val customerIds = unpaid.mapNotNull { it.customerId }.distinct()
            _unpaidCustomerNames.value =
                if (customerIds.isEmpty()) {
                    emptyMap()
                } else {
                    customerDao.getByIds(customerIds).associate { it.id to it.name }
                }
            _unpaidPaidAmounts.value = paidByOrder
            _unpaidOrders.value =
                unpaid.sortedWith(
                    compareByDescending<OrderEntity> { it.orderDate }.thenByDescending { it.createdAt }
                )
        }
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

    private fun resolveOrderPaymentState(total: BigDecimal, paid: BigDecimal): PaymentState {
        if (paid <= BigDecimal.ZERO) return PaymentState.UNPAID
        val balance = total - paid
        return when {
            balance > BigDecimal.ZERO -> PaymentState.PARTIAL
            balance == BigDecimal.ZERO -> PaymentState.PAID
            else -> PaymentState.OVERPAID
        }
    }
}

data class OrderPaymentAllocationUi(
    val allocationId: Long,
    val receiptId: Long,
    val amount: BigDecimal,
    val receivedAt: Long,
    val method: PaymentMethod,
    val transactionCode: String?,
    val senderName: String?,
    val senderPhone: String?
)

data class OrderMoveOption(
    val orderId: Long,
    val label: String
)

enum class OrderPaymentAction {
    MOVE,
    VOID
}

data class OrderCreditPrompt(
    val orderId: Long,
    val customerId: Long,
    val availableCredit: BigDecimal,
    val orderLabel: String
)
