package com.zeynbakers.order_management_system

import android.Manifest
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.zeynbakers.order_management_system.accounting.ui.PaymentHistoryFilter
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

internal fun topLevelRouteFor(route: String?): String? {
    return when {
        route == null -> null
        route == AppRoutes.Calendar -> AppRoutes.Calendar
        route == AppRoutes.CalendarTutorial -> AppRoutes.Calendar
        route.startsWith("day/") -> AppRoutes.Calendar
        route == AppRoutes.Summary -> AppRoutes.Calendar
        route == AppRoutes.Orders -> AppRoutes.Orders
        route == AppRoutes.Customers -> AppRoutes.Customers
        route.startsWith("customer/") -> AppRoutes.Customers
        route == AppRoutes.ImportContacts -> AppRoutes.Customers
        route == AppRoutes.Money -> AppRoutes.Money
        route.startsWith("payment_history/") -> AppRoutes.Money
        else -> null
    }
}

internal fun navigateTopLevel(
    navController: NavHostController,
    route: String,
    resetToRoot: Boolean = false
) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    if (resetToRoot) {
        if (currentRoute == route) {
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
        return
    }
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun navigateCalendarExternal(
    navController: NavHostController,
    route: String
) {
    navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
    navController.navigate(route) {
        popUpTo(AppRoutes.Calendar) { inclusive = false }
        launchSingleTop = true
        restoreState = false
    }
}

internal fun navigateToPaymentHistory(
    navController: NavHostController,
    filter: PaymentHistoryFilter,
    focusReceiptId: Long?
) {
    val route = when (filter) {
        PaymentHistoryFilter.All -> AppRoutes.paymentHistoryAll(focusReceiptId)
        is PaymentHistoryFilter.Customer -> AppRoutes.paymentHistoryCustomer(filter.customerId)
        is PaymentHistoryFilter.Order -> AppRoutes.paymentHistoryOrder(filter.orderId)
    }
    navController.navigate(route)
}

internal fun navToImportContacts(
    context: Context,
    onNavigate: (String) -> Unit,
    onOpen: () -> Unit
) {
    val hasPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (hasPermission) {
        onOpen()
    } else {
        onNavigate(AppRoutes.Customers)
    }
}

internal fun monthLabel(
    year: Int,
    month: Int,
    loadingLabel: String,
    locale: Locale = Locale.getDefault()
): String {
    if (month == 0 || year == 0) return loadingLabel
    val monthName =
        runCatching { Month.of(month).getDisplayName(TextStyle.FULL, locale) }
            .getOrElse { month.toString() }
    return "$monthName $year"
}

internal fun shouldAppendSharedPaymentText(
    currentRoute: String?,
    isMoneyCollectTab: Boolean
): Boolean {
    return currentRoute == AppRoutes.Money && isMoneyCollectTab
}

internal fun sharedPaymentTextPreview(
    text: String,
    maxChars: Int = 160
): String {
    val normalized = text.trim().replace("\r\n", "\n")
    if (normalized.length <= maxChars) {
        return normalized
    }
    return normalized.take(maxChars).trimEnd() + "..."
}
