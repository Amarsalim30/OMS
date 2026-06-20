package com.zeynbakers.order_management_system.order.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.zeynbakers.order_management_system.accounting.data.AccountingDao
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.util.expandPhoneCandidates
import com.zeynbakers.order_management_system.core.util.formatOrderLabelWithId
import com.zeynbakers.order_management_system.core.util.normalizePhoneNumberE164
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.domain.OrderRepository
import com.zeynbakers.order_management_system.order.ui.common.CalendarDayUi
import com.zeynbakers.order_management_system.order.ui.common.OrderItemDraft
import com.zeynbakers.order_management_system.order.ui.common.PaymentState
import com.zeynbakers.order_management_system.order.ui.day_detail.models.ImportAction
import com.zeynbakers.order_management_system.order.ui.day_detail.models.OrderDraft
import com.zeynbakers.order_management_system.order.ui.day_detail.models.OrderImportAction
import com.zeynbakers.order_management_system.order.ui.models.OrderUiModel
import com.zeynbakers.order_management_system.product.data.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

data class MonthKey(val year: Int, val month: Int)

data class MonthSnapshot(
    val days: List<CalendarDayUi>,
    val daysByDate: Map<LocalDate, CalendarDayUi>,
    val total: BigDecimal,
    val badgeCount: Int
)

class CalendarViewModel(private val database: AppDatabase) : ViewModel() {

    private val orderDao: OrderDao = database.orderDao()
    private val accountingDao: AccountingDao = database.accountingDao()
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()
    private val allocationDao = database.paymentAllocationDao()
    private val receiptDao = database.paymentReceiptDao()
    private val receiptProcessor = PaymentReceiptProcessor(database)
    private val orderRepository = OrderRepository(orderDao, database.orderItemDao(), productDao)

    private val _calendarDays = MutableStateFlow<List<CalendarDayUi>>(emptyList())
    val calendarDays = _calendarDays.asStateFlow()

    private val _ordersForDate = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val ordersForDate = _ordersForDate.asStateFlow()

    private val _dayTotal = MutableStateFlow(BigDecimal.ZERO)
    val dayTotal = _dayTotal.asStateFlow()

    private val _summaryOrders = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val summaryOrders = _summaryOrders.asStateFlow()

    private val _summaryTotal = MutableStateFlow(BigDecimal.ZERO)
    val summaryTotal = _summaryTotal.asStateFlow()

    private val _monthTotal = MutableStateFlow(BigDecimal.ZERO)
    val monthTotal = _monthTotal.asStateFlow()

    private val _monthBadgeCount = MutableStateFlow(0)
    val monthBadgeCount = _monthBadgeCount.asStateFlow()

    private val _monthSnapshots = MutableStateFlow<Map<MonthKey, MonthSnapshot>>(emptyMap())
    val monthSnapshots = _monthSnapshots.asStateFlow()

    private val _unpaidOrders = MutableStateFlow<List<OrderUiModel>>(emptyList())
    val unpaidOrders = _unpaidOrders.asStateFlow()

    private val _creditPrompt = MutableStateFlow<OrderCreditPrompt?>(null)
    val creditPrompt = _creditPrompt.asStateFlow()

    private val _dayDrafts = MutableStateFlow<Map<LocalDate, OrderDraft>>(emptyMap())
    val dayDrafts = _dayDrafts.asStateFlow()

    fun updateDraft(date: LocalDate, draft: OrderDraft?) {
        _dayDrafts.update { current ->
            if (draft == null) {
                current - date
            } else {
                current + (date to draft)
            }
        }
    }

    private var lastMonth: Int? = null
    private var lastYear: Int? = null
    private var currentMonthLoadJob: Job? = null

    fun saveOrder(
        date: LocalDate,
        cartItems: List<OrderItemDraft>,
        customerName: String,
        customerPhone: String,
        pickupTime: String?,
        existingOrderId: Long?
    ) {
        viewModelScope.launch {
            val result =
                database.withTransaction {
                    saveOrderTransactional(
                        date = date,
                        cartItems = cartItems,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        pickupTime = pickupTime,
                        existingOrderId = existingOrderId
                    )
                }
            result.creditPrompt?.let { _creditPrompt.value = it }
            loadOrdersForDate(result.date)
            refreshMonthSnapshots(result.affectedMonths)
        }
    }

