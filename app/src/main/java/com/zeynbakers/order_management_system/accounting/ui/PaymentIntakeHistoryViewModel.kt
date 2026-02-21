package com.zeynbakers.order_management_system.accounting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

sealed class PaymentHistoryFilter {
    data object All : PaymentHistoryFilter()
    data class Customer(val customerId: Long) : PaymentHistoryFilter()
    data class Order(val orderId: Long) : PaymentHistoryFilter()
}

data class PaymentHistoryHeader(
    val title: String,
    val subtitle: String?
)

data class PaymentHistoryAllocationUi(
    val allocationId: Long,
    val orderId: Long?,
    val orderDate: LocalDate?,
    val orderNotes: String?,
    val amount: BigDecimal,
    val type: PaymentAllocationType,
    val status: PaymentAllocationStatus
)

data class PaymentHistoryItemUi(
    val receiptId: Long,
    val amount: BigDecimal,
    val displayAmount: BigDecimal,
    val secondaryAmount: BigDecimal?,
    val receivedAt: Long,
    val method: PaymentMethod,
    val transactionCode: String?,
    val senderName: String?,
    val senderPhone: String?,
    val rawText: String?,
    val customerId: Long?,
    val customerName: String?,
    val note: String?,
    val isBadDebt: Boolean,
    val status: PaymentReceiptStatus,
    val allocations: List<PaymentHistoryAllocationUi>,
    val missingLedgerCount: Int
)

data class PaymentHistoryActionResult(
    val success: Boolean,
    val message: String
)

data class MoveOrderOption(
    val orderId: Long,
    val label: String
)

class PaymentIntakeHistoryViewModel(private val database: AppDatabase) : ViewModel() {
    private val receiptDao = database.paymentReceiptDao()
    private val allocationDao = database.paymentAllocationDao()
    private val customerDao = database.customerDao()
    private val orderDao = database.orderDao()
    private val accountingDao = database.accountingDao()
    private val receiptProcessor = PaymentReceiptProcessor(database)

    private val _history = MutableStateFlow<List<PaymentHistoryItemUi>>(emptyList())
    val history = _history.asStateFlow()

    private val _header = MutableStateFlow<PaymentHistoryHeader?>(null)
    val header = _header.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load(filter: PaymentHistoryFilter) {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) { buildHistory(filter) }
                _header.value = result.header
                _history.value = result.items
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                _error.value = historyLoadErrorMessageOrNull(t)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun voidReceipt(receiptId: Long, reason: String?): PaymentHistoryActionResult {
        return withContext(Dispatchers.IO) {
            val applied = receiptProcessor.voidReceipt(receiptId, reason)
            if (applied) {
                PaymentHistoryActionResult(true, "Payment voided")
            } else {
                PaymentHistoryActionResult(false, "Payment not found or already voided")
            }
        }
    }

    suspend fun onVoidReceipt(receiptId: Long, reason: String?): PaymentHistoryActionResult {
        return voidReceipt(receiptId, reason)
    }

    suspend fun onMoveReceipt(
        receiptId: Long,
        targetAllocation: ReceiptAllocation
    ): PaymentHistoryActionResult {
        return withContext(Dispatchers.IO) {
            val receipt = receiptDao.getById(receiptId)
                ?: return@withContext PaymentHistoryActionResult(false, "Payment not found")
            val updated =
                receiptProcessor.reallocateReceipt(
                    receiptId = receiptId,
                    allocation = targetAllocation,
                    descriptionBase = buildReceiptDescription(receipt)
                )
            if (updated) {
                PaymentHistoryActionResult(true, "Payment moved")
            } else {
                PaymentHistoryActionResult(false, "Unable to move payment")
            }
        }
    }

