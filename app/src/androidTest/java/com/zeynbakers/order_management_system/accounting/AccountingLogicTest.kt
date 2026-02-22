package com.zeynbakers.order_management_system.accounting

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.core.db.AppDatabase
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class AccountingLogicTest {

    private lateinit var db: AppDatabase

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
        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 1, 2),
                notes = "Test",
                totalAmount = BigDecimal("100.00"),
                customerId = 1L
            )
        )

        val date = LocalDate(2026, 1, 2)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = 1L,
            amount = BigDecimal("100.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )
        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = 1L,
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

        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 2, 3),
                notes = "Test",
                totalAmount = BigDecimal("80.00"),
                customerId = 1L
            )
        )

        val date = LocalDate(2026, 2, 3)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = 1L,
            amount = BigDecimal("80.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )

        processor.cancelOrder(orderId)

        val entries = accountingDao.getEntriesForOrder(orderId).filter { it.type == EntryType.DEBIT }
        assertEquals(0, entries.size)
    }

    @Test
    fun recordPayment_splitsOverpaymentToCustomerCredit() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())

        val orderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 3, 4),
                notes = "Test",
                totalAmount = BigDecimal("100.00"),
                customerId = 1L
            )
        )

        val date = LocalDate(2026, 3, 4)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        accountingDao.upsertDebitForOrder(
            orderId = orderId,
            customerId = 1L,
            amount = BigDecimal("100.00"),
            date = date,
            description = "Charge: Order #$orderId"
        )

        viewModel.recordPaymentInternal(
            customerId = 1L,
            amount = BigDecimal("150.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = orderId
        )

        val paidForOrder = accountingDao.getPaidForOrder(orderId)
        val balance = accountingDao.getCustomerBalance(1L)

        assertEquals(BigDecimal("100.00"), paidForOrder)
        assertEquals(BigDecimal("-50.00"), balance)
    }

    @Test
    fun recordPayment_autoAllocatesAcrossOldestOrders() = runBlocking {
        val orderDao = db.orderDao()
        val accountingDao = db.accountingDao()
        val viewModel = CustomerAccountsViewModel(db, ApplicationProvider.getApplicationContext())

        val firstOrderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 4, 1),
                notes = "Older",
                totalAmount = BigDecimal("100.00"),
                customerId = 1L
            )
        )
        val secondOrderId = orderDao.insert(
            OrderEntity(
                orderDate = LocalDate(2026, 4, 2),
                notes = "Newer",
                totalAmount = BigDecimal("200.00"),
                customerId = 1L
            )
        )

        viewModel.recordPaymentInternal(
            customerId = 1L,
            amount = BigDecimal("150.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = null
        )

        val firstPaid = accountingDao.getPaidForOrder(firstOrderId)
        val secondPaid = accountingDao.getPaidForOrder(secondOrderId)
        val unallocatedCredits = accountingDao.getLedgerForCustomer(1L)
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

        val firstOrderDate = LocalDate(2026, 5, 1)
        val secondOrderDate = LocalDate(2026, 5, 2)

        val firstOrderId = orderDao.insert(
            OrderEntity(
                orderDate = firstOrderDate,
                notes = "First",
                totalAmount = BigDecimal("2000.00"),
                customerId = 1L
            )
        )
        val secondOrderId = orderDao.insert(
            OrderEntity(
                orderDate = secondOrderDate,
                notes = "Second",
                totalAmount = BigDecimal("1500.00"),
                customerId = 1L
            )
        )

        accountingDao.upsertDebitForOrder(
            orderId = firstOrderId,
            customerId = 1L,
            amount = BigDecimal("2000.00"),
            date = firstOrderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: Order #$firstOrderId"
        )
        accountingDao.upsertDebitForOrder(
            orderId = secondOrderId,
            customerId = 1L,
            amount = BigDecimal("1500.00"),
            date = secondOrderDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            description = "Charge: Order #$secondOrderId"
        )

        viewModel.recordPaymentInternal(
            customerId = 1L,
            amount = BigDecimal("1000.00"),
            method = com.zeynbakers.order_management_system.accounting.data.PaymentMethod.CASH,
            note = "",
            orderId = null
        )

        viewModel.markBadDebt(
            customerId = 1L,
            amount = BigDecimal("1200.00"),
            note = "Unreachable"
        )

        // markBadDebt is async via viewModelScope; wait for persistence.
        var synced = false
        for (attempt in 0 until 20) {
            if (accountingDao.getCustomerBalance(1L) == BigDecimal("1300.00")) {
                synced = true
                break
            }
            delay(50)
        }
        if (!synced) delay(100)

        val firstPaid = accountingDao.getPaidForOrder(firstOrderId)
        val secondPaid = accountingDao.getPaidForOrder(secondOrderId)
        val balance = accountingDao.getCustomerBalance(1L)
        val customerLevelWriteOffs =
            accountingDao.getLedgerForCustomer(1L)
                .filter { it.type == EntryType.WRITE_OFF && it.orderId == null }

        assertEquals(BigDecimal("2000.00"), firstPaid)
        assertEquals(BigDecimal("200.00"), secondPaid)
        assertEquals(BigDecimal("1300.00"), balance)
        assertEquals(0, customerLevelWriteOffs.size)
    }
}
