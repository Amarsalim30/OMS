package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zeynbakers.order_management_system.navigateTopLevel
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.core.helper.ui.HelperSettingsScreen
import com.zeynbakers.order_management_system.core.helper.ui.NotesHistoryScreen
import com.zeynbakers.order_management_system.core.helper.ui.NotesHistoryViewModel
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen
import com.zeynbakers.order_management_system.core.tutorial.BeginnerTutorialScreen
import com.zeynbakers.order_management_system.core.ui.AppViewModelFactory

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

    composable(AppRoutes.NotesHistory) {
        val context = LocalContext.current
        val database = remember { DatabaseProvider.getDatabase(context.applicationContext) }
        val factory = remember(database) { AppViewModelFactory(database, context.applicationContext) }
        val viewModel: NotesHistoryViewModel = viewModel(factory = factory)
        NotesHistoryScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppRoutes.HelperSettings) {
        HelperSettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenNotesHistory = {
                navController.navigate(AppRoutes.NotesHistory) {
                    launchSingleTop = true
                }
            }
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
            onOpenNotesHistory = {
                navController.navigate(AppRoutes.NotesHistory) {
                    launchSingleTop = true
                }
            },
            onOpenHelperSettings = {
                navController.navigate(AppRoutes.HelperSettings) {
                    launchSingleTop = true
                }
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
