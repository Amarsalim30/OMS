@file:Suppress("unused")

package com.zeynbakers.order_management_system.accounting.domain

import androidx.room.withTransaction
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.order.data.OrderEntity
import java.math.BigDecimal
import kotlinx.datetime.Clock

sealed class ReceiptAllocation {
    data class Order(val orderId: Long) : ReceiptAllocation()
    data class OldestOrders(val customerId: Long) : ReceiptAllocation()
    data class CustomerCredit(val customerId: Long) : ReceiptAllocation()
    data object Unapplied : ReceiptAllocation()
}

class PaymentReceiptProcessor(private val database: AppDatabase) {
    private val receiptDao = database.paymentReceiptDao()
    private val allocationDao = database.paymentAllocationDao()
    private val accountingDao = database.accountingDao()
    private val orderDao = database.orderDao()

    data class AllocationMoveSummary(
        val movedAllocations: Int,
        val affectedReceipts: Int
    )

    suspend fun createReceipt(
        amount: BigDecimal,
        receivedAt: Long,
        method: PaymentMethod,
        transactionCode: String?,
        hash: String?,
        senderName: String?,
        senderPhone: String?,
        rawText: String?,
        customerId: Long?,
        note: String?
    ): PaymentReceiptEntity {
        val now = Clock.System.now().toEpochMilliseconds()
        val receipt =
            PaymentReceiptEntity(
                amount = amount,
                receivedAt = receivedAt,
                method = method,
                transactionCode = transactionCode,
                hash = hash,
                senderName = senderName,
                senderPhone = senderPhone,
                rawText = rawText,
                customerId = customerId,
                note = note,
                status = PaymentReceiptStatus.UNAPPLIED,
                createdAt = now,
                voidedAt = null,
                voidReason = null
            )
        val id = receiptDao.insert(receipt)
        return receipt.copy(id = id)
    }

    suspend fun createAndApplyReceipt(
        receipt: PaymentReceiptEntity,
        descriptionBase: String,
        allocation: ReceiptAllocation
    ): PaymentReceiptEntity {
        val applied =
            database.withTransaction {
                val saved =
                    if (receipt.id == 0L) {
                        receipt.copy(id = receiptDao.insert(receipt))
                    } else {
                        receiptDao.getById(receipt.id) ?: receipt
                    }
                val totalApplied =
                    when (allocation) {
                        is ReceiptAllocation.Order ->
                            applyToOrder(saved, allocation.orderId, descriptionBase)
                        is ReceiptAllocation.OldestOrders ->
                            applyToOldestOrders(saved, allocation.customerId, descriptionBase)
                        is ReceiptAllocation.CustomerCredit ->
                            applyToCustomerCredit(saved, allocation.customerId, descriptionBase, saved.amount)
                        ReceiptAllocation.Unapplied -> BigDecimal.ZERO
                    }
                val status =
                    when {
                        allocation == ReceiptAllocation.Unapplied -> PaymentReceiptStatus.UNAPPLIED
                        totalApplied <= BigDecimal.ZERO -> PaymentReceiptStatus.UNAPPLIED
                        totalApplied < saved.amount -> PaymentReceiptStatus.PARTIAL
                        else -> PaymentReceiptStatus.APPLIED
                    }
                if (saved.status != status) {
                    receiptDao.update(saved.copy(status = status))
                }
                saved
            }
        return applied
    }

    suspend fun voidReceipt(receiptId: Long, reason: String?): Boolean {
        return database.withTransaction {
            val receipt = receiptDao.getById(receiptId) ?: return@withTransaction false
            if (receipt.status == PaymentReceiptStatus.VOIDED) return@withTransaction false
            val now = Clock.System.now().toEpochMilliseconds()
            val allocations = allocationDao.getByReceiptId(receiptId)
            allocations.filter { it.status != PaymentAllocationStatus.VOIDED }.forEach { allocation ->
                val entryId =
                    allocation.accountEntryId?.let { accountEntryId ->
                        val reversalId =
                            accountingDao.insertAccountEntry(
                                AccountEntryEntity(
                                    orderId = allocation.orderId,
                                    customerId = allocation.customerId,
                                    type = EntryType.REVERSAL,
                                    amount = allocation.amount,
                                    date = now,
                                    description = buildVoidDescription(receiptId, reason)
                                )
                            )
                        reversalId
                    }
                allocationDao.markVoided(
                    id = allocation.id,
                    status = PaymentAllocationStatus.VOIDED,
                    reversalEntryId = entryId,
                    voidedAt = now,
                    voidReason = reason
                )
            }
            receiptDao.update(
                receipt.copy(
                    status = PaymentReceiptStatus.VOIDED,
                    voidedAt = now,
                    voidReason = reason
                )
            )
            true
        }
    }