    fun importOrders(
        actions: List<OrderImportAction>,
        targetDate: LocalDate
    ) {
        viewModelScope.launch {
            database.withTransaction {
                actions.forEach { action ->
                    val item = action.importItem
                    val orderDate = targetDate
                    val customerName = item.customerName ?: ""
                    val customerPhone = item.customerPhone ?: ""
                    val pickupTime = item.pickupTime

                    when (action.action) {
                        ImportAction.CREATE -> {
                            val cartItems = item.cartItems.map {
                                OrderItemDraft(
                                    productId = null,
                                    emoji = it.emoji,
                                    name = it.name,
                                    quantity = it.quantity,
                                    unitPrice = BigDecimal(it.unitPrice),
                                    categorySnapshot = null
                                )
                            }
                            saveOrderTransactional(
                                date = orderDate,
                                cartItems = cartItems,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                pickupTime = pickupTime,
                                existingOrderId = null
                            )
                        }

                        ImportAction.MERGE -> {
                            val duplicateOrderId = action.duplicateOrderId
                            if (duplicateOrderId != null) {
                                val existingOrder = orderDao.getOrderById(duplicateOrderId)
                                if (existingOrder != null) {
                                    val existingItems =
                                        database.orderItemDao().getOrderItems(duplicateOrderId)
                                    val importItems = item.cartItems.map {
                                        OrderItemDraft(
                                            productId = null,
                                            emoji = it.emoji,
                                            name = it.name,
                                            quantity = it.quantity,
                                            unitPrice = BigDecimal(it.unitPrice),
                                            categorySnapshot = null
                                        )
                                    }
                                    val mergedItems = mergeOrderItems(existingItems, importItems)
                                    saveOrderTransactional(
                                        date = orderDate,
                                        cartItems = mergedItems,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        pickupTime = pickupTime,
                                        existingOrderId = duplicateOrderId
                                    )
                                }
                            }
                        }
                    }
                }
            }
            loadOrdersForDate(targetDate)
            refreshMonthSnapshots(setOf(MonthKey(targetDate.year, targetDate.monthNumber)))
        }
    }

    private fun mergeOrderItems(
        existingItems: List<com.zeynbakers.order_management_system.order.data.OrderItemEntity>,
        importItems: List<OrderItemDraft>
    ): List<OrderItemDraft> {
        val mergedItems = existingItems.map {
            OrderItemDraft(
                productId = it.productId,
                emoji = "",
                name = it.productNameSnapshot,
                quantity = it.quantity,
                unitPrice = it.effectivePrice,
                categorySnapshot = it.categorySnapshot
            )
        }.toMutableList()

        importItems.forEach { importItem ->
            // Match by productId first, then by name as fallback
            val existing = mergedItems.find {
                (it.productId != null && it.productId == importItem.productId) ||
                        (it.productId == null && importItem.productId == null && it.name == importItem.name)
            }
            if (existing != null) {
                // Preserve productId from existing item if import has none
                val finalProductId = existing.productId ?: importItem.productId
                mergedItems[mergedItems.indexOf(existing)] = existing.copy(
                    quantity = existing.quantity + importItem.quantity,
                    productId = finalProductId
                )
            } else {
                mergedItems.add(importItem)
            }
        }

        return mergedItems
    }

