package com.zeynbakers.order_management_system.accounting

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.accounting.domain.PaymentReceiptProcessor
import com.zeynbakers.order_management_system.accounting.domain.ReceiptAllocation
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.domain.OrderProcessor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class AccountingLogicTest {

    private lateinit var db: AppDatabase

    private suspend fun createCustomer(
        name: String = "Test Customer",
        phone: String = "+254700000000"
    ): Long =
        db.customerDao().insert(
            CustomerEntity(
                name = name,
                phone = phone
            )
        )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertDebit_isIdempotent() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val customerId = createCustomer(phone = "+254700000101")
        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 1, 2),
                notes = "Test",
                totalAmount = BigDecimal("100.00"),
                customerId = customerId
            )
        )

        val date = LocalDate(2026, 1, 2)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = customerId,
            amount = BigDecimal("100.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )
        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = customerId,
            amount = BigDecimal("120.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )

        val entries = accountingDao.getEntriesForOrder(orderId).filter { it.type == EntryType.DEBIT }
        assertEquals(1, entries.size)
        assertEquals(BigDecimal("120.00"), entries.first().amount)
    }

    @Test
    fun cancelOrder_removesDebitEntry() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val processor = OrderProcessor(orderDao, accountingDao)
        val customerId = createCustomer(phone = "+254700000102")

        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 2, 3),
                notes = "Test",
                totalAmount = BigDecimal("80.00"),
                customerId = customerId
            )
        )

        val date = LocalDate(2026, 2, 3)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = customerId,
            amount = BigDecimal("80.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )

        processor.cancelOrder(orderId)

        val entries = accountingDao.getEntriesForOrder(orderId).filter { it.type == EntryType.DEBIT }
        assertEquals(0, entries.size)
    }

    @Test
    fun searchCustomers_loadsAllSummaryPages() = runBlocking {
        val customerDao = db.customerDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())
        val totalCustomers = 1_805
        val customers =
            (1..totalCustomers).map { index ->
                CustomerEntity(
                    name = "Imported Customer ${index.toString().padStart(4, '0')}",
                    phone = "+2547${index.toString().padStart(8, '0')}"
                )
            }
        customerDao.insertAll(customers)

        viewModel.searchCustomers("")

        var loadedAll = false
        repeat(40) {
            if (viewModel.summaries.value.size == totalCustomers) {
                loadedAll = true
                return@repeat
            }
            delay(50)
        }

        assertTrue(loadedAll)
        assertEquals(totalCustomers, viewModel.summaries.value.size)
    }

    @Test
    fun customerSummary_marksHasOrders_whenLedgerTotalsAreZero() = runBlocking {
        val customerDao = db.customerDao()
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()

        val customerId =
            customerDao.insert(
                CustomerEntity(
                    name = "Has Order",
                    phone = "+254700000001"
                )
            )

        orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 2, 1),
                notes = "No ledger yet",
                totalAmount = BigDecimal("100.00"),
                customerId = customerId
            )
        )

        val summaries = accountingDao.getCustomerAccountSummaries("%")
        val summary = summaries.firstOrNull { it.customerId == customerId }
        assertTrue(summary != null)
        assertTrue(summary!!.hasOrders)
        assertEquals(0, summary.billed.compareTo(BigDecimal.ZERO))
        assertEquals(0, summary.paid.compareTo(BigDecimal.ZERO))
        assertEquals(0, summary.balance.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun recordPayment_splitsOverpaymentToCustomerCredit() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())
        val customerId = createCustomer(phone = "+254700000103")

        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 3, 4),
                notes = "Test",
                totalAmount = BigDecimal("100.00"),
                customerId = customerId
            )
        )

        val date = LocalDate(2026, 3, 4)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = customerId,
            amount = BigDecimal("100.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )

        viewModel.recordPaymentInternal(
            customerId = customerId,
            amount = BigDecimal("150.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = orderId
        )

        val paidForOrder = accountingDao.getPaidForOrder(orderId)
        val balance = accountingDao.getCustomerBalance(customerId)

        assertEquals(BigDecimal("100.00"), paidForOrder)
        assertEquals(BigDecimal("-50.00"), balance)
    }

    @Test
    fun recordPayment_autoAllocatesAcrossOldestOrders() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())
        val customerId = createCustomer(phone = "+254700000104")

        val firstOrderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 4, 1),
                notes = "Older",
                totalAmount = BigDecimal("100.00"),
                customerId = customerId
            )
        )
        val secondOrderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 4, 2),
                notes = "Newer",
                totalAmount = BigDecimal("200.00"),
                customerId = customerId
            )
        )

        viewModel.recordPaymentInternal(
            customerId = customerId,
            amount = BigDecimal("150.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = null
        )

        val firstPaid = accountingDao.getPaidForOrder(firstOrderId)
        val secondPaid = accountingDao.getPaidForOrder(secondOrderId)
        val unallocatedCredits = accountingDao.getLedgerForCustomer(customerId)
            .filter { it.type == EntryType.CREDIT && it.orderId == null }

        assertEquals(BigDecimal("100.00"), firstPaid)
        assertEquals(BigDecimal("50.00"), secondPaid)
        assertEquals(0, unallocatedCredits.size)
    }

    @Test
    fun markBadDebt_allocatesToOldestOpenOrders_beforeCustomerLevel() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())
        val customerId = createCustomer(phone = "+254700000105")

        val firstOrderDate = LocalDate(2026, 5, 1)
        val secondOrderDate = LocalDate(2026, 5, 2)

        val firstOrderId = orderDao.insert(
            OrderEntity(
                orderDate = firstOrderDate,
                notes = "First",
                totalAmount = BigDecimal("2000.00"),
                customerId = customerId
            )
        )
        val secondOrderId = orderDao.insert(
            OrderEntity(
                orderDate = secondOrderDate,
                notes = "Second",
                totalAmount = BigDecimal("1500.00"),
                customerId = customerId
            )
        )

        accountingDao.upsertDebitForOrder(
            orderId = firstOrderId,
            customerId = customerId,
            amount = BigDecimal("2000.00"),
            date = firstOrderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: Order #$firstOrderId"
        )
        accountingDao.upsertDebitForOrder(
            orderId = secondOrderId,
            customerId = customerId,
            amount = BigDecimal("1500.00"),
            date = secondOrderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: Order #$secondOrderId"
        )

        viewModel.recordPaymentInternal(
            customerId = customerId,
            amount = BigDecimal("1000.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = null
        )

        viewModel.markBadDebt(
            customerId = customerId,
            amount = BigDecimal("1200.00"),
            note = "Unreachable"
        )

        // markBadDebt is async via viewModelScope; wait for persistence.
        var synced = false
        for (attempt in 0 until 20) {
            if (accountingDao.getCustomerBalance(customerId) == BigDecimal("1300.00")) {
                synced = true
                break
            }
            delay(50)
        }
        if (!synced) delay(100)

        val firstPaid = accountingDao.getPaidForOrder(firstOrderId)
        val secondPaid = accountingDao.getPaidForOrder(secondOrderId)
        val balance = accountingDao.getCustomerBalance(customerId)
        val customerLevelWriteOffs =
            accountingDao.getLedgerForCustomer(customerId)
                .filter { it.type == EntryType.WRITE_OFF && it.orderId == null }

        assertEquals(BigDecimal("2000.00"), firstPaid)
        assertEquals(BigDecimal("200.00"), secondPaid)
        assertEquals(BigDecimal("1300.00"), balance)
        assertEquals(0, customerLevelWriteOffs.size)
    }

    @Test
    fun moveAllocations_fullReceiptMove_retargetsEntireReceipts() = runBlocking {
        val customerDao = db.customerDao()
        val orderDao = db.orderDao()
        val allocationDao = db.paymentAllocationDao()
        val receiptDao = db.paymentReceiptDao()
        val processor = PaymentReceiptProcessor(db)

        val customerId =
            customerDao.insert(
                CustomerEntity(
                    name = "Move Test",
                    phone = "+254700111222"
                )
            )
        val sourceOrderId =
            orderDao.insert(
                OrderEntity(
                    orderDate = LocalDate(2026, 6, 1),
                    notes = "Source",
                    totalAmount = BigDecimal("500.00"),
                    customerId = customerId
                )
            )
        val targetOrderId =
            orderDao.insert(
                OrderEntity(
                    orderDate = LocalDate(2026, 6, 2),
                    notes = "Target",
                    totalAmount = BigDecimal("500.00"),
                    customerId = customerId
                )
            )

        val receipt1 =
            processor.createReceipt(
                amount = BigDecimal("100.00"),
                receivedAt = 10L,
                method = PaymentMethod.MPESA,
                transactionCode = "MOVE001",
                hash = "hash-move-1",
                senderName = "Sender 1",
                senderPhone = "+254700111222",
                rawText = "move 1",
                customerId = customerId,
                note = null
            )
        processor.createAndApplyReceipt(
            receipt = receipt1,
            descriptionBase = "Move test 1",
            allocation = ReceiptAllocation.Order(sourceOrderId)
        )

        val receipt2 =
            processor.createReceipt(
                amount = BigDecimal("150.00"),
                receivedAt = 20L,
                method = PaymentMethod.MPESA,
                transactionCode = "MOVE002",
                hash = "hash-move-2",
                senderName = "Sender 2",
                senderPhone = "+254700111222",
                rawText = "move 2",
                customerId = customerId,
                note = null
            )
        processor.createAndApplyReceipt(
            receipt = receipt2,
            descriptionBase = "Move test 2",
            allocation = ReceiptAllocation.Order(sourceOrderId)
        )

        val selectedAllocationIds =
            allocationDao.getByOrderId(sourceOrderId)
                .filter { it.status == PaymentAllocationStatus.APPLIED }
                .map { it.id }

        val summary =
            processor.moveAllocations(
                allocationIds = selectedAllocationIds,
                target = ReceiptAllocation.Order(targetOrderId),
                descriptionBase = "bulk move",
                moveFullReceipts = true
            )

        assertEquals(2, summary.movedAllocations)
        assertEquals(2, summary.affectedReceipts)
        assertEquals(
            0,
            allocationDao.getByOrderId(sourceOrderId)
                .count { it.status == PaymentAllocationStatus.APPLIED }
        )
        assertEquals(
            2,
            allocationDao.getByOrderId(targetOrderId)
                .count { it.status == PaymentAllocationStatus.APPLIED }
        )
        val movedReceipts = receiptDao.getByIds(listOf(receipt1.id, receipt2.id))
        assertTrue(movedReceipts.all { it.status == PaymentReceiptStatus.APPLIED })
    }

    @Test
    fun voidAllocations_setsReceiptUnapplied_whenAllAppliedAllocationsVoided() = runBlocking {
        val customerDao = db.customerDao()
        val orderDao = db.orderDao()
        val allocationDao = db.paymentAllocationDao()
        val receiptDao = db.paymentReceiptDao()
        val processor = PaymentReceiptProcessor(db)

        val customerId =
            customerDao.insert(
                CustomerEntity(
                    name = "Void Test",
                    phone = "+254700333444"
                )
            )
        val orderId =
            orderDao.insert(
                OrderEntity(
                    orderDate = LocalDate(2026, 7, 1),
                    notes = "Void source",
                    totalAmount = BigDecimal("300.00"),
                    customerId = customerId
                )
            )

        val receipt =
            processor.createReceipt(
                amount = BigDecimal("120.00"),
                receivedAt = 30L,
                method = PaymentMethod.MPESA,
                transactionCode = "VOID001",
                hash = "hash-void-1",
                senderName = "Sender 3",
                senderPhone = "+254700333444",
                rawText = "void 1",
                customerId = customerId,
                note = null
            )
        processor.createAndApplyReceipt(
            receipt = receipt,
            descriptionBase = "Void test",
            allocation = ReceiptAllocation.Order(orderId)
        )

        val allocationId =
            allocationDao.getByOrderId(orderId)
                .first { it.status == PaymentAllocationStatus.APPLIED }
                .id

        val summary = processor.voidAllocations(listOf(allocationId), reason = "Regression test")

        assertEquals(1, summary.movedAllocations)
        assertEquals(1, summary.affectedReceipts)
        val allocation = allocationDao.getByIds(listOf(allocationId)).first()
        assertEquals(PaymentAllocationStatus.VOIDED, allocation.status)
        val updatedReceipt = receiptDao.getById(receipt.id)
        assertEquals(PaymentReceiptStatus.UNAPPLIED, updatedReceipt?.status)
    }
}