    suspend fun reallocateReceipt(
        receiptId: Long,
        allocation: ReceiptAllocation,
        descriptionBase: String
    ): Boolean {
        return database.withTransaction {
            val receipt = receiptDao.getById(receiptId) ?: return@withTransaction false
            val now = Clock.System.now().toEpochMilliseconds()
            val existingAllocations = allocationDao.getByReceiptId(receiptId)
            existingAllocations.filter { it.status != PaymentAllocationStatus.VOIDED }.forEach { allocationRow ->
                val entryId =
                    allocationRow.accountEntryId?.let {
                        accountingDao.insertAccountEntry(
                            AccountEntryEntity(
                                orderId = allocationRow.orderId,
                                customerId = allocationRow.customerId,
                                type = EntryType.REVERSAL,
                                amount = allocationRow.amount,
                                date = now,
                                description = buildReallocateDescription(receiptId)
                            )
                        )
                    }
                allocationDao.markVoided(
                    id = allocationRow.id,
                    status = PaymentAllocationStatus.VOIDED,
                    reversalEntryId = entryId,
                    voidedAt = now,
                    voidReason = "Reallocated"
                )
            }

            val resolvedCustomerId =
                when (allocation) {
                    is ReceiptAllocation.Order -> orderDao.getOrderById(allocation.orderId)?.customerId
                    is ReceiptAllocation.OldestOrders -> allocation.customerId
                    is ReceiptAllocation.CustomerCredit -> allocation.customerId
                    ReceiptAllocation.Unapplied -> receipt.customerId
                }
            val resetReceipt =
                receipt.copy(
                    status = PaymentReceiptStatus.UNAPPLIED,
                    customerId = resolvedCustomerId ?: receipt.customerId
                )
            receiptDao.update(resetReceipt)

            val totalApplied =
                when (allocation) {
                    is ReceiptAllocation.Order ->
                        applyToOrder(resetReceipt, allocation.orderId, descriptionBase)
                    is ReceiptAllocation.OldestOrders ->
                        applyToOldestOrders(resetReceipt, allocation.customerId, descriptionBase)
                    is ReceiptAllocation.CustomerCredit ->
                        applyToCustomerCredit(resetReceipt, allocation.customerId, descriptionBase, resetReceipt.amount)
                    ReceiptAllocation.Unapplied -> BigDecimal.ZERO
                }
            val status =
                when {
                    allocation == ReceiptAllocation.Unapplied -> PaymentReceiptStatus.UNAPPLIED
                    totalApplied <= BigDecimal.ZERO -> PaymentReceiptStatus.UNAPPLIED
                    totalApplied < resetReceipt.amount -> PaymentReceiptStatus.PARTIAL
                    else -> PaymentReceiptStatus.APPLIED
                }
            if (resetReceipt.status != status) {
                receiptDao.update(resetReceipt.copy(status = status))
            }
            true
        }
    }

