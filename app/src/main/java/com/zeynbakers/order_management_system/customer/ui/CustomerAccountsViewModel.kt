package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class CustomerAccountsViewModel(private val database: AppDatabase) : ViewModel() {
    private val accountingDao: AccountingDao = database.accountingDao()
    private val customerDao = database.customerDao()
    private val orderDao = database.orderDao()

    private val _summaries = MutableStateFlow<List<CustomerAccountSummary>>(emptyList())
    val summaries = _summaries.asStateFlow()

    private val _customer = MutableStateFlow<CustomerEntity?>(null)
    val customer = _customer.asStateFlow()

    private val _ledger = MutableStateFlow<List<AccountEntryEntity>>(emptyList())
    val ledger = _ledger.asStateFlow()

    private val _balance = MutableStateFlow(BigDecimal.ZERO)
    val balance = _balance.asStateFlow()

    private val _orders = MutableStateFlow<List<CustomerOrderUi>>(emptyList())
    val orders = _orders.asStateFlow()
    private var lastQuery: String = ""

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            lastQuery = query
            val pattern = "%${query.trim()}%"
            _summaries.value = accountingDao.getCustomerAccountSummaries(pattern)
        }
    }

    fun importCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val cleanPhone = phone.trim()
            if (cleanPhone.isBlank()) return@launch
            val cleanName = name.trim().ifBlank { cleanPhone }

            val existing = customerDao.getByPhone(cleanPhone)
            if (existing != null) {
                if (existing.name != cleanName) {
                    customerDao.update(existing.copy(name = cleanName))
                }
            } else {
                customerDao.insert(CustomerEntity(name = cleanName, phone = cleanPhone))
            }

            val pattern = "%${lastQuery.trim()}%"
            _summaries.value = accountingDao.getCustomerAccountSummaries(pattern)
        }
    }

    fun loadCustomer(customerId: Long) {
        viewModelScope.launch {
            _customer.value = customerDao.getById(customerId)
            _ledger.value = accountingDao.getLedgerForCustomer(customerId)
            _balance.value = accountingDao.getCustomerBalance(customerId)
            val orders = orderDao.getOrdersByCustomer(customerId).filter { it.status != OrderStatus.CANCELLED }
            val orderIds = orders.map { it.id }.filter { it != 0L }
            val paidByOrder =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
                }
            val lastPaymentByOrder =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    accountingDao.getLastPaymentDatesForOrders(orderIds)
                        .associate { it.orderId to it.lastPaymentAt }
                }
            val uiOrders =
                orders.map { order ->
                    val paidAmount = paidByOrder[order.id] ?: BigDecimal.ZERO
                    val paymentState =
                        when {
                            paidAmount <= BigDecimal.ZERO -> OrderPaymentState.UNPAID
                            paidAmount < order.totalAmount -> OrderPaymentState.PARTIAL
                            else -> OrderPaymentState.PAID
                        }
                    val effectiveStatus =
                        when (order.statusOverride) {
                            OrderStatusOverride.OPEN -> OrderEffectiveStatus.OPEN
                            OrderStatusOverride.CLOSED -> OrderEffectiveStatus.CLOSED
                            null ->
                                if (paidAmount >= order.totalAmount) {
                                    OrderEffectiveStatus.CLOSED
                                } else {
                                    OrderEffectiveStatus.OPEN
                                }
                        }
                    val orderDateMillis =
                        order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val lastPaymentAt = lastPaymentByOrder[order.id]
                    val lastActivityAt =
                        maxOf(orderDateMillis, order.updatedAt, lastPaymentAt ?: 0L)
                    CustomerOrderUi(
                        order = order,
                        paidAmount = paidAmount,
                        lastPaymentAt = lastPaymentAt,
                        lastActivityAt = lastActivityAt,
                        paymentState = paymentState,
                        effectiveStatus = effectiveStatus,
                        statusOverride = order.statusOverride
                    )
                }
            _orders.value = uiOrders.sortedByDescending { it.order.createdAt }
        }
    }

    fun recordPayment(
        customerId: Long,
        amount: BigDecimal,
        method: PaymentMethod,
        note: String,
        orderId: Long?
    ) {
        viewModelScope.launch {
            recordPaymentInternal(customerId, amount, method, note, orderId)
            loadCustomer(customerId)
        }
    }

    fun writeOffOrder(orderId: Long) {
        viewModelScope.launch {
            val order = orderDao.getOrderById(orderId) ?: return@launch
            if (!isOlderThanOneMonth(order.orderDate)) return@launch
            val paidSoFar = accountingDao.getPaidForOrder(orderId)
            val remaining = order.totalAmount - paidSoFar
            if (remaining <= BigDecimal.ZERO) return@launch

            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = orderId,
                    customerId = order.customerId,
                    type = EntryType.WRITE_OFF,
                    amount = remaining,
                    date = Clock.System.now().toEpochMilliseconds(),
                    description = "Bad debt write-off: Order #$orderId"
                )
            )
            orderDao.updateStatusOverride(orderId, null, Clock.System.now().toEpochMilliseconds())

            val customerId = _customer.value?.id ?: return@launch
            loadCustomer(customerId)
        }
    }

    fun setOrderStatusOverride(orderId: Long, statusOverride: OrderStatusOverride?) {
        viewModelScope.launch {
            orderDao.updateStatusOverride(orderId, statusOverride?.name, Clock.System.now().toEpochMilliseconds())
            val customerId = _customer.value?.id ?: return@launch
            loadCustomer(customerId)
        }
    }

    internal suspend fun recordPaymentInternal(
        customerId: Long,
        amount: BigDecimal,
        method: PaymentMethod,
        note: String,
        orderId: Long?
    ) {
        if (amount <= BigDecimal.ZERO) return

        val baseDescription = buildString {
            append("Payment (")
            append(method.name)
            append(")")
            if (note.isNotBlank()) {
                append(": ")
                append(note.trim())
            }
        }

        if (orderId == null) {
            allocatePaymentToOldestOrders(customerId, amount, baseDescription)
            return
        }

        val order = orderDao.getOrderById(orderId) ?: return
        val paidSoFar = accountingDao.getPaidForOrder(orderId)
        val remaining = order.totalAmount - paidSoFar

        if (remaining <= BigDecimal.ZERO) {
            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = null,
                    customerId = customerId,
                    type = EntryType.CREDIT,
                    amount = amount,
                    date = Clock.System.now().toEpochMilliseconds(),
                    description = "Extra payment for Order #$orderId"
                )
            )
            return
        }

        val appliedToOrder = if (amount > remaining) remaining else amount
        accountingDao.insertAccountEntry(
            AccountEntryEntity(
                orderId = orderId,
                customerId = customerId,
                type = EntryType.CREDIT,
                amount = appliedToOrder,
                date = Clock.System.now().toEpochMilliseconds(),
                description = baseDescription
            )
        )

        val excess = amount - appliedToOrder
        if (excess > BigDecimal.ZERO) {
            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = null,
                    customerId = customerId,
                    type = EntryType.CREDIT,
                    amount = excess,
                    date = Clock.System.now().toEpochMilliseconds(),
                    description = "Extra payment for Order #$orderId"
                )
            )
        }
    }

    private suspend fun allocatePaymentToOldestOrders(
        customerId: Long,
        amount: BigDecimal,
        description: String
    ) {
        var remainingPayment = amount
        val now = Clock.System.now().toEpochMilliseconds()
        val orders =
            orderDao.getOrdersByCustomer(customerId)
                .filter {
                    it.status != OrderStatus.CANCELLED &&
                        it.statusOverride != OrderStatusOverride.CLOSED
                }
                .sortedBy { it.orderDate }

        for (order in orders) {
            if (remainingPayment <= BigDecimal.ZERO) break
            val paidSoFar = accountingDao.getPaidForOrder(order.id)
            val remaining = order.totalAmount - paidSoFar
            if (remaining <= BigDecimal.ZERO) continue

            val applied = if (remainingPayment > remaining) remaining else remainingPayment
            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = order.id,
                    customerId = customerId,
                    type = EntryType.CREDIT,
                    amount = applied,
                    date = now,
                    description = description
                )
            )
            remainingPayment -= applied
        }

        if (remainingPayment > BigDecimal.ZERO) {
            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = null,
                    customerId = customerId,
                    type = EntryType.CREDIT,
                    amount = remainingPayment,
                    date = now,
                    description = "Extra payment"
                )
            )
        }
    }

    private fun isOlderThanOneMonth(orderDate: kotlinx.datetime.LocalDate): Boolean {
        val cutoff = orderDate.plus(1, DateTimeUnit.MONTH)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today >= cutoff
    }
}
