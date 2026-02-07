package com.zeynbakers.order_management_system.accounting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.accounting.mpesa.MpesaParsedTransaction
import com.zeynbakers.order_management_system.accounting.mpesa.MpesaParser
import com.zeynbakers.order_management_system.accounting.mpesa.computeMpesaHash
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.expandPhoneCandidates
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

data class MpesaOrderSuggestion(
    val orderId: Long,
    val customerId: Long?,
    val customerName: String?,
    val orderDate: kotlinx.datetime.LocalDate,
    val notes: String,
    val outstanding: BigDecimal,
    val confidence: Int
)

enum class DuplicateState {
    NONE,
    EXISTING,
    INTAKE
}

enum class AllocationMode {
    ORDER,
    CUSTOMER_CREDIT,
    OLDEST_ORDERS
}

data class MpesaTransactionUi(
    val key: String,
    val transactionCode: String?,
    val amount: BigDecimal,
    val senderName: String?,
    val senderPhone: String?,
    val receivedAt: Long?,
    val rawText: String,
    val duplicateState: DuplicateState,
    val existingReceiptId: Long?,
    val existingReceiptStatus: PaymentReceiptStatus?,
    val suggestedCustomerId: Long?,
    val suggestedCustomerName: String?,
    val customerConfidence: Int,
    val orderSuggestions: List<MpesaOrderSuggestion>,
    val selectedOrderId: Long?,
    val selectedCustomerId: Long?,
    val allocationMode: AllocationMode,
    val selected: Boolean
)

data class PaymentApplySummary(
    val applied: Int,
    val existingDuplicates: Int,
    val intakeDuplicates: Int,
    val skippedNoCustomer: Int
)

data class ExistingReceiptAllocationUi(
    val orderId: Long?,
    val orderLabel: String?,
    val amount: BigDecimal,
    val status: PaymentAllocationStatus,
    val voidReason: String?
)

data class ExistingReceiptDetails(
    val receiptId: Long,
    val amount: BigDecimal,
    val receivedAt: Long,
    val method: PaymentMethod,
    val transactionCode: String?,
    val status: PaymentReceiptStatus,
    val voidReason: String?,
    val allocations: List<ExistingReceiptAllocationUi>
)

data class ReceiptActionResult(
    val success: Boolean,
    val message: String
)

data class CustomerSuggestionUi(
    val id: Long,
    val name: String,
    val phone: String
)

class PaymentIntakeViewModel(private val database: AppDatabase) : ViewModel() {
    private val accountingDao = database.accountingDao()
    private val receiptDao = database.paymentReceiptDao()
    private val allocationDao = database.paymentAllocationDao()
    private val orderDao = database.orderDao()
    private val customerDao = database.customerDao()
    private val receiptProcessor = PaymentReceiptProcessor(database)

    private val _rawText = MutableStateFlow("")
    val rawText = _rawText.asStateFlow()

    private val _transactions = MutableStateFlow<List<MpesaTransactionUi>>(emptyList())
    val transactions = _transactions.asStateFlow()
    private var parseJob: Job? = null

    fun setRawText(text: String) {
        _rawText.value = text
        parse(text)
    }

    fun appendRawText(text: String) {
        if (text.isBlank()) return
        val combined =
            if (_rawText.value.isBlank()) {
                text.trim()
            } else {
                "${_rawText.value.trim()}\n\n${text.trim()}"
            }
        setRawText(combined)
    }

    fun toggleSelected(key: String, selected: Boolean) {
        setSelected(key, selected)
    }

    fun setSelected(key: String, selected: Boolean) {
        _transactions.value =
            _transactions.value.map { item ->
                if (item.key != key) {
                    item
                } else {
                    val canSelect = item.isReadySelectable()
                    item.copy(selected = if (selected) canSelect else false)
                }
            }
    }

    fun selectReadyOnly() {
        _transactions.value =
            _transactions.value.map { item ->
                item.copy(selected = item.isReadySelectable())
            }
    }

    fun setAllSelected(selected: Boolean) {
        _transactions.value =
            _transactions.value.map { item ->
                val shouldSelect = selected && item.isReadySelectable()
                item.copy(selected = shouldSelect)
            }
    }

