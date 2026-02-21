package com.zeynbakers.order_management_system.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.ui.AllocationMode
import com.zeynbakers.order_management_system.accounting.ui.DuplicateState
import com.zeynbakers.order_management_system.accounting.ui.MpesaAllocationSheet
import com.zeynbakers.order_management_system.accounting.ui.MpesaTransactionUi
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerFinanceSummary
import com.zeynbakers.order_management_system.customer.ui.CustomerOrderUi
import com.zeynbakers.order_management_system.customer.ui.OrderEffectiveStatus
import com.zeynbakers.order_management_system.customer.ui.OrderPaymentState
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilitySmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarPrimaryActionsMeetTouchTarget() {
        composeRule.setContent {
            MaterialTheme {
                CalendarScreen(
                    days = emptyList(),
                    currentYear = 2026,
                    currentMonth = 2,
                    baseYear = 2026,
                    baseMonth = 2,
                    monthSnapshots = emptyMap(),
                    monthTotal = BigDecimal.ZERO,
                    monthBadgeCount = 0,
                    selectedDate = LocalDate(2026, 2, 7),
                    onSelectDate = {},
                    onOpenDay = {},
                    onSaveOrder = { _, _, _, _, _, _ -> },
                    searchCustomers = { emptyList() },
                    onSummaryClick = {},
                    onOpenMore = {},
                    onMonthSettled = { _, _ -> },
                    openQuickAddDate = null,
                    onQuickAddConsumed = {}
                )
            }
        }

        val minTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }

        val addOrderNode = composeRule.onNodeWithContentDescription("Add order")
            .assert(SemanticsMatcher("exists") { true })
            .fetchSemanticsNode()
        assertTrue(addOrderNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(addOrderNode.boundsInRoot.height >= minTouchTargetPx)

        val todayNode = composeRule.onNodeWithContentDescription("Today")
            .assert(SemanticsMatcher("exists") { true })
            .fetchSemanticsNode()
        assertTrue(todayNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(todayNode.boundsInRoot.height >= minTouchTargetPx)
    }

    @Test
    fun dayDetailFiltersStayDiscoverableAtLargeFontScale() {
        val date = LocalDate(2026, 2, 7)
        val order =
            OrderEntity(
                id = 1L,
                orderDate = date,
                notes = "Bread order",
                totalAmount = BigDecimal("1000.00")
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                MaterialTheme {
                    DayDetailScreen(
                        date = date,
                        orders = listOf(order),
                        dayTotal = BigDecimal("1000.00"),
                        customerNames = emptyMap(),
                        orderPaidAmounts = emptyMap(),
                        onBack = {},
                        onSaveOrder = { _, _, _, _, _, _ -> },
                        onDeleteOrder = {},
                        loadOrderPaymentAllocations = { emptyList() },
                        loadMoveOrderOptions = { _, _ -> emptyList() },
                        onDeleteOrderWithPayments = { _, _, _, _, _, _ -> true },
                        onOrderPaymentHistory = {},
                        onReceivePayment = {},
                        loadCustomerById = { null },
                        searchCustomers = { emptyList() },
                        draft = null,
                        onDraftChange = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("More filters").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Search orders").assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun customerPrimaryActionsStayDiscoverableAtLargeFontScale() {
        val summary =
            CustomerAccountSummary(
                customerId = 1L,
                name = "Jane Doe",
                phone = "0712345678",
                billed = BigDecimal("1500.00"),
                paid = BigDecimal("500.00"),
                balance = BigDecimal("1000.00")
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                MaterialTheme {
                    CustomerListScreen(
                        query = "",
                        summaries = listOf(summary),
                        onQueryChange = {},
                        onCustomerClick = {},
                        onBack = {},
                        onAddCustomer = {},
                        onPaymentHistory = {},
                        onRecordPayment = {},
                        onAddOrder = {},
                        onEditCustomer = {},
                        onArchiveCustomer = {},
                        onRestoreCustomer = {},
                        showBack = false
                    )
                }
            }
        }

        composeRule.onNodeWithText("Pay").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Order").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Message").assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText("Search customers").assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun summaryCopyActionMeetsTouchTarget() {
        val date = LocalDate(2026, 2, 7)
        val order =
            OrderEntity(
                id = 10L,
                orderDate = date,
                notes = "Bread 2",
                totalAmount = BigDecimal("1000.00")
            )

        composeRule.setContent {
            MaterialTheme {
                SummaryScreen(
                    monthLabel = "February 2026",
                    monthTotal = BigDecimal("1000.00"),
                    initialDate = date,
                    orders = listOf(order),
                    rangeTotal = BigDecimal("1000.00"),
                    customerNames = emptyMap(),
                    onAnchorDateChange = {},
                    onLoadRange = { _, _ -> },
                    onBack = {}
                )
            }
        }

        val minTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }
        val copyNode = composeRule.onNodeWithContentDescription("Copy chef list")
            .assert(SemanticsMatcher("exists") { true })
            .fetchSemanticsNode()
        assertTrue(copyNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(copyNode.boundsInRoot.height >= minTouchTargetPx)
    }

    @Test
    fun moneyAllocationActionsStayDiscoverableAtLargeFontScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item =
            MpesaTransactionUi(
                key = "A1",
                transactionCode = "QWE123",
                amount = BigDecimal("1000.00"),
                senderName = "Jane",
                senderPhone = "0712345678",
                receivedAt = 0L,
                rawText = "sample",
                duplicateState = DuplicateState.NONE,
                existingReceiptId = null,
                existingReceiptStatus = null,
                suggestedCustomerId = null,
                suggestedCustomerName = null,
                customerConfidence = 0,
                orderSuggestions = emptyList(),
                selectedOrderId = null,
                selectedCustomerId = null,
                allocationMode = AllocationMode.OLDEST_ORDERS,
                selected = false
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                MaterialTheme {
                    MpesaAllocationSheet(
                        item = item,
                        searchCustomers = { emptyList() },
                        onSelectCustomer = {},
                        onSelectOrder = {},
                        onSelectAllocationMode = {},
                        onViewExisting = {},
                        onMoveExisting = {},
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.money_allocation)).assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText(context.getString(R.string.money_oldest_orders)).assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText(context.getString(R.string.money_customer_credit)).assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText(context.getString(R.string.money_search_name_or_phone)).assert(SemanticsMatcher("exists") { true })
    }

    @Test
    fun moneyAllocationModeChipsMeetTouchTarget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item =
            MpesaTransactionUi(
                key = "A2",
                transactionCode = "QWE124",
                amount = BigDecimal("800.00"),
                senderName = "John",
                senderPhone = "0700000000",
                receivedAt = 0L,
                rawText = "sample",
                duplicateState = DuplicateState.NONE,
                existingReceiptId = null,
                existingReceiptStatus = null,
                suggestedCustomerId = null,
                suggestedCustomerName = null,
                customerConfidence = 0,
                orderSuggestions = emptyList(),
                selectedOrderId = null,
                selectedCustomerId = null,
                allocationMode = AllocationMode.OLDEST_ORDERS,
                selected = false
            )

        composeRule.setContent {
            MaterialTheme {
                MpesaAllocationSheet(
                    item = item,
                    searchCustomers = { emptyList() },
                    onSelectCustomer = {},
                    onSelectOrder = {},
                    onSelectAllocationMode = {},
                    onViewExisting = {},
                    onMoveExisting = {},
                    onDismiss = {}
                )
            }
        }

        val minTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }

        val oldestChip = composeRule.onNodeWithText(context.getString(R.string.money_oldest_orders))
            .fetchSemanticsNode()
        assertTrue(oldestChip.boundsInRoot.height >= minTouchTargetPx)

        val creditChip = composeRule.onNodeWithText(context.getString(R.string.money_customer_credit))
            .fetchSemanticsNode()
        assertTrue(creditChip.boundsInRoot.height >= minTouchTargetPx)
    }

    @Test
    fun customerDetailActionsStayDiscoverableAtLargeFontScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val date = LocalDate(2026, 2, 7)
        val customer = CustomerEntity(id = 1L, name = "Jane Doe", phone = "0712345678")
        val order = OrderEntity(id = 5L, orderDate = date, notes = "Bread", totalAmount = BigDecimal("1000.00"))
        val orderUi =
            CustomerOrderUi(
                order = order,
                paidAmount = BigDecimal.ZERO,
                lastPaymentAt = null,
                lastActivityAt = 0L,
                paymentState = OrderPaymentState.UNPAID,
                effectiveStatus = OrderEffectiveStatus.OPEN,
                statusOverride = null
            )
        val ledgerEntry =
            AccountEntryEntity(
                id = 1L,
                orderId = order.id,
                customerId = customer.id,
                type = EntryType.DEBIT,
                amount = BigDecimal("1000.00"),
                date = 0L,
                description = "Bread order"
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                MaterialTheme {
                    CustomerDetailScreen(
                        customer = customer,
                        ledger = listOf(ledgerEntry),
                        balance = BigDecimal("1000.00"),
                        financeSummary = CustomerFinanceSummary(
                            orderTotal = BigDecimal("1000.00"),
                            paidToOrders = BigDecimal.ZERO,
                            availableCredit = BigDecimal.ZERO,
                            netBalance = BigDecimal("1000.00")
                        ),
                        orders = listOf(orderUi),
                        orderLabels = mapOf(order.id to "Order #5"),
                        onBack = {},
                        onPaymentHistory = {},
                        onOpenStatement = {},
                        onReceivePayment = {},
                        onOrderPaymentHistory = {},
                        onUpdateOrderStatusOverride = { _, _ -> },
                        onWriteOffOrder = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.customer_action_record_payment))
            .assert(SemanticsMatcher("exists") { true })
        composeRule.onNodeWithText(context.getString(R.string.customer_detail_search_statement))
            .assert(SemanticsMatcher("exists") { true })

        val minTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }
        val statementNode = composeRule.onNodeWithContentDescription(context.getString(R.string.customer_detail_statement))
            .fetchSemanticsNode()
        assertTrue(statementNode.boundsInRoot.width >= minTouchTargetPx)
        assertTrue(statementNode.boundsInRoot.height >= minTouchTargetPx)
    }
}
