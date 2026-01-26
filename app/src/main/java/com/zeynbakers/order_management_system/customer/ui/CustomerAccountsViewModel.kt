package com.zeynbakers.order_management_system.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
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

data class CustomerFinanceSummary(
    val orderTotal: BigDecimal,
    val paidToOrders: BigDecimal,
    val availableCredit: BigDecimal,
    val netBalance: BigDecimal
)

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

    private val _orderLabels = MutableStateFlow<Map<Long, String>>(emptyMap())
    val orderLabels = _orderLabels.asStateFlow()

    private val _balance = MutableStateFlow(BigDecimal.ZERO)
    val balance = _balance.asStateFlow()

    private val _financeSummary = MutableStateFlow<CustomerFinanceSummary?>(null)
    val financeSummary = _financeSummary.asStateFlow()

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
            val financeTotals = accountingDao.getCustomerFinanceTotals(customerId)
            val orderTotal = financeTotals.orderBilled ?: BigDecimal.ZERO
            val paidToOrders = financeTotals.orderPaid ?: BigDecimal.ZERO
            val availableCredit = financeTotals.extraCredit ?: BigDecimal.ZERO
            val netBalance = orderTotal - paidToOrders - availableCredit
            _financeSummary.value =
                CustomerFinanceSummary(
                    orderTotal = orderTotal,
                    paidToOrders = paidToOrders,
                    availableCredit = availableCredit,
                    netBalance = netBalance
                )
            val allOrders = orderDao.getOrdersByCustomer(customerId)
            val customerName = _customer.value?.name
            _orderLabels.value =
                allOrders.associate { order ->
                    order.id to
                        formatOrderLabelWithId(
                            orderId = order.id,
                            date = order.orderDate,
                            customerName = customerName,
                            notes = order.notes,
                            totalAmount = order.totalAmount
                        )
                }
            val orders = allOrders.filter { it.status != OrderStatus.CANCELLED }
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
                    val paidComparison = paidAmount.compareTo(order.totalAmount)
                    val paymentState =
                        when {
                            paidAmount <= BigDecimal.ZERO -> OrderPaymentState.UNPAID
                            paidComparison < 0 -> OrderPaymentState.PARTIAL
                            paidComparison == 0 -> OrderPaymentState.PAID
                            else -> OrderPaymentState.OVERPAID
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
            _orders.value =
                uiOrders.sortedWith(
                    compareByDescending<CustomerOrderUi> { it.lastActivityAt }
                        .thenByDescending { it.order.createdAt }
                )
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

            val customerName = _customer.value?.name
            val orderLabel =
                formatOrderLabelWithId(
                    orderId = orderId,
                    date = order.orderDate,
                    customerName = customerName,
                    notes = order.notes,
                    totalAmount = order.totalAmount
                )
            accountingDao.insertAccountEntry(
                AccountEntryEntity(
                    orderId = orderId,
                    customerId = order.customerId,
                    type = EntryType.WRITE_OFF,
                    amount = remaining,
                    date = Clock.System.now().toEpochMilliseconds(),
                    description = "Bad debt write-off: $orderLabel"
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

        val processor = PaymentReceiptProcessor(database)
        val now = Clock.System.now().toEpochMilliseconds()
        val receipt =
            processor.createReceipt(
                amount = amount,
                receivedAt = now,
                method = method,
                transactionCode = null,
                hash = null,
                senderName = null,
                senderPhone = null,
                rawText = null,
                customerId = customerId,
                note = note.takeIf { it.isNotBlank() }
            )
        val allocation =
            if (orderId == null) {
                ReceiptAllocation.OldestOrders(customerId)
            } else {
                ReceiptAllocation.Order(orderId)
            }
        processor.createAndApplyReceipt(
            receipt = receipt,
            descriptionBase = baseDescription,
            allocation = allocation
        )
    }

    private fun isOlderThanOneMonth(orderDate: kotlinx.datetime.LocalDate): Boolean {
        val cutoff = orderDate.plus(1, DateTimeUnit.MONTH)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today >= cutoff
    }
}