    fun selectOrder(key: String, orderId: Long?) {
        _transactions.value =
            _transactions.value.map { item ->
                if (item.key != key) return@map item
                val selectedOrder =
                    item.orderSuggestions.firstOrNull { it.orderId == orderId }
                val resolvedCustomerId =
                    if (orderId == null) {
                        item.suggestedCustomerId
                    } else {
                        selectedOrder?.customerId
                    }
                val canApply = resolvedCustomerId != null || selectedOrder != null
                val hasUnpaidOrders = item.orderSuggestions.isNotEmpty()
                val allocationMode =
                    if (orderId != null) {
                        AllocationMode.ORDER
                    } else if (resolvedCustomerId != null && !hasUnpaidOrders) {
                        AllocationMode.CUSTOMER_CREDIT
                    } else {
                        AllocationMode.OLDEST_ORDERS
                    }
                item.copy(
                    selectedOrderId = orderId,
                    selectedCustomerId = resolvedCustomerId,
                    allocationMode = allocationMode,
                    selected = item.duplicateState == DuplicateState.NONE && canApply
                )
            }
    }

    fun selectAllocationMode(key: String, mode: AllocationMode) {
        _transactions.value =
            _transactions.value.map { item ->
                if (item.key != key) return@map item
                val resolvedCustomerId =
                    when (mode) {
                        AllocationMode.ORDER -> item.selectedOrderId?.let { orderId ->
                            item.orderSuggestions.firstOrNull { it.orderId == orderId }?.customerId
                        }
                        AllocationMode.CUSTOMER_CREDIT,
                        AllocationMode.OLDEST_ORDERS -> item.suggestedCustomerId
                    }
                val canApply =
                    when (mode) {
                        AllocationMode.ORDER -> item.selectedOrderId != null
                        AllocationMode.CUSTOMER_CREDIT,
                        AllocationMode.OLDEST_ORDERS -> resolvedCustomerId != null
                    }
                item.copy(
                    allocationMode = mode,
                    selectedOrderId = if (mode == AllocationMode.ORDER) item.selectedOrderId else null,
                    selectedCustomerId = resolvedCustomerId,
                    selected = item.duplicateState == DuplicateState.NONE && canApply
                )
            }
    }

