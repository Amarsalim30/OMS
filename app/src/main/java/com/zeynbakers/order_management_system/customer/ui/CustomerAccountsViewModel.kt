package com.zeynbakers.order_management_system.customer.ui

import androidx.room.withTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.formatOrderLabel
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.core.util.expandPhoneCandidates
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
    private val receiptDao = database.paymentReceiptDao()
    private val allocationDao = database.paymentAllocationDao()
    private val helperNoteDao = database.helperNoteDao()
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
    private val _statementRows = MutableStateFlow<List<CustomerStatementRowUi>>(emptyList())
    val statementRows = _statementRows.asStateFlow()
    private val _isStatementLoading = MutableStateFlow(false)
    val isStatementLoading = _isStatementLoading.asStateFlow()
    private var lastQuery: String = ""

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            lastQuery = query
            refreshSummaries()
        }
    }

    fun importCustomer(name: String, phone: String) {
        viewModelScope.launch {
            val normalizedPhone = normalizePhoneNumberE164(phone) ?: return@launch
            val cleanName = name.trim().ifBlank { normalizedPhone }

            val exactMatch = customerDao.getByPhone(normalizedPhone)
            val existing =
                exactMatch ?: customerDao.getByPhones(expandPhoneCandidates(phone))
            if (existing != null) {
                val canUpdatePhone =
                    existing.phone != normalizedPhone &&
                        exactMatch == null &&
                        customerDao.getByPhone(normalizedPhone) == null
                val updated =
                    existing.copy(
                        name = if (cleanName.isNotBlank()) cleanName else existing.name,
                        phone = if (canUpdatePhone) normalizedPhone else existing.phone,
                        isArchived = false
                    )
                if (updated != existing) {
                    customerDao.update(updated)
                }
            } else {
                val insertedId =
                    customerDao.insertIgnore(CustomerEntity(name = cleanName, phone = normalizedPhone))
                if (insertedId == -1L) {
                    customerDao.getByPhone(normalizedPhone)?.let { concurrent ->
                        val shouldUpdate = concurrent.isArchived || concurrent.name != cleanName
                        if (shouldUpdate) {
                            customerDao.update(
                                concurrent.copy(
                                    name = cleanName.ifBlank { concurrent.name },
                                    isArchived = false
                                )
                            )
                        }
                    }
                }
            }

            refreshSummaries()
        }
    }

    fun archiveCustomer(customerId: Long) {
        viewModelScope.launch {
            customerDao.archiveById(customerId)
            refreshSummaries()
        }
    }

    fun unarchiveCustomer(customerId: Long) {
        viewModelScope.launch {
            customerDao.unarchiveById(customerId)
            refreshSummaries()
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            val hasReferences =
                accountingDao.countEntriesForCustomer(customerId) > 0 ||
                    orderDao.countOrdersForCustomer(customerId) > 0 ||
                    receiptDao.countByCustomerId(customerId) > 0 ||
                    allocationDao.countByCustomerId(customerId) > 0 ||
                    helperNoteDao.countByLinkedCustomerId(customerId) > 0
            if (hasReferences) {
                customerDao.archiveById(customerId)
            } else {
                customerDao.getById(customerId)?.let { customerDao.delete(it) }
            }
            refreshSummaries()
        }
    }

    fun loadCustomer(customerId: Long) {
        viewModelScope.launch {
            _isStatementLoading.value = true
            try {
                _customer.value = customerDao.getById(customerId)
                val ledgerEntries =
                    accountingDao.getLedgerForCustomerLimited(customerId, CUSTOMER_LEDGER_MAX_ROWS)
                _ledger.value = ledgerEntries
                _balance.value = accountingDao.getCustomerBalance(customerId)
                val financeTotals = accountingDao.getCustomerFinanceTotals(customerId)
                val orderTotal = financeTotals.orderBilled ?: BigDecimal.ZERO
                val paidToOrders = financeTotals.orderPaid ?: BigDecimal.ZERO
                val availableCredit = financeTotals.extraCredit ?: BigDecimal.ZERO
                // Single source of truth for customer net position.
                val netBalance = _balance.value
                _financeSummary.value =
                    CustomerFinanceSummary(
                        orderTotal = orderTotal,
                        paidToOrders = paidToOrders,
                        availableCredit = availableCredit,
                        netBalance = netBalance
                    )
                val allOrders =
                    orderDao.getOrdersByCustomerLimited(customerId, CUSTOMER_ORDERS_MAX_ROWS)
                val customerName = _customer.value?.name
                _orderLabels.value =
                    allOrders.associate { order ->
                        order.id to
                            formatOrderLabel(
                                date = order.orderDate,
                                customerName = customerName,
                                notes = order.notes,
                                totalAmount = order.totalAmount
                            )
                    }

                _statementRows.value =
                    buildStatementRows(
                        entries = ledgerEntries,
                        ordersById = allOrders.associateBy { it.id }
                    )

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
            } finally {
                _isStatementLoading.value = false
            }
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
            refreshSummaries()
        }
    }

    fun writeOffOrder(orderId: Long) {
        viewModelScope.launch {
            val order = orderDao.getOrderById(orderId) ?: return@launch
            if (!isOlderThanOneMonth(order.orderDate)) return@launch
            val remaining = order.totalAmount - accountingDao.getPaidForOrder(orderId)
            if (remaining <= BigDecimal.ZERO) return@launch

            val customerName = _customer.value?.name
            val orderLabel =
                formatOrderLabel(
                    date = order.orderDate,
                    customerName = customerName,
                    notes = order.notes,
                    totalAmount = order.totalAmount
                )
            val note = "Bad debt write-off: $orderLabel"
            val now = Clock.System.now().toEpochMilliseconds()
            database.withTransaction {
                val receiptId =
                    receiptDao.insert(
                        PaymentReceiptEntity(
                            amount = remaining,
                            receivedAt = now,
                            method = PaymentMethod.CASH,
                            transactionCode = null,
                            hash = null,
                            senderName = null,
                            senderPhone = null,
                            rawText = null,
                            customerId = order.customerId,
                            note = note,
                            status = PaymentReceiptStatus.APPLIED,
                            createdAt = now,
                            voidedAt = null,
                            voidReason = null
                        )
                    )
                val entryId =
                    accountingDao.insertAccountEntry(
                        AccountEntryEntity(
                            orderId = orderId,
                            customerId = order.customerId,
                            type = EntryType.WRITE_OFF,
                            amount = remaining,
                            date = now,
                            description = note
                        )
                    )
                allocationDao.insert(
                    PaymentAllocationEntity(
                        receiptId = receiptId,
                        orderId = orderId,
                        customerId = order.customerId,
                        amount = remaining,
                        type = PaymentAllocationType.ORDER,
                        status = PaymentAllocationStatus.APPLIED,
                        accountEntryId = entryId,
                        reversalEntryId = null,
                        createdAt = now,
                        voidedAt = null,
                        voidReason = null
                    )
                )
            }
            orderDao.updateStatusOverride(orderId, null, Clock.System.now().toEpochMilliseconds())

            val customerId = _customer.value?.id ?: return@launch
            loadCustomer(customerId)
            refreshSummaries()
        }
    }

    fun markBadDebt(customerId: Long, amount: BigDecimal, note: String) {
        viewModelScope.launch {
            if (amount <= BigDecimal.ZERO) return@launch
            val description =
                note.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { "Bad debt - $it" }
                    ?: "Bad debt"
            database.withTransaction {
                val outstanding = accountingDao.getCustomerBalance(customerId).max(BigDecimal.ZERO)
                if (outstanding <= BigDecimal.ZERO) return@withTransaction
                val appliedAmount = if (amount > outstanding) outstanding else amount
                if (appliedAmount <= BigDecimal.ZERO) return@withTransaction
                var remainingAmount = appliedAmount
                val now = Clock.System.now().toEpochMilliseconds()
                val receiptId =
                    receiptDao.insert(
                        PaymentReceiptEntity(
                            amount = appliedAmount,
                            receivedAt = now,
                            method = PaymentMethod.CASH,
                            transactionCode = null,
                            hash = null,
                            senderName = null,
                            senderPhone = null,
                            rawText = null,
                            customerId = customerId,
                            note = description,
                            status = PaymentReceiptStatus.APPLIED,
                            createdAt = now,
                            voidedAt = null,
                            voidReason = null
                        )
                    )

                val oldestOpenOrders =
                    orderDao.getOrdersByCustomer(customerId)
                        .filter { it.status != OrderStatus.CANCELLED }
                        .sortedWith(
                            compareBy<OrderEntity> { it.orderDate }
                                .thenBy { it.createdAt }
                                .thenBy { it.id }
                        )

                for (order in oldestOpenOrders) {
                    if (remainingAmount <= BigDecimal.ZERO) break
                    val paidSoFar = accountingDao.getPaidForOrder(order.id)
                    val due = order.totalAmount - paidSoFar
                    if (due <= BigDecimal.ZERO) continue
                    val applyAmount = if (remainingAmount > due) due else remainingAmount
                    val entryId =
                        accountingDao.insertAccountEntry(
                            AccountEntryEntity(
                                orderId = order.id,
                                customerId = customerId,
                                type = EntryType.WRITE_OFF,
                                amount = applyAmount,
                                date = now,
                                description = description
                            )
                        )
                    allocationDao.insert(
                        PaymentAllocationEntity(
                            receiptId = receiptId,
                            orderId = order.id,
                            customerId = customerId,
                            amount = applyAmount,
                            type = PaymentAllocationType.ORDER,
                            status = PaymentAllocationStatus.APPLIED,
                            accountEntryId = entryId,
                            reversalEntryId = null,
                            createdAt = now,
                            voidedAt = null,
                            voidReason = null
                        )
                    )
                    remainingAmount -= applyAmount
                }

                // Fallback only if customer has positive net exposure not tied to open orders.
                if (remainingAmount > BigDecimal.ZERO) {
                    val entryId =
                        accountingDao.insertAccountEntry(
                            AccountEntryEntity(
                                orderId = null,
                                customerId = customerId,
                                type = EntryType.WRITE_OFF,
                                amount = remainingAmount,
                                date = now,
                                description = description
                            )
                        )
                    allocationDao.insert(
                        PaymentAllocationEntity(
                            receiptId = receiptId,
                            orderId = null,
                            customerId = customerId,
                            amount = remainingAmount,
                            type = PaymentAllocationType.CUSTOMER_CREDIT,
                            status = PaymentAllocationStatus.APPLIED,
                            accountEntryId = entryId,
                            reversalEntryId = null,
                            createdAt = now,
                            voidedAt = null,
                            voidReason = null
                        )
                    )
                }
            }
            loadCustomer(customerId)
            refreshSummaries()
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

    private suspend fun buildStatementRows(
        entries: List<AccountEntryEntity>,
        ordersById: Map<Long, OrderEntity>
    ): List<CustomerStatementRowUi> {
        if (entries.isEmpty()) return emptyList()
        val sortedEntries = entries.sortedWith(compareBy<AccountEntryEntity> { it.date }.thenBy { it.id })
        val receiptIds =
            sortedEntries.mapNotNull { entry ->
                if (entry.type == EntryType.CREDIT) {
                    parseReceiptId(entry.description)
                } else {
                    null
                }
            }.distinct()

        val receiptsById =
            if (receiptIds.isEmpty()) {
                emptyMap()
            } else {
                receiptDao.getByIds(receiptIds).associateBy { it.id }
            }
        val allocationsByReceipt =
            if (receiptIds.isEmpty()) {
                emptyMap()
            } else {
                allocationDao.getByReceiptIds(receiptIds)
                    .filter { it.status == PaymentAllocationStatus.APPLIED }
                    .groupBy { it.receiptId }
            }

        val rows = mutableListOf<CustomerStatementRowUi>()
        var runningBalance = BigDecimal.ZERO
        var index = 0
        while (index < sortedEntries.size) {
            val entry = sortedEntries[index]
            if (entry.type == EntryType.CREDIT) {
                val receiptId = parseReceiptId(entry.description)
                if (receiptId != null) {
                    val groupedEntries = mutableListOf<AccountEntryEntity>()
                    var cursor = index
                    while (cursor < sortedEntries.size) {
                        val candidate = sortedEntries[cursor]
                        if (candidate.type != EntryType.CREDIT) break
                        if (parseReceiptId(candidate.description) != receiptId) break
                        groupedEntries.add(candidate)
                        cursor += 1
                    }
                    val totalAmount = groupedEntries.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
                    val receipt = receiptsById[receiptId]
                    val methodLabel = paymentMethodLabel(receipt?.method, entry.description)
                    val paymentTitle =
                        if (methodLabel == "Other") {
                            "Payment"
                        } else {
                            "Payment - $methodLabel"
                        }
                    val paymentNote =
                        compactNote(receipt?.note ?: extractPaymentNote(entry.description))
                            .takeIf { it.isNotBlank() }
                    runningBalance = runningBalance - totalAmount
                    rows.add(
                        CustomerStatementRowUi(
                            key = "payment-receipt-$receiptId-${groupedEntries.first().id}",
                            timestamp = groupedEntries.last().date,
                            type = CustomerStatementRowType.PAYMENT,
                            title = paymentTitle,
                            subtitle = paymentNote,
                            amount = totalAmount,
                            signedAmount = totalAmount.negate(),
                            runningBalance = runningBalance,
                            paymentDetails =
                                buildPaymentDetails(
                                    receiptId = receiptId,
                                    methodLabel = methodLabel,
                                    groupedEntries = groupedEntries,
                                    allocationsByReceipt = allocationsByReceipt,
                                    ordersById = ordersById
                                )
                        )
                    )
                    index = cursor
                    continue
                }
            }

            val signedAmount = signedAmount(entry)
            runningBalance += signedAmount
            rows.add(
                CustomerStatementRowUi(
                    key = "entry-${entry.id}",
                    timestamp = entry.date,
                    type = statementRowType(entry.type),
                    title = statementTitle(entry, ordersById),
                    subtitle = statementSubtitle(entry, ordersById),
                    amount = entry.amount,
                    signedAmount = signedAmount,
                    runningBalance = runningBalance
                )
            )
            index += 1
        }
        return rows
    }

    private fun buildPaymentDetails(
        receiptId: Long,
        methodLabel: String,
        groupedEntries: List<AccountEntryEntity>,
        allocationsByReceipt: Map<Long, List<PaymentAllocationEntity>>,
        ordersById: Map<Long, OrderEntity>
    ): CustomerPaymentAllocationDetailsUi {
        val allocations = allocationsByReceipt[receiptId].orEmpty()
        val orderAmounts = linkedMapOf<Long, BigDecimal>()
        val unallocatedCredit: BigDecimal

        if (allocations.isNotEmpty()) {
            allocations
                .filter { it.type == PaymentAllocationType.ORDER && it.orderId != null }
                .forEach { allocation ->
                    val orderId = allocation.orderId ?: return@forEach
                    val current = orderAmounts[orderId] ?: BigDecimal.ZERO
                    orderAmounts[orderId] = current + allocation.amount
                }
            unallocatedCredit =
                allocations
                    .filter { it.type == PaymentAllocationType.CUSTOMER_CREDIT }
                    .fold(BigDecimal.ZERO) { acc, allocation -> acc + allocation.amount }
        } else {
            groupedEntries.forEach { entry ->
                val orderId = entry.orderId
                if (orderId == null) {
                    return@forEach
                }
                val current = orderAmounts[orderId] ?: BigDecimal.ZERO
                orderAmounts[orderId] = current + entry.amount
            }
            unallocatedCredit =
                groupedEntries
                    .filter { it.orderId == null }
                    .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
        }

        val targets =
            orderAmounts.map { (orderId, amount) ->
                val order = ordersById[orderId]
                CustomerPaymentAllocationTargetUi(
                    orderId = orderId,
                    orderLabel =
                        if (order == null) {
                            "Order"
                        } else {
                            formatOrderLabel(
                                date = order.orderDate,
                                customerName = null,
                                notes = order.notes,
                                totalAmount = null
                            )
                        },
                    amount = amount
                )
            }

        return CustomerPaymentAllocationDetailsUi(
            receiptId = receiptId,
            methodLabel = methodLabel,
            allocations = targets,
            unallocatedCredit = unallocatedCredit
        )
    }

    private fun statementRowType(type: EntryType): CustomerStatementRowType {
        return when (type) {
            EntryType.DEBIT -> CustomerStatementRowType.ORDER
            EntryType.CREDIT -> CustomerStatementRowType.PAYMENT
            EntryType.WRITE_OFF -> CustomerStatementRowType.BAD_DEBT
            EntryType.REVERSAL -> CustomerStatementRowType.ADJUSTMENT
        }
    }

    private fun statementTitle(entry: AccountEntryEntity, ordersById: Map<Long, OrderEntity>): String {
        return when (entry.type) {
            EntryType.DEBIT -> {
                val note = compactNote(ordersById[entry.orderId]?.notes)
                if (note.isBlank()) "Order" else "Order - $note"
            }
            EntryType.CREDIT -> "Payment"
            EntryType.WRITE_OFF -> {
                val note = compactNote(extractBadDebtNote(entry.description))
                if (note.isBlank()) "Bad debt" else "Bad debt - $note"
            }
            EntryType.REVERSAL -> {
                val note = compactNote(entry.description)
                if (note.isBlank()) {
                    "Adjustment"
                } else {
                    "Adjustment - $note"
                }
            }
        }
    }

    private fun statementSubtitle(entry: AccountEntryEntity, ordersById: Map<Long, OrderEntity>): String? {
        return when (entry.type) {
            EntryType.DEBIT -> {
                val order = ordersById[entry.orderId]
                compactNote(order?.notes).takeIf { it.isNotBlank() }
            }
            EntryType.CREDIT -> compactNote(extractPaymentNote(entry.description)).takeIf { it.isNotBlank() }
            EntryType.WRITE_OFF -> compactNote(extractBadDebtNote(entry.description)).takeIf { it.isNotBlank() }
            EntryType.REVERSAL -> compactNote(entry.description).takeIf { it.isNotBlank() }
        }
    }

    private fun signedAmount(entry: AccountEntryEntity): BigDecimal {
        return when (entry.type) {
            EntryType.DEBIT -> entry.amount
            EntryType.CREDIT -> entry.amount.negate()
            EntryType.WRITE_OFF -> entry.amount.negate()
            EntryType.REVERSAL -> entry.amount
        }
    }

    private fun parseReceiptId(description: String): Long? {
        val match = RECEIPT_ID_CAPTURE.find(description) ?: return null
        return match.groupValues.getOrNull(1)?.toLongOrNull()
    }

    private fun paymentMethodLabel(method: PaymentMethod?, description: String): String {
        return when {
            method == PaymentMethod.MPESA -> "M-Pesa"
            method == PaymentMethod.CASH -> "Cash"
            description.contains("MPESA", ignoreCase = true) -> "M-Pesa"
            description.contains("M-PESA", ignoreCase = true) -> "M-Pesa"
            description.contains("CASH", ignoreCase = true) -> "Cash"
            else -> "Other"
        }
    }

    private fun extractPaymentNote(description: String): String {
        return description.substringAfter(": ", "")
    }

    private fun extractBadDebtNote(description: String): String {
        if (description.contains(":", ignoreCase = true)) {
            return description.substringAfter(":")
        }
        return description.substringAfter("-", "")
    }

    private fun compactNote(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(RECEIPT_ID_CAPTURE, " ")
            .replace(ORDER_HASH_REGEX, " ")
            .replace(ORDER_ID_PAREN_REGEX, " ")
            .replace(ORDER_ID_TEXT_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim(' ', '-', ':', '|')
            .take(42)
    }

    private fun isOlderThanOneMonth(orderDate: kotlinx.datetime.LocalDate): Boolean {
        val cutoff = orderDate.plus(1, DateTimeUnit.MONTH)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today >= cutoff
    }

    private suspend fun refreshSummaries() {
        val pattern = "%${lastQuery.trim()}%"
        _summaries.value = accountingDao.getCustomerAccountSummaries(pattern)
    }

    companion object {
        private const val CUSTOMER_LEDGER_MAX_ROWS = 1_000
        private const val CUSTOMER_ORDERS_MAX_ROWS = 500
        private val RECEIPT_ID_CAPTURE = Regex("Receipt\\s*#(\\d+)", RegexOption.IGNORE_CASE)
        private val ORDER_HASH_REGEX = Regex("Order\\s*#\\d+", RegexOption.IGNORE_CASE)
        private val ORDER_ID_PAREN_REGEX = Regex("\\(\\s*ID\\s*\\d+\\s*\\)", RegexOption.IGNORE_CASE)
        private val ORDER_ID_TEXT_REGEX = Regex("\\bID\\s*\\d+\\b", RegexOption.IGNORE_CASE)
        private val MULTI_SPACE_REGEX = Regex("\\s{2,}")
    }
}
