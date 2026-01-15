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
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
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
            _orders.value = orderDao.getOrdersByCustomer(customerId).sortedByDescending { it.orderDate }
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
                .filter { it.status != OrderStatus.CANCELLED }
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
}
