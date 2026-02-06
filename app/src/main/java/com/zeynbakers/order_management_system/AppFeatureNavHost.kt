package com.zeynbakers.order_management_system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zeynbakers.order_management_system.accounting.ui.LedgerViewModel
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryScreen
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen
import com.zeynbakers.order_management_system.core.ui.MoneyScreen
import com.zeynbakers.order_management_system.core.ui.MoneyTab
import com.zeynbakers.order_management_system.core.widget.WidgetUpdater
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerFinanceSummary
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerOrderUi
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import com.zeynbakers.order_management_system.customer.ui.ImportContactsScreen
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.ui.CalendarDayUi
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.MonthKey
import com.zeynbakers.order_management_system.order.ui.MonthSnapshot
import com.zeynbakers.order_management_system.order.ui.OrderDraft
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import com.zeynbakers.order_management_system.order.ui.UnpaidOrdersScreen
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

internal data class AppFeatureNavigationActions(
    val onOpenMore: () -> Unit,
    val openImportContacts: () -> Unit,
    val navigateToMoneyRecord: (Long?) -> Unit,
    val navigateToMoneyStatements: (Long) -> Unit,
    val navigateToCalendarQuickAdd: (LocalDate) -> Unit,
    val navigateToPaymentHistory: (PaymentHistoryFilter, Long?) -> Unit
)

internal data class AppFeatureSupportActions(
    val refreshAfterPayments: () -> Unit,
    val currentDate: () -> LocalDate,
    val monthLabel: (Int, Int) -> String,
    val onShowMessage: (String) -> Unit,
    val loadContacts: suspend () -> List<ImportContact>
)

internal data class AppCalendarState(
    val calendarDays: List<CalendarDayUi>,
    val currentYear: Int,
    val currentMonth: Int,
    val baseYear: Int,
    val baseMonth: Int,
    val monthSnapshots: Map<MonthKey, MonthSnapshot>,
    val monthTotal: BigDecimal,
    val monthBadgeCount: Int,
    val selectedDate: LocalDate?,
    val summaryDate: LocalDate?,
    val quickAddDate: LocalDate?,
    val ordersForDate: List<OrderEntity>,
    val dayTotal: BigDecimal,
    val orderCustomerNames: Map<Long, String>,
    val orderPaidAmounts: Map<Long, BigDecimal>,
    val summaryOrders: List<OrderEntity>,
    val summaryTotal: BigDecimal,
    val summaryCustomerNames: Map<Long, String>,
    val dayDrafts: MutableMap<LocalDate, OrderDraft>
)

internal data class AppOrdersState(
    val unpaidOrders: List<OrderEntity>,
    val unpaidPaidAmounts: Map<Long, BigDecimal>,
    val unpaidCustomerNames: Map<Long, String>
)

internal data class AppCustomersState(
    val customerSummaries: List<CustomerAccountSummary>,
    val customerDetail: CustomerEntity?,
    val customerLedger: List<AccountEntryEntity>,
    val customerBalance: BigDecimal,
    val customerFinanceSummary: CustomerFinanceSummary?,
    val customerOrders: List<CustomerOrderUi>,
    val customerOrderLabels: Map<Long, String>,
    val customerQuery: String,
    val importContacts: List<ImportContact>,
    val selectedContactPhones: Set<String>,
    val isContactsLoading: Boolean
)

internal data class AppAccountsState(
    val moneyTab: MoneyTab,
    val paymentIntakeText: String?,
    val manualCustomerId: Long?,
    val statementCustomerId: Long?
)

internal data class AppCalendarCallbacks(
    val onSelectedDateChange: (LocalDate?) -> Unit,
    val onSummaryDateChange: (LocalDate?) -> Unit,
    val onQuickAddDateChange: (LocalDate?) -> Unit,
    val onMonthSettled: (Int, Int) -> Unit
)

internal data class AppCustomersCallbacks(
    val onCustomerQueryChange: (String) -> Unit,
    val onImportContactsChange: (List<ImportContact>) -> Unit,
    val onSelectedContactPhonesChange: (Set<String>) -> Unit,
    val onContactsLoadingChange: (Boolean) -> Unit
)

internal data class AppAccountsCallbacks(
    val onMoneyTabChange: (MoneyTab) -> Unit,
    val onPaymentIntakeTextChange: (String?) -> Unit,
    val onManualCustomerIdChange: (Long?) -> Unit,
    val onStatementCustomerIdChange: (Long?) -> Unit
)

