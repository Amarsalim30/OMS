package com.zeynbakers.order_management_system.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

data class MoreAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    windowSizeClass: WindowSizeClass,
    destinations: List<TopLevelDestination>,
    selectedRoute: String,
    onDestinationSelected: (String) -> Unit,
    showMoreSheet: Boolean,
    onOpenMore: () -> Unit,
    onDismissMore: () -> Unit,
    moreActions: List<MoreAction>,
    content: @Composable (PaddingValues) -> Unit
) {
    val useRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = selectedRoute == destination.route,
                        onClick = { onDestinationSelected(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = false
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenMore) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = selectedRoute == destination.route,
                                onClick = { onDestinationSelected(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenMore,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        ) { padding ->
            content(padding)
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = onDismissMore) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                moreActions.forEach { action ->
                    ElevatedButton(
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(action.icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(action.label)
                    }
                }
            }
        }
    }
}