    suspend fun moveAllocations(
        allocationIds: List<Long>,
        target: ReceiptAllocation,
        descriptionBase: String,
        moveFullReceipts: Boolean
    ): AllocationMoveSummary {
        if (allocationIds.isEmpty()) return AllocationMoveSummary(0, 0)
        val allocations =
            allocationDao.getByIds(allocationIds)
                .filter { it.status == PaymentAllocationStatus.APPLIED }
        if (allocations.isEmpty()) return AllocationMoveSummary(0, 0)

        val receiptIds = allocations.map { it.receiptId }.distinct()
        if (moveFullReceipts) {
            receiptIds.forEach { receiptId ->
                reallocateReceipt(
                    receiptId = receiptId,
                    allocation = target,
                    descriptionBase = descriptionBase
                )
            }
            return AllocationMoveSummary(allocations.size, receiptIds.size)
        }

        return database.withTransaction {
            val receiptsById = receiptDao.getByIds(receiptIds).associateBy { it.id }
            val allocationsByReceipt = allocations.groupBy { it.receiptId }
            val now = Clock.System.now().toEpochMilliseconds()

            allocations.forEach { allocation ->
                val entryId =
                    allocation.accountEntryId?.let { accountEntryId ->
                        accountingDao.insertAccountEntry(
                            AccountEntryEntity(
                                orderId = allocation.orderId,
                                customerId = allocation.customerId,
                                type = EntryType.REVERSAL,
                                amount = allocation.amount,
                                date = now,
                                description = buildReallocateDescription(allocation.receiptId)
                            )
                        )
                    }
                allocationDao.markVoided(
                    id = allocation.id,
                    status = PaymentAllocationStatus.VOIDED,
                    reversalEntryId = entryId,
                    voidedAt = now,
                    voidReason = "Reallocated"
                )
            }

            allocationsByReceipt.forEach { (receiptId, movedAllocations) ->
                val receipt = receiptsById[receiptId] ?: return@forEach
                val amountToMove =
                    movedAllocations.fold(BigDecimal.ZERO) { acc, allocation -> acc + allocation.amount }
                val resolvedCustomerId = resolveCustomerIdForTarget(receipt, target)
                val updatedReceipt =
                    if (receipt.customerId == null && resolvedCustomerId != null) {
                        receipt.copy(customerId = resolvedCustomerId)
                    } else {
                        receipt
                    }
                if (updatedReceipt !== receipt) {
                    receiptDao.update(updatedReceipt)
                }

                when (target) {
                    is ReceiptAllocation.Order ->
                        applyAmountToOrder(updatedReceipt, target.orderId, amountToMove, descriptionBase)
                    is ReceiptAllocation.OldestOrders ->
                        applyAmountToOldestOrders(updatedReceipt, target.customerId, amountToMove, descriptionBase)
                    is ReceiptAllocation.CustomerCredit ->
                        applyAmountToCustomerCredit(updatedReceipt, target.customerId, amountToMove, descriptionBase)
                    ReceiptAllocation.Unapplied -> Unit
                }

                updateReceiptStatus(updatedReceipt)
            }

            AllocationMoveSummary(allocations.size, receiptIds.size)
        }
    }

    suspend fun voidAllocations(
        allocationIds: List<Long>,
        reason: String?
    ): AllocationMoveSummary {
        if (allocationIds.isEmpty()) return AllocationMoveSummary(0, 0)
        return database.withTransaction {
            val allocations =
                allocationDao.getByIds(allocationIds)
                    .filter { it.status == PaymentAllocationStatus.APPLIED }
            if (allocations.isEmpty()) return@withTransaction AllocationMoveSummary(0, 0)

            val receiptIds = allocations.map { it.receiptId }.distinct()
            val now = Clock.System.now().toEpochMilliseconds()
            allocations.forEach { allocation ->
                val entryId =
                    allocation.accountEntryId?.let { accountEntryId ->
                        accountingDao.insertAccountEntry(
                            AccountEntryEntity(
                                orderId = allocation.orderId,
                                customerId = allocation.customerId,
                                type = EntryType.REVERSAL,
                                amount = allocation.amount,
                                date = now,
                                description = buildVoidDescription(allocation.receiptId, reason)
                            )
                        )
                    }
                allocationDao.markVoided(
                    id = allocation.id,
                    status = PaymentAllocationStatus.VOIDED,
                    reversalEntryId = entryId,
                    voidedAt = now,
                    voidReason = reason
                )
            }
            receiptDao.getByIds(receiptIds).forEach { receipt ->
                updateReceiptStatus(receipt)
            }
            AllocationMoveSummary(allocations.size, receiptIds.size)
        }
    }

