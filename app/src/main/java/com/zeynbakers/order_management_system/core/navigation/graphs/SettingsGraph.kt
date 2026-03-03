package com.zeynbakers.order_management_system.core.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        LaunchedEffect(Unit) {
            navigationActions.startPracticalTutorial(0)
            navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
        }
        TutorialLaunchPlaceholder()
    }

    composable(AppRoutes.TutorialAfterCalendar) {
        LaunchedEffect(Unit) {
            navigationActions.startPracticalTutorial(1)
            navigateTopLevel(navController, AppRoutes.Calendar, resetToRoot = true)
        }
        TutorialLaunchPlaceholder()
    }
}

@Composable
private fun TutorialLaunchPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
