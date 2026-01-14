package com.zeynbakers.order_management_system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import com.zeynbakers.order_management_system.customer.ui.CustomerAccountsViewModel
import com.zeynbakers.order_management_system.customer.ui.CustomerDetailScreen
import com.zeynbakers.order_management_system.customer.ui.CustomerListScreen
import com.zeynbakers.order_management_system.order.ui.CalendarScreen
import com.zeynbakers.order_management_system.order.ui.DayDetailScreen
import com.zeynbakers.order_management_system.order.ui.OrderViewModel
import com.zeynbakers.order_management_system.order.ui.SummaryScreen
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Order_management_systemTheme {
                val database = remember { DatabaseProvider.getDatabase(applicationContext) }
                val viewModel = remember { OrderViewModel(database = database) }
                val customerViewModel = remember { CustomerAccountsViewModel(database = database) }

                var currentMonth by remember { mutableStateOf(0) }
                var currentYear by remember { mutableStateOf(0) }
                var baseMonth by remember { mutableStateOf(0) }
                var baseYear by remember { mutableStateOf(0) }
                var screen by remember { mutableStateOf<Screen>(Screen.Calendar) }
                var customerQuery by remember { mutableStateOf("") }

                val calendarDays by viewModel.calendarDays.collectAsState()
                val monthTotal by viewModel.monthTotal.collectAsState()
                val monthBadgeCount by viewModel.monthBadgeCount.collectAsState()
                val ordersForDate by viewModel.ordersForDate.collectAsState()
                val dayTotal by viewModel.dayTotal.collectAsState()
                val orderCustomerNames by viewModel.orderCustomerNames.collectAsState()
                val orderPaidAmounts by viewModel.orderPaidAmounts.collectAsState()
                val monthSnapshots by viewModel.monthSnapshots.collectAsState()

                val customerSummaries by customerViewModel.summaries.collectAsState()
                val customerDetail by customerViewModel.customer.collectAsState()
                val customerLedger by customerViewModel.ledger.collectAsState()
                val customerBalance by customerViewModel.balance.collectAsState()
                val customerOrders by customerViewModel.orders.collectAsState()

                var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

                LaunchedEffect(Unit) {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    currentMonth = now.monthNumber
                    currentYear = now.year
                    selectedDate = now.date
                    if (baseMonth == 0 && baseYear == 0) {
                        baseMonth = now.monthNumber
                        baseYear = now.year
                    }
                }

                LaunchedEffect(currentMonth, currentYear) {
                    if (currentMonth > 0 && currentYear > 0) {
                        viewModel.loadMonth(month = currentMonth, year = currentYear)
                        viewModel.prefetchAdjacentMonths(year = currentYear, month = currentMonth)
                    }
                }

                LaunchedEffect(screen, customerQuery) {
                    val currentScreen = screen
                    if (currentScreen is Screen.CustomerList) {
                        customerViewModel.searchCustomers(customerQuery)
                    }
                }

                LaunchedEffect(screen) {
                    val currentScreen = screen
                    if (currentScreen is Screen.CustomerDetail) {
                        customerViewModel.loadCustomer(currentScreen.customerId)
                    }
                }

                when (val active = screen) {
                    Screen.Calendar -> {
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
                            onSelectDate = { selectedDate = it },
                            onSaveOrder = { date, notes, total, name, phone ->
                                viewModel.saveOrder(
                                    date = date,
                                    notes = notes,
                                    totalAmount = total,
                                    customerName = name,
                                    customerPhone = phone,
                                    existingOrderId = null
                                )
                            },
                            searchCustomers = { query -> viewModel.searchCustomers(query) },
                            onCustomersClick = { screen = Screen.CustomerList },
                            onSummaryClick = { screen = Screen.Summary },
                            onMonthSettled = { year, month ->
                                currentYear = year
                                currentMonth = month
                            },
                            onOpenDay = { date ->
                                viewModel.loadOrdersForDate(date)
                                screen = Screen.Day(date)
                            }
                        )
                    }
                    is Screen.Day -> {
                        BackHandler { screen = Screen.Calendar }
                        DayDetailScreen(
                            date = active.date,
                            orders = ordersForDate,
                            dayTotal = dayTotal,
                            customerNames = orderCustomerNames,
                            orderPaidAmounts = orderPaidAmounts,
                            onBack = { screen = Screen.Calendar },
                            onSaveOrder = { notes, total, name, phone, orderId ->
                                viewModel.saveOrder(
                                    date = active.date,
                                    notes = notes,
                                    totalAmount = total,
                                    customerName = name,
                                    customerPhone = phone,
                                    existingOrderId = orderId
                                )
                            },
                            loadCustomerById = { id -> viewModel.getCustomerById(id) },
                            searchCustomers = { query -> viewModel.searchCustomers(query) }
                        )
                    }
                    Screen.Summary -> {
                        BackHandler { screen = Screen.Calendar }
                        SummaryScreen(
                            monthLabel = monthLabel(currentYear, currentMonth),
                            monthTotal = monthTotal,
                            onBack = { screen = Screen.Calendar }
                        )
                    }
                    Screen.CustomerList -> {
                        BackHandler { screen = Screen.Calendar }
                        CustomerListScreen(
                            query = customerQuery,
                            summaries = customerSummaries,
                            onQueryChange = { customerQuery = it },
                            onCustomerClick = { id ->
                                screen = Screen.CustomerDetail(id)
                            },
                            onBack = { screen = Screen.Calendar }
                        )
                    }
                    is Screen.CustomerDetail -> {
                        BackHandler { screen = Screen.CustomerList }
                        CustomerDetailScreen(
                            customer = customerDetail,
                            ledger = customerLedger,
                            balance = customerBalance,
                            orders = customerOrders,
                            onBack = { screen = Screen.CustomerList },
                            onRecordPayment = { amount, method, note, orderId ->
                                customerViewModel.recordPayment(
                                    customerId = active.customerId,
                                    amount = amount,
                                    method = method,
                                    note = note,
                                    orderId = orderId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private sealed class Screen {
    data object Calendar : Screen()
    data class Day(val date: LocalDate) : Screen()
    data object Summary : Screen()
    data object CustomerList : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
}

private fun monthLabel(year: Int, month: Int): String {
    if (month == 0 || year == 0) return "Loading..."
    val monthName = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }
    return "$monthName $year"
}