    suspend fun loadMoveOrderOptions(customerId: Long): List<MoveOrderOption> {
        return withContext(Dispatchers.IO) {
            val orders =
                orderDao.getOrdersByCustomer(customerId)
                .filter {
                    it.status != com.zeynbakers.order_management_system.order.data.OrderStatus.CANCELLED &&
                        it.statusOverride != com.zeynbakers.order_management_system.order.data.OrderStatusOverride.CLOSED
                }
                .sortedWith(
                    compareBy<OrderEntity> { it.orderDate }
                        .thenBy { it.createdAt }
                        .thenBy { it.id }
                )
            val customerIds = orders.mapNotNull { it.customerId }.distinct()
            val customerNames =
                if (customerIds.isEmpty()) emptyMap()
                else customerDao.getByIds(customerIds).associate { it.id to it.name }
            orders.map { order ->
                val label =
                    formatOrderLabelWithId(
                        orderId = order.id,
                        date = order.orderDate,
                        customerName = order.customerId?.let { customerNames[it] },
                        notes = order.notes,
                        totalAmount = order.totalAmount
                    )
                MoveOrderOption(order.id, label)
            }
        }
    }

    suspend fun applyReceiptToOldestOrders(receiptId: Long): PaymentHistoryActionResult {
        return withContext(Dispatchers.IO) {
            val receipt = receiptDao.getById(receiptId)
                ?: return@withContext PaymentHistoryActionResult(false, "Payment not found")
            if (receipt.status != PaymentReceiptStatus.UNAPPLIED) {
                return@withContext PaymentHistoryActionResult(false, "Payment already used")
            }
            val customerId = receipt.customerId
                ?: return@withContext PaymentHistoryActionResult(false, "Missing customer")
            receiptProcessor.createAndApplyReceipt(
                receipt = receipt,
                descriptionBase = buildReceiptDescription(receipt),
                allocation = ReceiptAllocation.OldestOrders(customerId)
            )
            PaymentHistoryActionResult(true, "Used on oldest orders")
        }
    }

    private suspend fun buildHistory(filter: PaymentHistoryFilter): HistoryResult {
        val receipts =
            when (filter) {
                PaymentHistoryFilter.All -> receiptDao.getAll()
                is PaymentHistoryFilter.Customer -> receiptDao.getByCustomerId(filter.customerId)
                is PaymentHistoryFilter.Order -> receiptDao.getByOrderId(filter.orderId)
            }

        val header = buildHeader(filter)
        if (receipts.isEmpty()) {
            return HistoryResult(header = header, items = emptyList())
        }

        val receiptIds = receipts.map { it.id }
        val allocations = allocationDao.getByReceiptIds(receiptIds)
        val allocationsByReceipt = allocations.groupBy { it.receiptId }

        val orderIds = allocations.mapNotNull { it.orderId }.distinct()
        val ordersById =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                orderDao.getOrdersByIds(orderIds).associateBy { it.id }
            }
        val customerIds = buildList {
            addAll(receipts.mapNotNull { it.customerId })
            addAll(ordersById.values.mapNotNull { it.customerId })
        }.distinct()
        val resolvedCustomersById =
            if (customerIds.isEmpty()) {
                emptyMap()
            } else {
                customerDao.getByIds(customerIds).associateBy { it.id }
            }
        val entryIds = allocations.mapNotNull { it.accountEntryId }.distinct()
        val entriesById =
            if (entryIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getEntriesByIds(entryIds).associateBy { it.id }
            }