    private suspend fun applyToOrder(
        receipt: PaymentReceiptEntity,
        orderId: Long,
        descriptionBase: String
    ): BigDecimal {
        val order = orderDao.getOrderById(orderId) ?: return BigDecimal.ZERO
        val resolvedCustomerId = receipt.customerId ?: order.customerId
        val paid = accountingDao.getPaidForOrder(orderId)
        val remaining = order.totalAmount.subtract(paid)
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)
        if (remaining <= BigDecimal.ZERO) {
            return applyToCustomerCredit(receipt, resolvedCustomerId, descriptionBase, receipt.amount)
        }
        return if (receipt.amount <= remaining) {
            val entryId = insertCreditEntry(order.id, resolvedCustomerId, receipt.amount, description, now)
            insertAllocation(
                receiptId = receipt.id,
                orderId = order.id,
                customerId = resolvedCustomerId,
                amount = receipt.amount,
                type = PaymentAllocationType.ORDER,
                accountEntryId = entryId,
                createdAt = now
            )
            receipt.amount
        } else {
            val entryId =
                insertCreditEntry(
                    order.id,
                    resolvedCustomerId,
                    remaining,
                    "$description (order)",
                    now
                )
            insertAllocation(
                receiptId = receipt.id,
                orderId = order.id,
                customerId = resolvedCustomerId,
                amount = remaining,
                type = PaymentAllocationType.ORDER,
                accountEntryId = entryId,
                createdAt = now
            )
            val extra = receipt.amount.subtract(remaining)
            applyToCustomerCredit(receipt, resolvedCustomerId, descriptionBase, extra)
            receipt.amount
        }
    }

    private suspend fun applyToOldestOrders(
        receipt: PaymentReceiptEntity,
        customerId: Long,
        descriptionBase: String
    ): BigDecimal {
        var remainingPayment = receipt.amount
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)

        var offset = 0
        while (remainingPayment > BigDecimal.ZERO) {
            val orders =
                orderDao.getOpenOrdersByCustomerPaged(
                    customerId = customerId,
                    limit = OLDEST_ORDERS_PAGE_SIZE,
                    offset = offset
                )
            if (orders.isEmpty()) break

            for (order in orders) {
                if (remainingPayment <= BigDecimal.ZERO) break
                val paidSoFar = accountingDao.getPaidForOrder(order.id)
                val remaining = order.totalAmount - paidSoFar
                if (remaining <= BigDecimal.ZERO) continue

                val applied = if (remainingPayment > remaining) remaining else remainingPayment
                val entryId = insertCreditEntry(order.id, customerId, applied, description, now)
                insertAllocation(
                    receiptId = receipt.id,
                    orderId = order.id,
                    customerId = customerId,
                    amount = applied,
                    type = PaymentAllocationType.ORDER,
                    accountEntryId = entryId,
                    createdAt = now
                )
                remainingPayment -= applied
            }
            if (orders.size < OLDEST_ORDERS_PAGE_SIZE) {
                break
            }
            offset += OLDEST_ORDERS_PAGE_SIZE
        }

        if (remainingPayment > BigDecimal.ZERO) {
            applyToCustomerCredit(receipt, customerId, descriptionBase, remainingPayment)
        }

        return receipt.amount
    }

    private suspend fun applyAmountToOrder(
        receipt: PaymentReceiptEntity,
        orderId: Long,
        amount: BigDecimal,
        descriptionBase: String
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO
        val order = orderDao.getOrderById(orderId) ?: return BigDecimal.ZERO
        val resolvedCustomerId = receipt.customerId ?: order.customerId
        val paid = accountingDao.getPaidForOrder(orderId)
        val remaining = order.totalAmount.subtract(paid)
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)
        if (remaining <= BigDecimal.ZERO) {
            return applyAmountToCustomerCredit(receipt, resolvedCustomerId, amount, descriptionBase)
        }
        return if (amount <= remaining) {
            val entryId = insertCreditEntry(order.id, resolvedCustomerId, amount, description, now)
            insertAllocation(
                receiptId = receipt.id,
                orderId = order.id,
                customerId = resolvedCustomerId,
                amount = amount,
                type = PaymentAllocationType.ORDER,
                accountEntryId = entryId,
                createdAt = now
            )
            amount
        } else {
            val entryId =
                insertCreditEntry(
                    order.id,
                    resolvedCustomerId,
                    remaining,
                    "$description (order)",
                    now
                )
            insertAllocation(
                receiptId = receipt.id,
                orderId = order.id,
                customerId = resolvedCustomerId,
                amount = remaining,
                type = PaymentAllocationType.ORDER,
                accountEntryId = entryId,
                createdAt = now
            )
            val extra = amount.subtract(remaining)
            applyAmountToCustomerCredit(receipt, resolvedCustomerId, extra, descriptionBase)
            amount
        }
    }

    private suspend fun applyAmountToOldestOrders(
        receipt: PaymentReceiptEntity,
        customerId: Long,
        amount: BigDecimal,
        descriptionBase: String
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO
        var remainingPayment = amount
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)

        var offset = 0
        while (remainingPayment > BigDecimal.ZERO) {
            val orders =
                orderDao.getOpenOrdersByCustomerPaged(
                    customerId = customerId,
                    limit = OLDEST_ORDERS_PAGE_SIZE,
                    offset = offset
                )
            if (orders.isEmpty()) break

            for (order in orders) {
                if (remainingPayment <= BigDecimal.ZERO) break
                val paidSoFar = accountingDao.getPaidForOrder(order.id)
                val remaining = order.totalAmount - paidSoFar
                if (remaining <= BigDecimal.ZERO) continue

                val applied = if (remainingPayment > remaining) remaining else remainingPayment
                val entryId = insertCreditEntry(order.id, customerId, applied, description, now)
                insertAllocation(
                    receiptId = receipt.id,
                    orderId = order.id,
                    customerId = customerId,
                    amount = applied,
                    type = PaymentAllocationType.ORDER,
                    accountEntryId = entryId,
                    createdAt = now
                )
                remainingPayment -= applied
            }
            if (orders.size < OLDEST_ORDERS_PAGE_SIZE) {
                break
            }
            offset += OLDEST_ORDERS_PAGE_SIZE
        }

        if (remainingPayment > BigDecimal.ZERO) {
            applyAmountToCustomerCredit(receipt, customerId, remainingPayment, descriptionBase)
        }

        return amount
    }

    private suspend fun applyAmountToCustomerCredit(
        receipt: PaymentReceiptEntity,
        customerId: Long?,
        amount: BigDecimal,
        descriptionBase: String
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)
        val entryId = insertCreditEntry(null, customerId, amount, description, now)
        insertAllocation(
            receiptId = receipt.id,
            orderId = null,
            customerId = customerId,
            amount = amount,
            type = PaymentAllocationType.CUSTOMER_CREDIT,
            accountEntryId = entryId,
            createdAt = now
        )
        return amount
    }

    private suspend fun updateReceiptStatus(receipt: PaymentReceiptEntity) {
        if (receipt.status == PaymentReceiptStatus.VOIDED) return
        val allocations = allocationDao.getByReceiptId(receipt.id)
        val appliedTotal =
            allocations.filter { it.status == PaymentAllocationStatus.APPLIED }
                .fold(BigDecimal.ZERO) { acc, allocation -> acc + allocation.amount }
        val status =
            when {
                appliedTotal <= BigDecimal.ZERO -> PaymentReceiptStatus.UNAPPLIED
                appliedTotal < receipt.amount -> PaymentReceiptStatus.PARTIAL
                else -> PaymentReceiptStatus.APPLIED
            }
        if (receipt.status != status) {
            receiptDao.update(receipt.copy(status = status))
        }
    }

    private suspend fun resolveCustomerIdForTarget(
        receipt: PaymentReceiptEntity,
        allocation: ReceiptAllocation
    ): Long? {
        return when (allocation) {
            is ReceiptAllocation.Order -> orderDao.getOrderById(allocation.orderId)?.customerId
            is ReceiptAllocation.OldestOrders -> allocation.customerId
            is ReceiptAllocation.CustomerCredit -> allocation.customerId
            ReceiptAllocation.Unapplied -> receipt.customerId
        }
    }

    private suspend fun applyToCustomerCredit(
        receipt: PaymentReceiptEntity,
        customerId: Long?,
        descriptionBase: String,
        amount: BigDecimal
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO
        val now = receipt.receivedAt
        val description = buildReceiptDescription(receipt.id, descriptionBase)
        val entryId = insertCreditEntry(null, customerId, amount, description, now)
        insertAllocation(
            receiptId = receipt.id,
            orderId = null,
            customerId = customerId,
            amount = amount,
            type = PaymentAllocationType.CUSTOMER_CREDIT,
            accountEntryId = entryId,
            createdAt = now
        )
        return amount
    }

    private suspend fun insertAllocation(
        receiptId: Long,
        orderId: Long?,
        customerId: Long?,
        amount: BigDecimal,
        type: PaymentAllocationType,
        accountEntryId: Long?,
        createdAt: Long
    ) {
        allocationDao.insert(
            PaymentAllocationEntity(
                receiptId = receiptId,
                orderId = orderId,
                customerId = customerId,
                amount = amount,
                type = type,
                status = PaymentAllocationStatus.APPLIED,
                accountEntryId = accountEntryId,
                reversalEntryId = null,
                createdAt = createdAt,
                voidedAt = null,
                voidReason = null
            )
        )
    }

    private suspend fun insertCreditEntry(
        orderId: Long?,
        customerId: Long?,
        amount: BigDecimal,
        description: String,
        date: Long
    ): Long {
        return accountingDao.insertAccountEntry(
            AccountEntryEntity(
                orderId = orderId,
                customerId = customerId,
                type = EntryType.CREDIT,
                amount = amount,
                date = date,
                description = description
            )
        )
    }

    private fun buildReceiptDescription(receiptId: Long, descriptionBase: String): String {
        val trimmed = descriptionBase.trim().ifBlank { "Payment" }
        return "Receipt #$receiptId - $trimmed"
    }

    private fun buildVoidDescription(receiptId: Long, reason: String?): String {
        val detail = reason?.trim().takeIf { !it.isNullOrBlank() }?.let { ": $it" } ?: ""
        return "Void receipt #$receiptId$detail"
    }

    private fun buildReallocateDescription(receiptId: Long): String {
        return "Reallocated receipt #$receiptId"
    }

    companion object {
        private const val OLDEST_ORDERS_PAGE_SIZE = 250
    }
}
