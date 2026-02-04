package com.zeynbakers.order_management_system.accounting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LedgerSummaryUi(
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
    val totalWriteOffs: BigDecimal,
    val totalReversals: BigDecimal,
    val netBalance: BigDecimal
)

data class LedgerEntryUi(
    val id: Long,
    val type: EntryType,
    val amount: BigDecimal,
    val date: Long,
    val description: String,
    val orderId: Long?,
    val orderLabel: String?,
    val customerLabel: String?
)

class LedgerViewModel(private val database: AppDatabase) : ViewModel() {
    private val accountingDao: AccountingDao = database.accountingDao()
    private val orderDao = database.orderDao()
    private val customerDao = database.customerDao()

    private val _summary = MutableStateFlow<LedgerSummaryUi?>(null)
    val summary = _summary.asStateFlow()

    private val _entries = MutableStateFlow<List<LedgerEntryUi>>(emptyList())
    val entries = _entries.asStateFlow()

    fun load(limit: Int = 250) {
        viewModelScope.launch {
            val entries = accountingDao.getRecentLedgerEntries(limit)
            val orderIds = entries.mapNotNull { it.orderId }.distinct()
            val ordersById =
                if (orderIds.isEmpty()) emptyMap()
                else orderDao.getOrdersByIds(orderIds).associateBy { it.id }
            val customerIds = buildList {
                addAll(entries.mapNotNull { it.customerId })
                addAll(ordersById.values.mapNotNull { it.customerId })
            }.distinct()
            val customersById =
                if (customerIds.isEmpty()) emptyMap()
                else customerDao.getByIds(customerIds).associateBy { it.id }

            _entries.value = entries.map { entry ->
                val order = entry.orderId?.let { ordersById[it] }
                val customerId = entry.customerId ?: order?.customerId
                val customer = customerId?.let { customersById[it] }
                LedgerEntryUi(
                    id = entry.id,
                    type = entry.type,
                    amount = entry.amount,
                    date = entry.date,
                    description = entry.description,
                    orderId = entry.orderId,
                    orderLabel = orderLabel(order, customer?.name),
                    customerLabel = customerLabel(customer, customerId)
                )
            }

            _summary.value = buildSummary(entries)
        }
    }

    private fun buildSummary(entries: List<AccountEntryEntity>): LedgerSummaryUi {
        var debits = BigDecimal.ZERO
        var credits = BigDecimal.ZERO
        var writeOffs = BigDecimal.ZERO
        var reversals = BigDecimal.ZERO
        entries.forEach { entry ->
            when (entry.type) {
                EntryType.DEBIT -> debits += entry.amount
                EntryType.CREDIT -> credits += entry.amount
                EntryType.WRITE_OFF -> writeOffs += entry.amount
                EntryType.REVERSAL -> reversals += entry.amount
            }
        }
        val net = debits - credits - writeOffs + reversals
        return LedgerSummaryUi(
            totalDebits = debits,
            totalCredits = credits,
            totalWriteOffs = writeOffs,
            totalReversals = reversals,
            netBalance = net
        )
    }

    private fun orderLabel(order: OrderEntity?, customerName: String?): String? {
        if (order == null) return null
        return formatOrderLabelWithId(
            orderId = order.id,
            date = order.orderDate,
            customerName = customerName,
            notes = order.notes,
            totalAmount = order.totalAmount
        )
    }

    private fun customerLabel(customer: CustomerEntity?, id: Long?): String? {
        return when {
            customer != null -> customer.name
            id != null -> "Customer #$id"
            else -> null
        }
    }
}
