package com.zeynbakers.order_management_system.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tutorialTargetId: String? = null
)

data class MoreAction(
    val label: String,
    val groupLabel: String,
    val icon: ImageVector,
    val supportingText: String? = null,
    val tutorialTargetId: String? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    windowSizeClass: WindowSizeClass,
    destinations: List<TopLevelDestination>,
    selectedRoute: String,
    onDestinationSelected: (String) -> Unit,
    showNavigation: Boolean,
    showMoreSheet: Boolean,
    onOpenMore: () -> Unit,
    onDismissMore: () -> Unit,
    moreActions: List<MoreAction>,
    content: @Composable (PaddingValues) -> Unit
) {
    val useRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val navItemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            indicatorColor = MaterialTheme.colorScheme.primary
        )
    val railItemColors =
        NavigationRailItemDefaults.colors(
            selectedIconColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )

    if (!showNavigation) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues(0.dp))
        }
    } else if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.weight(1f, fill = false))
                destinations.forEach { destination ->
                    NavigationRailItem(
                        modifier =
                            if (destination.tutorialTargetId != null) {
                                Modifier.tutorialCoachTarget(destination.tutorialTargetId)
                            } else {
                                Modifier
                            },
                        selected = selectedRoute == destination.route,
                        onClick = { onDestinationSelected(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                        colors = railItemColors
                    )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        tonalElevation = 6.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(80.dp)
                        ) {
                            destinations.forEach { destination ->
                                val isSelected = selectedRoute == destination.route
                                NavigationBarItem(
                                    modifier =
                                        if (destination.tutorialTargetId != null) {
                                            Modifier.tutorialCoachTarget(destination.tutorialTargetId)
                                        } else {
                                            Modifier
                                        },
                                    selected = isSelected,
                                    onClick = { onDestinationSelected(destination.route) },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label,
                                            modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            letterSpacing = if (isSelected) 0.5.sp else 0.sp
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    colors = navItemColors
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            content(padding)
        }
    }

    if (showNavigation && showMoreSheet) {
        ModalBottomSheet(onDismissRequest = onDismissMore) {
            val groupedActions = moreActions.groupBy { it.groupLabel }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_more),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.more_quick_tools_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                groupedActions.entries.forEachIndexed { groupIndex, (groupLabel, actions) ->
                    if (groupIndex > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )
                    }
                    Text(
                        text = groupLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    actions.forEach { action ->
                        ElevatedButton(
                            onClick = action.onClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (action.tutorialTargetId != null) {
                                            Modifier.tutorialCoachTarget(action.tutorialTargetId)
                                        } else {
                                            Modifier
                                        }
                                    )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(action.icon, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(action.label)
                                    action.supportingText?.let { supportingText ->
                                        Text(
                                            text = supportingText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
