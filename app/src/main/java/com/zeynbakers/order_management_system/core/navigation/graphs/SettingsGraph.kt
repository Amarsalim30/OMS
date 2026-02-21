package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.zeynbakers.order_management_system.navigateTopLevel
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen
import com.zeynbakers.order_management_system.core.tutorial.BeginnerTutorialScreen

internal fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    navigationActions: AppFeatureNavigationActions
) {
    composable(AppRoutes.Backup) {
        BackupSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppRoutes.Notifications) {
        NotificationSettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppRoutes.Tutorial) {
        BeginnerTutorialScreen(
            onBack = { navController.popBackStack() },
            onOpenCalendar = {
                navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
            },
            onOpenOrders = {
                navigateTopLevel(navController, AppRoutes.Orders, resetToRoot = true)
            },
            onOpenCustomers = {
                navigateTopLevel(navController, AppRoutes.Customers, resetToRoot = true)
            },
            onOpenMoney = {
                navigateTopLevel(navController, AppRoutes.Money, resetToRoot = true)
            },
            onOpenBackup = {
                navController.navigate(AppRoutes.Backup)
            },
            onOpenNotifications = {
                navController.navigate(AppRoutes.Notifications)
            }
        )
    }
}
