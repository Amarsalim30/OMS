package com.zeynbakers.order_management_system.core.helper.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.helper.HelperOverlayController
import com.zeynbakers.order_management_system.core.helper.HelperPermissions
import com.zeynbakers.order_management_system.core.helper.HelperPreferences
import com.zeynbakers.order_management_system.core.helper.HelperSettingsState
import com.zeynbakers.order_management_system.core.tutorial.TutorialCoachTargets
import com.zeynbakers.order_management_system.core.tutorial.tutorialCoachTarget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelperSettingsScreen(
    onBack: () -> Unit,
    onOpenNotesHistory: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { HelperPreferences(context) }
    val scope = rememberCoroutineScope()
    val helperState by prefs.state.collectAsState(initial = HelperSettingsState())
    var hasOverlayPermission by remember { mutableStateOf(HelperPermissions.hasOverlayPermission(context)) }
    var hasMicPermission by remember { mutableStateOf(HelperPermissions.hasMicrophonePermission(context)) }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
        }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = HelperPermissions.hasOverlayPermission(context)
                hasMicPermission = HelperPermissions.hasMicrophonePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.helper_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.helper_settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ToggleRow(
                title = stringResource(R.string.helper_settings_enable),
                checked = helperState.enabled,
                modifier = Modifier.tutorialCoachTarget(TutorialCoachTargets.HelperEnableSwitch),
                onCheckedChange = { enabled ->
                    scope.launch {
                        prefs.setEnabled(enabled)
                        if (enabled) {
                            HelperOverlayController.start(context)
                        } else {
                            HelperOverlayController.stop(context)
                        }
                    }
                }
            )

            ToggleRow(
                title = stringResource(R.string.helper_settings_fallback_only),
                checked = helperState.fallbackOnly,
                onCheckedChange = { value ->
                    scope.launch {
                        prefs.setFallbackOnly(value)
                        HelperOverlayController.refresh(context)
                    }
                }
            )

            ToggleRow(
                title = stringResource(R.string.helper_settings_smart_hide),
                checked = helperState.smartHideEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        prefs.setSmartHideEnabled(enabled)
                        HelperOverlayController.refresh(context)
                    }
                }
            )

            Text(
                text = stringResource(R.string.helper_settings_peek_delay_value, helperState.idlePeekSeconds),
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 6).forEach { seconds ->
                    ThemeChoiceButton(
                        label = stringResource(R.string.helper_settings_peek_delay_choice, seconds),
                        selected = helperState.idlePeekSeconds == seconds,
                        onClick = {
                            scope.launch {
                                prefs.setIdlePeekSeconds(seconds)
                                HelperOverlayController.refresh(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        prefs.clearBubblePosition()
                        HelperOverlayController.refresh(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.helper_settings_reset_position))
            }

            Text(
                text =
                    if (hasMicPermission) {
                        stringResource(R.string.helper_settings_mic_granted)
                    } else {
                        stringResource(R.string.helper_settings_mic_missing)
                    },
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    if (hasOverlayPermission) {
                        stringResource(R.string.helper_settings_overlay_granted)
                    } else {
                        stringResource(R.string.helper_settings_overlay_missing)
                    },
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.helper_settings_grant_mic))
                }
                OutlinedButton(
                    onClick = { context.startActivity(HelperPermissions.overlaySettingsIntent(context)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.helper_settings_open_overlay))
                }
            }

            Button(
                onClick = onOpenNotesHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.more_notes_history))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}