    fun selectCustomer(key: String, customerId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = _transactions.value.firstOrNull { it.key == key } ?: return@launch
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val forcedCustomer =
                customerId?.let { id -> customerDao.getById(id) }
            val suggestion =
                if (forcedCustomer != null) {
                    suggestForTransaction(
                        transaction = item.toParsedTransaction(),
                        today = today,
                        forcedCustomer = forcedCustomer
                    )
                } else {
                    SuggestionResult(
                        customer = null,
                        customerConfidence = 0,
                        orderSuggestions = emptyList(),
                        orderById = emptyMap(),
                        selectedOrderId = null
                    )
                }
            val selectedOrderId = suggestion.selectedOrderId
            val selectedCustomerId = forcedCustomer?.id
            val hasUnpaidOrders = suggestion.orderSuggestions.isNotEmpty()
            val allocationMode =
                when {
                    selectedOrderId != null -> AllocationMode.ORDER
                    selectedCustomerId != null && !hasUnpaidOrders -> AllocationMode.CUSTOMER_CREDIT
                    selectedCustomerId != null -> AllocationMode.OLDEST_ORDERS
                    else -> AllocationMode.OLDEST_ORDERS
                }
            val canApply =
                when (allocationMode) {
                    AllocationMode.ORDER -> selectedOrderId != null
                    AllocationMode.CUSTOMER_CREDIT,
                    AllocationMode.OLDEST_ORDERS -> selectedCustomerId != null
                }
            _transactions.update { latest ->
                latest.map { entry ->
                    if (entry.key != key) {
                        entry
                    } else {
                        entry.copy(
                            suggestedCustomerId = forcedCustomer?.id,
                            suggestedCustomerName = forcedCustomer?.name,
                            customerConfidence = if (forcedCustomer != null) 100 else 0,
                            orderSuggestions = suggestion.orderSuggestions,
                            selectedOrderId = selectedOrderId,
                            selectedCustomerId = selectedCustomerId,
                            allocationMode = allocationMode,
                            selected = entry.duplicateState == DuplicateState.NONE && canApply
                        )
                    }
                }
            }
        }
    }

    suspend fun searchCustomers(query: String): List<CustomerSuggestionUi> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()
        val pattern = "%$trimmed%"
        return withContext(Dispatchers.IO) {
            customerDao.searchCustomers(pattern).map { customer ->
                CustomerSuggestionUi(
                    id = customer.id,
                    name = customer.name,
                    phone = customer.phone
                )
            }
        }
    }

    suspend fun reallocateExistingReceipt(key: String): ReceiptActionResult {
        val item = _transactions.value.firstOrNull { it.key == key }
            ?: return ReceiptActionResult(false, "Receipt not found")
        val receiptId = item.existingReceiptId
            ?: return ReceiptActionResult(false, "Receipt not found")
        val allocation = buildAllocation(item) ?: return ReceiptActionResult(false, "Select a customer or order")
        return withContext(Dispatchers.IO) {
            val success =
                receiptProcessor.reallocateReceipt(
                    receiptId = receiptId,
                    allocation = allocation,
                    descriptionBase = buildDescription(item)
                )
            if (success) {
                parse(_rawText.value)
                ReceiptActionResult(true, "Receipt updated")
            } else {
                ReceiptActionResult(false, "Unable to update receipt")
            }
        }
    }

    suspend fun loadExistingReceiptDetails(receiptId: Long): ExistingReceiptDetails? {
        return withContext(Dispatchers.IO) {
            val receipt = receiptDao.getById(receiptId) ?: return@withContext null
            val allocationEntities = allocationDao.getByReceiptId(receiptId)
            val orderIds = allocationEntities.mapNotNull { it.orderId }.distinct()
            val ordersById =
                if (orderIds.isEmpty()) emptyMap()
                else orderDao.getOrdersByIds(orderIds).associateBy { it.id }
            val customerIds = ordersById.values.mapNotNull { it.customerId }.distinct()
            val customersById =
                if (customerIds.isEmpty()) emptyMap()
                else customerDao.getByIds(customerIds).associateBy { it.id }
            val allocations =
                allocationEntities.map { allocation ->
                    val order = allocation.orderId?.let { ordersById[it] }
                    val customerName = order?.customerId?.let { customersById[it]?.name }
                    val orderLabel =
                        if (order == null) {
                            null
                        } else {
                            com.zeynbakers.order_management_system.core.util.formatOrderLabel(
                                date = order.orderDate,
                                customerName = customerName,
                                notes = order.notes,
                                totalAmount = order.totalAmount
                            )
                        }
                    ExistingReceiptAllocationUi(
                        orderId = allocation.orderId,
                        orderLabel = orderLabel,
                        amount = allocation.amount,
                        status = allocation.status,
                        voidReason = allocation.voidReason
                    )
                }
            ExistingReceiptDetails(
                receiptId = receipt.id,
                amount = receipt.amount,
                receivedAt = receipt.receivedAt,
                method = receipt.method,
                transactionCode = receipt.transactionCode,
                status = receipt.status,
                voidReason = receipt.voidReason,
                allocations = allocations
            )
        }
    }

    suspend fun applySelected(): PaymentApplySummary {
        val current = _transactions.value
        val toApply =
            current.filter { it.selected && it.duplicateState == DuplicateState.NONE }
        if (toApply.isEmpty()) {
            return PaymentApplySummary(
                applied = 0,
                existingDuplicates = current.count { it.duplicateState == DuplicateState.EXISTING },
                intakeDuplicates = current.count { it.duplicateState == DuplicateState.INTAKE },
                skippedNoCustomer = 0
            )
        }

        var applied = 0
        var skippedNoCustomer = 0
        val now = Clock.System.now().toEpochMilliseconds()

        withContext(Dispatchers.IO) {
            val orderIds = toApply.mapNotNull { it.selectedOrderId }.distinct()
            val ordersById =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    orderDao.getOrdersByIds(orderIds).associateBy { it.id }
                }
            toApply.forEach { item ->
                val allocation = buildAllocation(item)
                if (allocation == null) {
                    skippedNoCustomer += 1
                    return@forEach
                }
                val receivedAt = item.receivedAt ?: now
                val hash =
                    computeMpesaHash(
                        amount = item.amount,
                        receivedAt = receivedAt,
                        senderPhone = item.senderPhone,
                        transactionCode = item.transactionCode,
                        rawText = item.rawText
                    )
                val resolvedCustomerId =
                    when (allocation) {
                        is ReceiptAllocation.Order ->
                            item.selectedCustomerId ?: ordersById[allocation.orderId]?.customerId
                        is ReceiptAllocation.OldestOrders -> allocation.customerId
                        is ReceiptAllocation.CustomerCredit -> allocation.customerId
                        ReceiptAllocation.Unapplied -> item.selectedCustomerId
                    }
                val receipt =
                    receiptProcessor.createReceipt(
                        amount = item.amount,
                        receivedAt = receivedAt,
                        method = PaymentMethod.MPESA,
                        transactionCode = item.transactionCode,
                        hash = hash,
                        senderName = item.senderName,
                        senderPhone = item.senderPhone,
                        rawText = item.rawText,
                        customerId = resolvedCustomerId,
                        note = null
                    )
                receiptProcessor.createAndApplyReceipt(
                    receipt = receipt,
                    descriptionBase = buildDescription(item),
                    allocation = allocation
                )
                applied += 1
            }
        }

        parse(_rawText.value)
        return PaymentApplySummary(
            applied = applied,
            existingDuplicates = current.count { it.duplicateState == DuplicateState.EXISTING },
            intakeDuplicates = current.count { it.duplicateState == DuplicateState.INTAKE },
            skippedNoCustomer = skippedNoCustomer
        )
    }

    suspend fun applySingle(key: String): ReceiptActionResult {
        val item = _transactions.value.firstOrNull { it.key == key }
            ?: return ReceiptActionResult(false, "Receipt not found")
        if (item.duplicateState != DuplicateState.NONE) {
            return ReceiptActionResult(false, "Already recorded")
        }
        val allocation = buildAllocation(item) ?: return ReceiptActionResult(false, "Select a customer or order")
        val now = Clock.System.now().toEpochMilliseconds()
        val receivedAt = item.receivedAt ?: now
        val hash =
            computeMpesaHash(
                amount = item.amount,
                receivedAt = receivedAt,
                senderPhone = item.senderPhone,
                transactionCode = item.transactionCode,
                rawText = item.rawText
            )
        return withContext(Dispatchers.IO) {
            val resolvedCustomerId =
                when (allocation) {
                    is ReceiptAllocation.Order ->
                        orderDao.getOrderById(allocation.orderId)?.customerId ?: item.selectedCustomerId
                    is ReceiptAllocation.OldestOrders -> allocation.customerId
                    is ReceiptAllocation.CustomerCredit -> allocation.customerId
                    ReceiptAllocation.Unapplied -> item.selectedCustomerId
                }
            val receipt =
                receiptProcessor.createReceipt(
                    amount = item.amount,
                    receivedAt = receivedAt,
                    method = PaymentMethod.MPESA,
                    transactionCode = item.transactionCode,
                    hash = hash,
                    senderName = item.senderName,
                    senderPhone = item.senderPhone,
                    rawText = item.rawText,
                    customerId = resolvedCustomerId,
                    note = null
                )
            receiptProcessor.createAndApplyReceipt(
                receipt = receipt,
                descriptionBase = buildDescription(item),
                allocation = allocation
            )
            parse(_rawText.value)
            ReceiptActionResult(true, "Payment applied")
        }
    }

    private fun parse(rawText: String) {
        parseJob?.cancel()
        parseJob =
            viewModelScope.launch(Dispatchers.IO) {
            val parsed = MpesaParser.parse(rawText)
            val ui = buildUi(parsed)
            _transactions.value = ui
            }
    }

    private suspend fun buildUi(parsed: List<MpesaParsedTransaction>): List<MpesaTransactionUi> {
        if (parsed.isEmpty()) return emptyList()
        val now = Clock.System.now().toEpochMilliseconds()
        val codes = parsed.mapNotNull { it.transactionCode }.distinct()
        val hashes =
            parsed.map { tx ->
                val receivedAt = tx.receivedAt ?: now
                computeMpesaHash(
                    amount = tx.amount,
                    receivedAt = receivedAt,
                    senderPhone = tx.senderPhone,
                    transactionCode = tx.transactionCode,
                    rawText = tx.rawText
                )
            }
        val existingByCode =
            if (codes.isEmpty()) {
                emptyMap()
            } else {
                receiptDao.getByCodes(codes).associateBy { it.transactionCode }
            }
        val existingByHash = receiptDao.getByHashes(hashes).associateBy { it.hash }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val seenKeys = mutableSetOf<String>()
        val keyOccurrences = mutableMapOf<String, Int>()
        return parsed.map { tx ->
            val receivedAt = tx.receivedAt ?: now
            val hash =
                computeMpesaHash(
                    amount = tx.amount,
                    receivedAt = receivedAt,
                    senderPhone = tx.senderPhone,
                    transactionCode = tx.transactionCode,
                    rawText = tx.rawText
                )
            val existingReceipt =
                if (tx.transactionCode != null) {
                    existingByCode[tx.transactionCode]
                } else {
                    existingByHash[hash]
                }
            val dedupeKey = tx.transactionCode ?: hash
            val intakeDuplicate = !seenKeys.add(dedupeKey)
            val duplicateState =
                when {
                    existingReceipt != null -> DuplicateState.EXISTING
                    intakeDuplicate -> DuplicateState.INTAKE
                    else -> DuplicateState.NONE
                }
            val suggestion = suggestForTransaction(tx, today)
            val selectedOrderId = suggestion.selectedOrderId
            val selectedCustomerId =
                if (selectedOrderId != null) {
                    suggestion.orderById[selectedOrderId]?.customerId
                } else {
                    suggestion.customer?.id
                }
            val hasUnpaidOrders = suggestion.orderSuggestions.isNotEmpty()
            val allocationMode =
                when {
                    selectedOrderId != null -> AllocationMode.ORDER
                    selectedCustomerId != null && !hasUnpaidOrders -> AllocationMode.CUSTOMER_CREDIT
                    else -> AllocationMode.OLDEST_ORDERS
                }
            val canApply =
                when (allocationMode) {
                    AllocationMode.ORDER -> selectedOrderId != null
                    AllocationMode.CUSTOMER_CREDIT,
                    AllocationMode.OLDEST_ORDERS -> selectedCustomerId != null
                }
            val occurrence = keyOccurrences[dedupeKey] ?: 0
            keyOccurrences[dedupeKey] = occurrence + 1
            MpesaTransactionUi(
                key = "$dedupeKey#$occurrence",
                transactionCode = tx.transactionCode,
                amount = tx.amount,
                senderName = tx.senderName,
                senderPhone = tx.senderPhone,
                receivedAt = tx.receivedAt,
                rawText = tx.rawText,
                duplicateState = duplicateState,
                existingReceiptId = existingReceipt?.id,
                existingReceiptStatus = existingReceipt?.status,
                suggestedCustomerId = suggestion.customer?.id,
                suggestedCustomerName = suggestion.customer?.name,
                customerConfidence = suggestion.customerConfidence,
                orderSuggestions = suggestion.orderSuggestions,
                selectedOrderId = selectedOrderId,
                selectedCustomerId = selectedCustomerId,
                allocationMode = allocationMode,
                selected = duplicateState == DuplicateState.NONE && canApply
            )
        }
    }

    private suspend fun suggestForTransaction(
        transaction: MpesaParsedTransaction,
        today: kotlinx.datetime.LocalDate,
        forcedCustomer: com.zeynbakers.order_management_system.customer.data.CustomerEntity? = null
    ): SuggestionResult {
        val customer = forcedCustomer ?: findCustomerByPhone(transaction.senderPhone)
        val candidateOrders =
            if (customer != null) {
                orderDao.getOrdersByCustomer(customer.id)
            } else {
                orderDao.getActiveOrders()
            }
        val activeOrders =
            candidateOrders.filter {
                it.status != OrderStatus.CANCELLED && it.statusOverride != OrderStatusOverride.CLOSED
            }
        val orderIds = activeOrders.map { it.id }.filter { it != 0L }
        val paidByOrder =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
            }
        val unpaidOrders =
            activeOrders.mapNotNull { order ->
                val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                val outstanding = order.totalAmount.subtract(paid)
                if (outstanding > BigDecimal.ZERO) UnpaidOrder(order, outstanding) else null
            }

        val referenceDate =
            transaction.receivedAt?.let {
                Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
            } ?: today
        val orderedUnpaid =
            unpaidOrders
                .sortedWith(
                    compareBy<UnpaidOrder> { it.order.orderDate }
                        .thenBy { it.order.createdAt }
                        .thenBy { it.order.id }
                )

        val customerUnpaidTotal =
            if (customer != null) {
                unpaidOrders.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.outstanding }
            } else {
                null
            }

        val suggestions =
            orderedUnpaid.map { entry ->
                val daysSince = max(0, entry.order.orderDate.daysUntil(referenceDate))
                val confidence =
                    computeConfidence(
                        amount = transaction.amount,
                        outstanding = entry.outstanding,
                        customerMatched = customer != null,
                        totalUnpaid = customerUnpaidTotal,
                        daysSince = daysSince
                    )
                MpesaOrderSuggestion(
                    orderId = entry.order.id,
                    customerId = entry.order.customerId,
                    customerName = customer?.name,
                    orderDate = entry.order.orderDate,
                    notes = entry.order.notes,
                    outstanding = entry.outstanding,
                    confidence = confidence
                )
            }
                .take(3)

        val orderById = suggestions.associateBy { it.orderId }
        val selectedOrderId: Long? = null

        val confidence =
            when {
                forcedCustomer != null -> 100
                customer != null -> 80
                else -> 0
            }
        return SuggestionResult(
            customer = customer,
            customerConfidence = confidence,
            orderSuggestions = suggestions,
            orderById = orderById,
            selectedOrderId = selectedOrderId
        )
    }

    private suspend fun findCustomerByPhone(senderPhone: String?): com.zeynbakers.order_management_system.customer.data.CustomerEntity? {
        if (senderPhone.isNullOrBlank()) return null
        val candidates = expandPhoneCandidates(senderPhone)
        for (candidate in candidates) {
            val found = customerDao.getByPhone(candidate)
            if (found != null) return found
        }
        return null
    }

    private fun computeConfidence(
        amount: BigDecimal,
        outstanding: BigDecimal,
        customerMatched: Boolean,
        totalUnpaid: BigDecimal?,
        daysSince: Int
    ): Int {
        var score = 0
        if (customerMatched) score += 35
        val amountComparison = amount.compareTo(outstanding)
        when {
            amountComparison == 0 -> score += 40
            amountComparison < 0 -> score += 20
        }
        if (totalUnpaid != null && amount.compareTo(totalUnpaid) == 0) {
            score += 15
        }
        score += max(0, 20 - daysSince)
        return min(score, 100)
    }

    private fun buildDescription(item: MpesaTransactionUi): String {
        return buildString {
            append("M-PESA")
            if (!item.transactionCode.isNullOrBlank()) {
                append(" ")
                append(item.transactionCode)
            }
            val sender =
                item.senderName?.takeIf { it.isNotBlank() } ?: item.senderPhone?.takeIf { it.isNotBlank() }
            if (sender != null) {
                append(" from ")
                append(sender)
            }
        }
    }

    private fun buildAllocation(item: MpesaTransactionUi): ReceiptAllocation? {
        return when (item.allocationMode) {
            AllocationMode.ORDER -> {
                val orderId = item.selectedOrderId ?: return null
                ReceiptAllocation.Order(orderId)
            }
            AllocationMode.OLDEST_ORDERS -> {
                val customerId = item.selectedCustomerId ?: return null
                ReceiptAllocation.OldestOrders(customerId)
            }
            AllocationMode.CUSTOMER_CREDIT -> {
                val customerId = item.selectedCustomerId ?: return null
                ReceiptAllocation.CustomerCredit(customerId)
            }
        }
    }

    private data class UnpaidOrder(val order: OrderEntity, val outstanding: BigDecimal)

    private data class SuggestionResult(
        val customer: com.zeynbakers.order_management_system.customer.data.CustomerEntity?,
        val customerConfidence: Int,
        val orderSuggestions: List<MpesaOrderSuggestion>,
        val orderById: Map<Long, MpesaOrderSuggestion>,
        val selectedOrderId: Long?
    )
}

private fun MpesaTransactionUi.toParsedTransaction(): MpesaParsedTransaction {
    return MpesaParsedTransaction(
        transactionCode = transactionCode,
        amount = amount,
        senderName = senderName,
        senderPhone = senderPhone,
        receivedAt = receivedAt,
        rawText = rawText
    )
}