@Composable
internal fun AppFeatureNavHost(
    navController: NavHostController,
    modifier: Modifier,
    orderViewModel: OrderViewModel,
    customerViewModel: CustomerAccountsViewModel,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    paymentHistoryViewModel: PaymentIntakeHistoryViewModel,
    ledgerViewModel: LedgerViewModel,
    calendarState: AppCalendarState,
    ordersState: AppOrdersState,
    customersState: AppCustomersState,
    accountsState: AppAccountsState,
    calendarCallbacks: AppCalendarCallbacks,
    customersCallbacks: AppCustomersCallbacks,
    accountsCallbacks: AppAccountsCallbacks,
    navigationActions: AppFeatureNavigationActions,
    supportActions: AppFeatureSupportActions
) {
    val calendarDays = calendarState.calendarDays
    val currentYear = calendarState.currentYear
    val currentMonth = calendarState.currentMonth
    val baseYear = calendarState.baseYear
    val baseMonth = calendarState.baseMonth
    val monthSnapshots = calendarState.monthSnapshots
    val monthTotal = calendarState.monthTotal
    val monthBadgeCount = calendarState.monthBadgeCount
    val selectedDate = calendarState.selectedDate
    val summaryDate = calendarState.summaryDate
    val quickAddDate = calendarState.quickAddDate
    val ordersForDate = calendarState.ordersForDate
    val dayTotal = calendarState.dayTotal
    val orderCustomerNames = calendarState.orderCustomerNames
    val orderPaidAmounts = calendarState.orderPaidAmounts
    val summaryOrders = calendarState.summaryOrders
    val summaryTotal = calendarState.summaryTotal
    val summaryCustomerNames = calendarState.summaryCustomerNames
    val dayDrafts = calendarState.dayDrafts

    val unpaidOrders = ordersState.unpaidOrders
    val unpaidPaidAmounts = ordersState.unpaidPaidAmounts
    val unpaidCustomerNames = ordersState.unpaidCustomerNames

    val customerSummaries = customersState.customerSummaries
    val customerDetail = customersState.customerDetail
    val customerLedger = customersState.customerLedger
    val customerBalance = customersState.customerBalance
    val customerFinanceSummary = customersState.customerFinanceSummary
    val customerOrders = customersState.customerOrders
    val customerOrderLabels = customersState.customerOrderLabels
    val customerQuery = customersState.customerQuery
    val importContacts = customersState.importContacts
    val selectedContactPhones = customersState.selectedContactPhones
    val isContactsLoading = customersState.isContactsLoading

    val moneyTab = accountsState.moneyTab
    val paymentIntakeText = accountsState.paymentIntakeText
    val manualCustomerId = accountsState.manualCustomerId
    val statementCustomerId = accountsState.statementCustomerId

    val onSelectedDateChange = calendarCallbacks.onSelectedDateChange
    val onSummaryDateChange = calendarCallbacks.onSummaryDateChange
    val onQuickAddDateChange = calendarCallbacks.onQuickAddDateChange
    val onMonthSettled = calendarCallbacks.onMonthSettled

    val onCustomerQueryChange = customersCallbacks.onCustomerQueryChange
    val onImportContactsChange = customersCallbacks.onImportContactsChange
    val onSelectedContactPhonesChange = customersCallbacks.onSelectedContactPhonesChange
    val onContactsLoadingChange = customersCallbacks.onContactsLoadingChange

    val onMoneyTabChange = accountsCallbacks.onMoneyTabChange
    val onPaymentIntakeTextChange = accountsCallbacks.onPaymentIntakeTextChange
    val onManualCustomerIdChange = accountsCallbacks.onManualCustomerIdChange
    val onStatementCustomerIdChange = accountsCallbacks.onStatementCustomerIdChange

    fun NavGraphBuilder.calendarFeature() {
        composable(AppRoutes.Calendar) {
            CalendarScreen(
                days = calendarDays,
                currentYear = currentYear,
                currentMonth = currentMonth,
                baseYear = baseYear,
                baseMonth = baseMonth,
                monthSnapshots = monthSnapshots,
                monthTotal = monthTotal,
                monthBadgeCount = monthBadgeCount,
                selectedDate = selectedDate,
                onSelectDate = { onSelectedDateChange(it) },
                onOpenDay = { date ->
                    onSelectedDateChange(date)
                    navController.navigate(AppRoutes.day(date))
                },
                onSaveOrder = { date, notes, total, name, phone, pickupTime ->
                    orderViewModel.saveOrder(
                        date = date,
                        notes = notes,
                        totalAmount = total,
                        customerName = name,
                        customerPhone = phone,
                        pickupTime = pickupTime,
                        existingOrderId = null
                    )
                    WidgetUpdater.enqueue(navController.context)
                    NotificationScheduler.enqueueNow(navController.context)
                },
                searchCustomers = { query -> orderViewModel.searchCustomers(query) },
                onSummaryClick = { navController.navigate(AppRoutes.Summary) },
                onMonthSettled = { year, month -> onMonthSettled(year, month) },
                openQuickAddDate = quickAddDate,
                onQuickAddConsumed = { onQuickAddDateChange(null) }
            )
        }

        composable(
            route = AppRoutes.Day,
            arguments = listOf(navArgument(AppRoutes.ARG_DATE) { type = NavType.StringType })
        ) { entry ->
            val dateArg = entry.arguments?.getString(AppRoutes.ARG_DATE)
            val date = dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: supportActions.currentDate()
            LaunchedEffect(date) {
                onSelectedDateChange(date)
                orderViewModel.loadOrdersForDate(date)
            }
            DayDetailScreen(
                date = date,
                orders = ordersForDate,
                dayTotal = dayTotal,
                customerNames = orderCustomerNames,
                orderPaidAmounts = orderPaidAmounts,
                onBack = { navController.popBackStack() },
                onSaveOrder = { notes, total, name, phone, pickupTime, orderId ->
                    orderViewModel.saveOrder(
                        date = date,
                        notes = notes,
                        totalAmount = total,
                        customerName = name,
                        customerPhone = phone,
                        pickupTime = pickupTime,
                        existingOrderId = orderId
                    )
                    WidgetUpdater.enqueue(navController.context)
                    NotificationScheduler.enqueueNow(navController.context)
                },
                onDeleteOrder = { orderId ->
                    orderViewModel.cancelOrder(orderId, date)
                    WidgetUpdater.enqueue(navController.context)
                    NotificationScheduler.enqueueNow(navController.context)
                },
                loadOrderPaymentAllocations = { orderId ->
                    orderViewModel.loadOrderPaymentAllocations(orderId)
                },
                loadMoveOrderOptions = { customerId, excludeOrderId ->
                    orderViewModel.loadMoveOrderOptions(customerId, excludeOrderId)
                },
                onDeleteOrderWithPayments = { orderId, day, allocationIds, action, target, moveFull ->
                    val result = orderViewModel.deleteOrderWithPayments(
                        orderId = orderId,
                        date = day,
                        allocationIds = allocationIds,
                        action = action,
                        target = target,
                        moveFullReceipts = moveFull
                    )
                    if (result) {
                        WidgetUpdater.enqueue(navController.context)
                        NotificationScheduler.enqueueNow(navController.context)
                    }
                    result
                },
                onOrderPaymentHistory = { orderId ->
                    navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Order(orderId), null)
                },
                onReceivePayment = { order ->
                    navigationActions.navigateToMoneyRecord(order.customerId)
                },
                loadCustomerById = { id -> orderViewModel.getCustomerById(id) },
                searchCustomers = { query -> orderViewModel.searchCustomers(query) },
                draft = dayDrafts[date],
                onDraftChange = { updated ->
                    if (updated == null) {
                        dayDrafts.remove(date)
                    } else {
                        dayDrafts[date] = updated
                    }
                }
            )
        }

        composable(AppRoutes.Summary) {
            SummaryScreen(
                monthLabel = supportActions.monthLabel(currentYear, currentMonth),
                monthTotal = monthTotal,
                initialDate = summaryDate ?: selectedDate ?: supportActions.currentDate(),
                orders = summaryOrders,
                rangeTotal = summaryTotal,
                customerNames = summaryCustomerNames,
                onAnchorDateChange = { updated -> onSummaryDateChange(updated) },
                onLoadRange = { start, end ->
                    orderViewModel.loadSummaryRange(startInclusive = start, endExclusive = end)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    fun NavGraphBuilder.ordersFeature() {
        composable(AppRoutes.Orders) {
            LaunchedEffect(Unit) { orderViewModel.loadUnpaidOrders() }
            UnpaidOrdersScreen(
                orders = unpaidOrders,
                paidAmounts = unpaidPaidAmounts,
                customerNames = unpaidCustomerNames,
                onBack = { navController.popBackStack() },
                onOpenDay = { date ->
                    onSelectedDateChange(date)
                    navController.navigate(AppRoutes.day(date))
                },
                onReceivePayment = { order ->
                    navigationActions.navigateToMoneyRecord(order.customerId)
                },
                onSettingsClick = navigationActions.onOpenMore,
                title = "Orders",
                showBack = false
            )
        }
    }

    fun NavGraphBuilder.customersFeature() {
        composable(AppRoutes.Customers) {
            LaunchedEffect(customerQuery) {
                customerViewModel.searchCustomers(customerQuery)
            }
            CustomerListScreen(
                query = customerQuery,
                summaries = customerSummaries,
                onQueryChange = { onCustomerQueryChange(it) },
                onCustomerClick = { id ->
                    navController.navigate(AppRoutes.customerDetail(id))
                },
                onBack = { navController.popBackStack() },
                onAddCustomer = navigationActions.openImportContacts,
                onPaymentHistory = { customerId ->
                    navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Customer(customerId), null)
                },
                onRecordPayment = { customerId ->
                    navigationActions.navigateToMoneyRecord(customerId)
                },
                onAddOrder = {
                    val targetDate = supportActions.currentDate()
                    navigationActions.navigateToCalendarQuickAdd(targetDate)
                },
                onArchiveCustomer = { customerId ->
                    customerViewModel.archiveCustomer(customerId)
                },
                onRestoreCustomer = { customerId ->
                    customerViewModel.unarchiveCustomer(customerId)
                },
                showBack = false
            )
        }

        composable(
            route = AppRoutes.CustomerDetail,
            arguments = listOf(navArgument(AppRoutes.ARG_CUSTOMER_ID) { type = NavType.LongType })
        ) { entry ->
            val customerId = entry.arguments?.getLong(AppRoutes.ARG_CUSTOMER_ID) ?: return@composable
            LaunchedEffect(customerId) {
                customerViewModel.loadCustomer(customerId)
            }
            CustomerDetailScreen(
                customer = customerDetail,
                ledger = customerLedger,
                balance = customerBalance,
                financeSummary = customerFinanceSummary,
                orders = customerOrders,
                orderLabels = customerOrderLabels,
                onBack = { navController.popBackStack() },
                onPaymentHistory = { id ->
                    navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Customer(id), null)
                },
                onOpenStatement = { id ->
                    navigationActions.navigateToMoneyStatements(id)
                },
                onReceivePayment = {
                    navigationActions.navigateToMoneyRecord(customerId)
                },
                onOrderPaymentHistory = { orderId ->
                    navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.Order(orderId), null)
                },
                onUpdateOrderStatusOverride = { orderId, override ->
                    customerViewModel.setOrderStatusOverride(orderId, override)
                    supportActions.refreshAfterPayments()
                },
                onWriteOffOrder = { orderId ->
                    customerViewModel.writeOffOrder(orderId)
                    supportActions.refreshAfterPayments()
                }
            )
        }

        composable(AppRoutes.ImportContacts) {
            LaunchedEffect(Unit) {
                onContactsLoadingChange(true)
                onImportContactsChange(supportActions.loadContacts())
                onSelectedContactPhonesChange(emptySet())
                onContactsLoadingChange(false)
            }
            ImportContactsScreen(
                contacts = importContacts,
                selectedPhones = selectedContactPhones,
                isLoading = isContactsLoading,
                onBack = { navController.popBackStack() },
                onToggleSelect = { phone ->
                    onSelectedContactPhonesChange(
                        if (selectedContactPhones.contains(phone)) {
                            selectedContactPhones - phone
                        } else {
                            selectedContactPhones + phone
                        }
                    )
                },
                onToggleSelectAll = { visiblePhones ->
                    val visibleSet = visiblePhones.toSet()
                    val allVisibleSelected =
                        visibleSet.isNotEmpty() &&
                            visibleSet.all { selectedContactPhones.contains(it) }
                    onSelectedContactPhonesChange(
                        if (allVisibleSelected) {
                            selectedContactPhones - visibleSet
                        } else {
                            selectedContactPhones + visibleSet
                        }
                    )
                },
                onImport = {
                    if (selectedContactPhones.isEmpty()) {
                        supportActions.onShowMessage("Select at least one contact to import")
                    } else {
                        val contactsByPhone = importContacts.associateBy { it.phone }
                        selectedContactPhones.forEach { phone ->
                            val contact = contactsByPhone[phone] ?: return@forEach
                            customerViewModel.importCustomer(contact.name, contact.phone)
                        }
                        navController.popBackStack()
                    }
                }
            )
        }
    }

    fun NavGraphBuilder.accountsFeature() {
        composable(AppRoutes.Money) {
            MoneyScreen(
                selectedTab = moneyTab,
                onTabChange = { onMoneyTabChange(it) },
                paymentIntakeViewModel = paymentIntakeViewModel,
                customerViewModel = customerViewModel,
                ledgerViewModel = ledgerViewModel,
                initialText = paymentIntakeText,
                manualCustomerId = manualCustomerId,
                statementCustomerId = statementCustomerId,
                onManualContextConsumed = {
                    onManualCustomerIdChange(null)
                },
                onStatementContextConsumed = {
                    onStatementCustomerIdChange(null)
                },
                onManualSaved = { supportActions.refreshAfterPayments() },
                onApplied = {
                    onPaymentIntakeTextChange(null)
                    supportActions.refreshAfterPayments()
                },
                onAppliedInPlace = { supportActions.refreshAfterPayments() },
                onOpenReceiptHistory = { receiptId ->
                    navigationActions.navigateToPaymentHistory(PaymentHistoryFilter.All, receiptId)
                }
            )
        }
        composable(
            route = "${AppRoutes.PaymentHistoryAll}?${AppRoutes.ARG_FOCUS_RECEIPT_ID}={${AppRoutes.ARG_FOCUS_RECEIPT_ID}}",
            arguments = listOf(
                navArgument(AppRoutes.ARG_FOCUS_RECEIPT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val focusId = entry.arguments
                ?.getLong(AppRoutes.ARG_FOCUS_RECEIPT_ID)
                ?.takeIf { it > 0 }
            PaymentIntakeHistoryScreen(
                viewModel = paymentHistoryViewModel,
                filter = PaymentHistoryFilter.All,
                focusReceiptId = focusId,
                onBack = { navController.popBackStack() },
                onRemoved = { supportActions.refreshAfterPayments() }
            )
        }

        composable(
            route = AppRoutes.PaymentHistoryCustomer,
            arguments = listOf(navArgument(AppRoutes.ARG_CUSTOMER_ID) { type = NavType.LongType })
        ) { entry ->
            val customerId = entry.arguments?.getLong(AppRoutes.ARG_CUSTOMER_ID) ?: return@composable
            PaymentIntakeHistoryScreen(
                viewModel = paymentHistoryViewModel,
                filter = PaymentHistoryFilter.Customer(customerId),
                focusReceiptId = null,
                onBack = { navController.popBackStack() },
                onRemoved = {
                    supportActions.refreshAfterPayments()
                    customerViewModel.loadCustomer(customerId)
                }
            )
        }

        composable(
            route = AppRoutes.PaymentHistoryOrder,
            arguments = listOf(navArgument(AppRoutes.ARG_ORDER_ID) { type = NavType.LongType })
        ) { entry ->
            val orderId = entry.arguments?.getLong(AppRoutes.ARG_ORDER_ID) ?: return@composable
            PaymentIntakeHistoryScreen(
                viewModel = paymentHistoryViewModel,
                filter = PaymentHistoryFilter.Order(orderId),
                focusReceiptId = null,
                onBack = { navController.popBackStack() },
                onRemoved = { supportActions.refreshAfterPayments() }
            )
        }
    }

    fun NavGraphBuilder.settingsFeature() {
        composable(AppRoutes.Backup) {
            BackupSettingsScreen(
                onBack = { navController.popBackStack() },
                onImportContacts = navigationActions.openImportContacts
            )
        }

        composable(AppRoutes.Notifications) {
            NotificationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Calendar,
        modifier = modifier
    ) {
        calendarFeature()
        ordersFeature()
        customersFeature()
        accountsFeature()
        settingsFeature()
    }
}