    private suspend fun saveOrderTransactional(
        date: LocalDate,
        cartItems: List<OrderItemDraft>,
        customerName: String,
        customerPhone: String,
        pickupTime: String?,
        existingOrderId: Long?
    ): SaveOrderResult {
        val now = Clock.System.now().toEpochMilliseconds()
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

        // Derive total from cartItems for single source of truth
        val totalAmount = cartItems.sumOf { it.lineTotal }

        val updatedOrder =
            (existingOrder
                ?: OrderEntity(
                    orderDate = date,
                    totalAmount = totalAmount,
                    customerId = customerId
                ))
                .copy(
                    orderDate = date,
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

        // Save order items - direct conversion from OrderItemDraft to OrderItemEntity
        if (cartItems.isNotEmpty()) {
            val orderItemDao = database.orderItemDao()
            // Delete existing items for this order if editing
            if (existingOrder != null) {
                orderItemDao.deleteOrderItems(orderId)
            }
            // Insert new items
            val orderItemEntities = cartItems.map { draft ->
                com.zeynbakers.order_management_system.order.data.OrderItemEntity(
                    orderId = orderId,
                    productId = draft.productId,
                    productNameSnapshot = draft.name,
                    unitPriceSnapshot = draft.unitPrice,
                    categorySnapshot = draft.categorySnapshot
                        ?: com.zeynbakers.order_management_system.order.data.ItemCategory.OTHER,
                    quantity = draft.quantity
                )
            }
            orderItemDao.insertAll(orderItemEntities)
        }

        if (existingOrder?.customerId != null && customerId != null && existingOrder.customerId != customerId) {
            accountingDao.updateCustomerIdForOrderEntries(
                orderId = orderId,
                customerId = customerId
            )
        } else if (existingOrder?.customerId == null && customerId != null) {
            accountingDao.updateCustomerIdForOrderEntries(
                orderId = orderId,
                customerId = customerId
            )
        }

        val savedOrder = updatedOrder.copy(id = orderId)
        upsertAccountingEntry(savedOrder)
        accountingDao.reconcileOrderSettlementToTotal(
            orderId = orderId,
            customerId = customerId,
            orderTotal = savedOrder.totalAmount,
            now = now
        )
        val creditPrompt =
            buildCreditPromptIfEligible(
                isNewOrder = isNewOrder,
                existingOrder = existingOrder,
                savedOrder = savedOrder,
                orderId = orderId,
                customerId = customerId
            )
        val affectedMonths =
            buildSet {
                add(date.toMonthKey())
                existingOrder?.orderDate?.let { add(it.toMonthKey()) }
            }

        return SaveOrderResult(
            date = date,
            affectedMonths = affectedMonths,
            creditPrompt = creditPrompt
        )
    }

    private suspend fun buildCreditPromptIfEligible(
        isNewOrder: Boolean,
        existingOrder: OrderEntity?,
        savedOrder: OrderEntity,
        orderId: Long,
        customerId: Long?
    ): OrderCreditPrompt? {
        val hasSettlementChange =
            isNewOrder ||
                    existingOrder == null ||
                    existingOrder.customerId != customerId ||
                    existingOrder.totalAmount.compareTo(savedOrder.totalAmount) != 0
        if (!hasSettlementChange || customerId == null) return null

        val availableCredit =
            accountingDao.getCustomerFinanceTotals(customerId).extraCredit ?: BigDecimal.ZERO
        val paidForOrder = accountingDao.getPaidForOrder(orderId)
        val outstandingAfterSave = savedOrder.totalAmount - paidForOrder
        if (!shouldPromptForAvailableCredit(customerId, availableCredit, outstandingAfterSave)) {
            return null
        }

        val resolvedCustomerName = customerDao.getById(customerId)?.name
        val orderLabel =
            formatOrderLabelWithId(
                orderId = orderId,
                date = savedOrder.orderDate,
                customerName = resolvedCustomerName,
                notes = "",
                totalAmount = savedOrder.totalAmount
            )
        return OrderCreditPrompt(
            orderId = orderId,
            customerId = customerId,
            availableCredit = availableCredit,
            orderLabel = orderLabel
        )
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
            refreshMonthSnapshots(setOf(order.orderDate.toMonthKey()))
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
                notes = "",
                totalAmount = order.totalAmount
            )
        accountingDao.upsertDebitForOrder(
            orderId = order.id,
            customerId = order.customerId,
            amount = order.totalAmount,
            date = order.orderDate.atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds(),
            description = "Charge: $orderLabel"
        )
    }