        val items =
            receipts
                .sortedWith(compareByDescending<PaymentReceiptEntity> { it.receivedAt }.thenByDescending { it.id })
                .map { receipt ->
                    val receiptAllocations = allocationsByReceipt[receipt.id].orEmpty()
                    val orderAllocationTotal =
                        if (filter is PaymentHistoryFilter.Order) {
                            receiptAllocations
                                .filter { it.status == PaymentAllocationStatus.APPLIED && it.orderId == filter.orderId }
                                .fold(BigDecimal.ZERO) { acc, allocation -> acc + allocation.amount }
                        } else {
                            null
                        }
                    val resolvedCustomerId =
                        receipt.customerId
                            ?: receiptAllocations.mapNotNull { allocation ->
                                allocation.orderId?.let { ordersById[it]?.customerId }
                            }.firstOrNull()
                    val customerName = resolvedCustomerId?.let { resolvedCustomersById[it]?.name }
                    val allocationUi =
                        receiptAllocations.map { allocation ->
                            val order = allocation.orderId?.let { ordersById[it] }
                            PaymentHistoryAllocationUi(
                                allocationId = allocation.id,
                                orderId = allocation.orderId,
                                orderDate = order?.orderDate,
                                orderNotes = order?.notes,
                                amount = allocation.amount,
                                type = allocation.type,
                                status = allocation.status
                            )
                        }
                    val missingLedgerCount =
                        receiptAllocations.count { allocation ->
                            allocation.status == PaymentAllocationStatus.APPLIED &&
                                (allocation.accountEntryId == null || entriesById[allocation.accountEntryId] == null)
                        }
                    val isBadDebt =
                        receiptAllocations.any { allocation ->
                            val entry = allocation.accountEntryId?.let { entriesById[it] }
                            entry?.type == EntryType.WRITE_OFF
                        }
                    val displayAmount =
                        orderAllocationTotal?.takeIf { it > BigDecimal.ZERO } ?: receipt.amount
                    val secondaryAmount =
                        if (displayAmount != receipt.amount) receipt.amount else null
                    PaymentHistoryItemUi(
                        receiptId = receipt.id,
                        amount = receipt.amount,
                        displayAmount = displayAmount,
                        secondaryAmount = secondaryAmount,
                        receivedAt = receipt.receivedAt,
                        method = receipt.method,
                        transactionCode = receipt.transactionCode,
                        senderName = receipt.senderName,
                        senderPhone = receipt.senderPhone,
                        rawText = receipt.rawText,
                        customerId = resolvedCustomerId,
                        customerName = customerName,
                        note = receipt.note,
                        isBadDebt = isBadDebt,
                        status = receipt.status,
                        allocations = allocationUi,
                        missingLedgerCount = missingLedgerCount
                    )
                }

        return HistoryResult(header = header, items = items)
    }

    private suspend fun buildHeader(filter: PaymentHistoryFilter): PaymentHistoryHeader? {
        return when (filter) {
            PaymentHistoryFilter.All -> null
            is PaymentHistoryFilter.Customer -> {
                val customer = customerDao.getById(filter.customerId)
                if (customer == null) {
                    PaymentHistoryHeader(
                        title = "Customer payments",
                        subtitle = "Customer #${filter.customerId}"
                    )
                } else {
                    val subtitle = customer.phone.takeIf { it.isNotBlank() }?.let { "${customer.name} - $it" }
                        ?: customer.name
                    PaymentHistoryHeader(
                        title = "Customer payments",
                        subtitle = subtitle
                    )
                }
            }
            is PaymentHistoryFilter.Order -> {
                val order = orderDao.getOrderById(filter.orderId)
                val label =
                    if (order == null) {
                        "Order ID ${filter.orderId}"
                    } else {
                        formatOrderLabelWithId(
                            orderId = order.id,
                            date = order.orderDate,
                            customerName = order.customerId?.let { customerDao.getById(it)?.name },
                            notes = order.notes,
                            totalAmount = order.totalAmount
                        )
                    }
                val subtitle = label
                PaymentHistoryHeader(
                    title = "Order payments",
                    subtitle = subtitle
                )
            }
        }
    }

    private fun buildReceiptDescription(receipt: PaymentReceiptEntity): String {
        return when (receipt.method) {
            PaymentMethod.MPESA -> buildMpesaDescription(receipt)
            PaymentMethod.CASH -> buildManualDescription(receipt)
        }
    }

    private fun buildMpesaDescription(receipt: PaymentReceiptEntity): String {
        return buildString {
            append("M-PESA")
            if (!receipt.transactionCode.isNullOrBlank()) {
                append(" ")
                append(receipt.transactionCode)
            }
            val sender = receipt.senderName?.takeIf { it.isNotBlank() }
                ?: receipt.senderPhone?.takeIf { it.isNotBlank() }
            if (sender != null) {
                append(" from ")
                append(sender)
            }
        }
    }

    private fun buildManualDescription(receipt: PaymentReceiptEntity): String {
        return buildString {
            append("Payment (")
            append(receipt.method.name)
            append(")")
            if (!receipt.note.isNullOrBlank()) {
                append(": ")
                append(receipt.note.trim())
            }
        }
    }

    private data class HistoryResult(
        val header: PaymentHistoryHeader?,
        val items: List<PaymentHistoryItemUi>
    )
}

internal fun historyLoadErrorMessageOrNull(throwable: Throwable): String? {
    if (throwable is CancellationException) return null
    return throwable.message ?: "Unable to load payment history."
}
