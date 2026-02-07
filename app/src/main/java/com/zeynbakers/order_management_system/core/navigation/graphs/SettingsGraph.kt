package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.zeynbakers.order_management_system.AppFeatureNavigationActions
import com.zeynbakers.order_management_system.core.backup.BackupSettingsScreen
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.notifications.NotificationSettingsScreen

internal fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    navigationActions: AppFeatureNavigationActions
) {
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
