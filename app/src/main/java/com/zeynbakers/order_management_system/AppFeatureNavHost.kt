package com.zeynbakers.order_management_system

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.CustomerAccountSummary
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeHistoryViewModel
import com.zeynbakers.order_management_system.accounting.ui.PaymentIntakeViewModel
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.navigation.graphs.accountsGraph
import com.zeynbakers.order_management_system.core.navigation.graphs.calendarGraph
import com.zeynbakers.order_management_system.core.navigation.graphs.customersGraph
import com.zeynbakers.order_management_system.core.navigation.graphs.onboardingGraph
import com.zeynbakers.order_management_system.core.navigation.graphs.ordersGraph
import com.zeynbakers.order_management_system.core.navigation.graphs.settingsGraph
import com.zeynbakers.order_management_system.core.ui.MoneyTab
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerFinanceSummary
import com.zeynbakers.order_management_system.customer.ui.CustomerOrderUi
import com.zeynbakers.order_management_system.customer.ui.CustomerStatementRowUi
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.ui.CalendarDayUi
import com.zeynbakers.order_management_system.order.ui.MonthKey
import com.zeynbakers.order_management_system.order.ui.MonthSnapshot
import com.zeynbakers.order_management_system.order.ui.OrderDraft
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

internal data class AppFeatureNavigationActions(
    val onOpenMore: () -> Unit,
    val openImportContacts: () -> Unit,
    val navigateToMoneyRecord: (Long?) -> Unit,
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
    val customerStatementRows: List<CustomerStatementRowUi>,
    val isCustomerStatementLoading: Boolean,
    val customerQuery: String,
    val importContacts: List<ImportContact>,
    val selectedContactPhones: Set<String>,
    val isContactsLoading: Boolean
)

internal data class AppAccountsState(
    val moneyTab: MoneyTab,
    val paymentIntakeText: String?,
    val manualCustomerId: Long?
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
    val onManualCustomerIdChange: (Long?) -> Unit
)

@Composable
internal fun AppFeatureNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier,
    orderViewModel: OrderViewModel,
    customerViewModel: CustomerAccountsViewModel,
    paymentIntakeViewModel: PaymentIntakeViewModel,
    paymentHistoryViewModel: PaymentIntakeHistoryViewModel,
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        onboardingGraph(navController = navController)

        calendarGraph(
            navController = navController,
            orderViewModel = orderViewModel,
            calendarState = calendarState,
            calendarCallbacks = calendarCallbacks,
            navigationActions = navigationActions,
            supportActions = supportActions
        )

        ordersGraph(
            navController = navController,
            orderViewModel = orderViewModel,
            ordersState = ordersState,
            calendarCallbacks = calendarCallbacks,
            navigationActions = navigationActions
        )

        customersGraph(
            navController = navController,
            customerViewModel = customerViewModel,
            customersState = customersState,
            customersCallbacks = customersCallbacks,
            navigationActions = navigationActions,
            supportActions = supportActions
        )

        accountsGraph(
            navController = navController,
            customerViewModel = customerViewModel,
            paymentIntakeViewModel = paymentIntakeViewModel,
            paymentHistoryViewModel = paymentHistoryViewModel,
            accountsState = accountsState,
            accountsCallbacks = accountsCallbacks,
            navigationActions = navigationActions,
            supportActions = supportActions
        )

        settingsGraph(
            navController = navController,
            navigationActions = navigationActions
        )
    }
}