    fun loadOrdersForDate(date: LocalDate) {
        viewModelScope.launch {
            val orders = orderDao.getOrdersByDate(date.toString())
            val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }

            val uiModels = activeOrders.map { order ->
                val items = database.orderItemDao().getOrderItems(order.id)
                val customer = order.customerId?.let { customerDao.getById(it) }
                val paid = accountingDao.getPaidForOrder(order.id)
                OrderUiModel(order, items, customer, paid)
            }

            _ordersForDate.value = uiModels
            _dayTotal.value =
                activeOrders.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }
        }
    }

    fun loadSummaryRange(startInclusive: LocalDate, endExclusive: LocalDate) {
        viewModelScope.launch {
            val orders =
                orderDao.getOrdersBetween(startInclusive.toString(), endExclusive.toString())
            val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }

            val uiModels = activeOrders.map { order ->
                val items = database.orderItemDao().getOrderItems(order.id)
                val customer = order.customerId?.let { customerDao.getById(it) }
                val paid = accountingDao.getPaidForOrder(order.id)
                OrderUiModel(order, items, customer, paid)
            }.sortedWith(
                compareByDescending<OrderUiModel> { it.order.orderDate }.thenByDescending { it.order.createdAt }
            )

            _summaryOrders.value = uiModels
            _summaryTotal.value =
                activeOrders.fold(BigDecimal.ZERO) { acc, order -> acc + order.totalAmount }
        }
    }

    fun cancelOrder(orderId: Long, date: LocalDate) {
        viewModelScope.launch {
            val existingDate = orderDao.getOrderById(orderId)?.orderDate ?: date
            database.withTransaction {
                cancelOrderTransactional(orderId)
            }
            loadOrdersForDate(existingDate)
            refreshMonthSnapshots(setOf(existingDate.toMonthKey()))
            loadUnpaidOrders()
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
                orderDao.getOpenOrdersLimited(MOVE_TARGET_MAX_ORDERS)
            } else {
                orderDao.getOpenOrdersByCustomerLimited(customerId, MOVE_TARGET_MAX_ORDERS)
            }
        val customerIds = orders.mapNotNull { it.customerId }.distinct()
        val customerNames =
            if (customerIds.isEmpty()) emptyMap()
            else customerDao.getByIds(customerIds).associate { it.id to it.name }
        return orders
            .filter { it.id != excludeOrderId }
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
                        notes = "",
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
        val existingDate = orderDao.getOrderById(orderId)?.orderDate ?: date
        database.withTransaction {
            val validAllocationIds =
                if (allocationIds.isEmpty()) {
                    emptyList()
                } else {
                    val orderAllocationIds =
                        allocationDao.getByOrderId(orderId).map { it.id }.toSet()
                    allocationIds.filter { it in orderAllocationIds }
                }
            val order = orderDao.getOrderById(orderId)
            val orderLabel =
                order?.let {
                    val customerName = it.customerId?.let { id -> customerDao.getById(id)?.name }
                    formatOrderLabelWithId(
                        orderId = it.id,
                        date = it.orderDate,
                        customerName = customerName,
                        notes = "",
                        totalAmount = it.totalAmount
                    )
                } ?: "Order ID $orderId"
            val description = "$orderLabel deleted"
            when (action) {
                OrderPaymentAction.MOVE -> {
                    if (validAllocationIds.isNotEmpty() && target != null) {
                        receiptProcessor.moveAllocations(
                            allocationIds = validAllocationIds,
                            target = target,
                            descriptionBase = description,
                            moveFullReceipts = moveFullReceipts
                        )
                    }
                }

                OrderPaymentAction.VOID -> {
                    if (validAllocationIds.isNotEmpty()) {
                        receiptProcessor.voidAllocations(
                            allocationIds = validAllocationIds,
                            reason = "Order deleted"
                        )
                    }
                }
            }
            cancelOrderTransactional(orderId)
        }
        loadOrdersForDate(existingDate)
        refreshMonthSnapshots(setOf(existingDate.toMonthKey()))
        loadUnpaidOrders()
        return true
    }

    private suspend fun cancelOrderTransactional(orderId: Long) {
        orderDao.markCancelled(orderId)
        accountingDao.deleteDebitEntriesForOrder(orderId)
        accountingDao.deleteWriteOffEntriesForOrder(orderId)
        accountingDao.moveOrderCreditsToCustomerLevel(orderId)
    }

    fun loadMonth(month: Int, year: Int, forceRefresh: Boolean = false) {
        currentMonthLoadJob?.cancel()
        currentMonthLoadJob =
            viewModelScope.launch {
                loadMonth(
                    month = month,
                    year = year,
                    setAsCurrent = true,
                    forceRefresh = forceRefresh
                )
            }
    }

    fun prefetchAdjacentMonths(year: Int, month: Int) {
        viewModelScope.launch {
            val (prevYear, prevMonth) = shiftMonth(year, month, -1)
            val (nextYear, nextMonth) = shiftMonth(year, month, 1)
            launch {
                loadMonth(
                    month = prevMonth,
                    year = prevYear,
                    setAsCurrent = false,
                    forceRefresh = false
                )
            }
            launch {
                loadMonth(
                    month = nextMonth,
                    year = nextYear,
                    setAsCurrent = false,
                    forceRefresh = false
                )
            }
        }
    }

    private suspend fun loadMonth(
        month: Int,
        year: Int,
        setAsCurrent: Boolean,
        forceRefresh: Boolean
    ) {
        if (setAsCurrent) {
            lastMonth = month
            lastYear = year
        }
        val key = MonthKey(year, month)
        if (!forceRefresh) {
            val cached = _monthSnapshots.value[key]
            if (cached != null) {
                if (setAsCurrent) {
                    _calendarDays.value = cached.days
                    _monthTotal.value = cached.total
                    _monthBadgeCount.value = cached.badgeCount
                }
                return
            }
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
        val orderIds = activeOrders.map { it.id }.filter { it != 0L }
        val paidByOrder =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                accountingDao.getPaidForOrders(orderIds).associate { it.orderId to it.paid }
            }
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val monthData =
            withContext(Dispatchers.Default) {
                buildMonthData(
                    activeOrders = activeOrders,
                    paidByOrder = paidByOrder,
                    month = month,
                    year = year,
                    today = today,
                    gridStart = gridStart,
                    daySlots = leadingDays + daysInMonth + trailingDays
                )
            }
        val snapshot =
            MonthSnapshot(
                days = monthData.calendarDays,
                daysByDate = monthData.calendarDaysByDate,
                total = monthData.monthTotal,
                badgeCount = monthData.badgeCount
            )
        val updatedSnapshots = _monthSnapshots.value + (key to snapshot)
        _monthSnapshots.value =
            if (updatedSnapshots.size > MAX_CACHED_SNAPSHOTS) {
                updatedSnapshots.entries
                    .drop(updatedSnapshots.size - MAX_CACHED_SNAPSHOTS)
                    .associate { it.key to it.value }
            } else {
                updatedSnapshots
            }

        if (setAsCurrent) {
            _calendarDays.value = monthData.calendarDays
            _monthTotal.value = monthData.monthTotal
            _monthBadgeCount.value = monthData.badgeCount
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

    suspend fun searchProducts(query: String): List<ProductEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return productDao.searchActiveProducts("%$trimmed%")
    }

    suspend fun ensureProduct(
        name: String,
        defaultPrice: BigDecimal,
        emoji: String
    ): ProductEntity {
        val cleanName = name.trim()
        val matches = productDao.searchActiveProducts(cleanName)
        val existing = matches.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) return existing
        val id =
            productDao.insertProduct(
                ProductEntity(
                    name = cleanName,
                    defaultPrice = defaultPrice,
                    emoji = emoji.trim().ifBlank { "📦" }
                )
            )
        return productDao.getProductById(id)
            ?: ProductEntity(name = cleanName, defaultPrice = defaultPrice, emoji = emoji)
    }

    private suspend fun resolveCustomerId(name: String, phone: String): Long? {
        val normalizedPhone = normalizePhoneNumberE164(phone) ?: return null

        val cleanName = name.ifBlank { normalizedPhone }
        val exactMatch = customerDao.getByPhone(normalizedPhone)
        val existing =
            exactMatch ?: customerDao.getByPhones(expandPhoneCandidates(phone))
        return if (existing != null) {
            val canUpdatePhone =
                existing.phone != normalizedPhone &&
                        exactMatch == null &&
                        customerDao.getByPhone(normalizedPhone) == null
            val updated =
                existing.copy(
                    name = if (name.isNotBlank()) name else existing.name,
                    phone = if (canUpdatePhone) normalizedPhone else existing.phone,
                    isArchived = false
                )
            if (updated != existing) {
                customerDao.update(updated)
            }
            existing.id
        } else {
            val insertedId =
                customerDao.insertIgnore(CustomerEntity(name = cleanName, phone = normalizedPhone))
            if (insertedId != -1L) {
                insertedId
            } else {
                customerDao.getByPhone(normalizedPhone)?.id
                    ?: customerDao.getByPhones(expandPhoneCandidates(phone))?.id
                    ?: return null
            }
        }
    }

    private fun refreshMonthSnapshots(affectedMonths: Set<MonthKey>) {
        if (affectedMonths.isEmpty()) return
        viewModelScope.launch {
            // Drop stale entries first so pager pages cannot keep rendering old prefetched data.
            _monthSnapshots.value = _monthSnapshots.value - affectedMonths
            val currentKey =
                if (lastYear != null && lastMonth != null) {
                    MonthKey(year = lastYear!!, month = lastMonth!!)
                } else {
                    null
                }
            affectedMonths.forEach { key ->
                loadMonth(
                    month = key.month,
                    year = key.year,
                    setAsCurrent = key == currentKey,
                    forceRefresh = true
                )
            }
        }
    }

    fun loadUnpaidOrders() {
        viewModelScope.launch {
            val orders =
                orderDao.getOpenOrdersLimited(UNPAID_SCREEN_MAX_ORDERS)
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

            val uiModels = unpaid.map { order ->
                val items = database.orderItemDao().getOrderItems(order.id)
                val customer = order.customerId?.let { customerDao.getById(it) }
                val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
                OrderUiModel(order, items, customer, paid)
            }.sortedWith(
                compareByDescending<OrderUiModel> { it.order.orderDate }.thenByDescending { it.order.createdAt }
            )

            _unpaidOrders.value = uiModels
        }
    }

    companion object {
        private const val MOVE_TARGET_MAX_ORDERS = 1_500
        private const val UNPAID_SCREEN_MAX_ORDERS = 2_000
        private const val MAX_CACHED_SNAPSHOTS = 36
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

    private fun buildMonthData(
        activeOrders: List<OrderEntity>,
        paidByOrder: Map<Long, BigDecimal>,
        month: Int,
        year: Int,
        today: LocalDate,
        gridStart: LocalDate,
        daySlots: Int
    ): MonthComputation {
        val aggregates = HashMap<LocalDate, DayAggregate>()
        var monthTotal = BigDecimal.ZERO
        var badgeCount = 0

        activeOrders.forEach { order ->
            val paid = paidByOrder[order.id] ?: BigDecimal.ZERO
            val aggregate = aggregates.getOrPut(order.orderDate) { DayAggregate() }
            aggregate.orderCount += 1
            aggregate.total = aggregate.total + order.totalAmount
            aggregate.paid = aggregate.paid + paid
            aggregate.orderStates.add(resolveOrderPaymentState(order.totalAmount, paid))

            if (order.orderDate.monthNumber == month && order.orderDate.year == year) {
                monthTotal = monthTotal + order.totalAmount
                if (paid < order.totalAmount) {
                    badgeCount += 1
                }
            }
        }

        val calendarDays =
            (0 until daySlots).map { offset ->
                val date = gridStart.plus(offset, DateTimeUnit.DAY)
                val aggregate = aggregates[date]
                val dayTotal = aggregate?.total ?: BigDecimal.ZERO
                val dayPaid = aggregate?.paid ?: BigDecimal.ZERO
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
                    orderCount = aggregate?.orderCount ?: 0,
                    totalAmount = dayTotal,
                    isToday = date == today,
                    isInCurrentMonth = date.monthNumber == month,
                    paymentState = paymentState,
                    orderStates = aggregate?.orderStates?.toList() ?: emptyList()
                )
            }
        return MonthComputation(
            calendarDays = calendarDays,
            calendarDaysByDate = calendarDays.associateBy { it.date },
            monthTotal = monthTotal,
            badgeCount = badgeCount
        )
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

private data class SaveOrderResult(
    val date: LocalDate,
    val affectedMonths: Set<MonthKey>,
    val creditPrompt: OrderCreditPrompt?
)

private data class DayAggregate(
    var orderCount: Int = 0,
    var total: BigDecimal = BigDecimal.ZERO,
    var paid: BigDecimal = BigDecimal.ZERO,
    val orderStates: MutableList<PaymentState> = mutableListOf()
)

private data class MonthComputation(
    val calendarDays: List<CalendarDayUi>,
    val calendarDaysByDate: Map<LocalDate, CalendarDayUi>,
    val monthTotal: BigDecimal,
    val badgeCount: Int
)

internal fun shouldPromptForAvailableCredit(
    customerId: Long?,
    availableCredit: BigDecimal,
    outstandingAfterSave: BigDecimal
): Boolean {
    if (customerId == null) return false
    if (availableCredit <= BigDecimal.ZERO) return false
    if (outstandingAfterSave <= BigDecimal.ZERO) return false
    return true
}

private fun LocalDate.toMonthKey(): MonthKey = MonthKey(year = year, month = monthNumber)
