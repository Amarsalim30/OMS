package com.zeynbakers.order_management_system.core.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.components.AppCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

@Composable
fun SplashScreen(
    onShouldEnterHome: suspend () -> Boolean,
    onOpenHome: () -> Unit,
    onOpenIntro: () -> Unit
) {
    var routed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (routed) return@LaunchedEffect
        delay(850)
        val completed = onShouldEnterHome()
        routed = true
        if (completed) onOpenHome() else onOpenIntro()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp).size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroPagerScreen(
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val valueItems = listOf(
        IntroValueItem(
            icon = Icons.Filled.CalendarToday,
            title = stringResource(R.string.intro_slide1_title),
            body = stringResource(R.string.intro_slide1_body)
        ),
        IntroValueItem(
            icon = Icons.Filled.Payments,
            title = stringResource(R.string.intro_slide2_title),
            body = stringResource(R.string.intro_slide2_body)
        ),
        IntroValueItem(
            icon = Icons.Filled.CloudDone,
            title = stringResource(R.string.intro_slide3_title),
            body = stringResource(R.string.intro_slide3_body)
        )
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.intro_title)) },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.intro_skip))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Text(
                    text = stringResource(R.string.intro_one_page_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.intro_one_page_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                valueItems.forEach { item ->
                    IntroValueRow(item = item)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.intro_skip))
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.intro_get_started))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupChecklistScreen(
    onboardingState: OnboardingState,
    backupConfigured: Boolean,
    backupTargetLabel: String?,
    contactsConfigured: Boolean,
    contactsPermissionGranted: Boolean,
    contactsPermissionPermanentlyDenied: Boolean,
    notificationsConfigured: Boolean,
    notificationsPermissionPermanentlyDenied: Boolean,
    helperConfigured: Boolean,
    helperMicGranted: Boolean,
    helperOverlayGranted: Boolean,
    onSaveBusinessProfile: (name: String, currency: String, timezone: String) -> Unit,
    onChooseBackupFile: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onOpenContactsImport: () -> Unit,
    onEnableNotifications: () -> Unit,
    onDisableNotifications: () -> Unit,
    onOpenHelperSetup: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onStartUsingApp: () -> Unit
) {
    var businessName by rememberSaveable(onboardingState.businessName) {
        mutableStateOf(onboardingState.businessName)
    }
    var currency by rememberSaveable(onboardingState.currency) {
        mutableStateOf(onboardingState.currency.ifBlank { "KES" })
    }
    var timezone by rememberSaveable(onboardingState.timezone) {
        mutableStateOf(onboardingState.timezone.ifBlank { TimeZone.currentSystemDefault().id })
    }
    val canSaveBusiness = businessName.trim().isNotEmpty() && currency.isNotBlank() && timezone.isNotBlank()

    val steps = remember(
        onboardingState.businessProfileCompleted,
        backupConfigured,
        contactsConfigured,
        notificationsConfigured,
        helperConfigured
    ) {
        listOf(
            SetupStep(
                type = SetupStepType.Business,
                title = R.string.setup_item_business_title,
                body = R.string.setup_item_business_body,
                required = true,
                done = onboardingState.businessProfileCompleted
            ),
            SetupStep(
                type = SetupStepType.Backup,
                title = R.string.setup_item_backup_title,
                body = R.string.setup_item_backup_body,
                required = false,
                done = backupConfigured
            ),
            SetupStep(
                type = SetupStepType.Contacts,
                title = R.string.setup_item_contacts_title,
                body = R.string.setup_item_contacts_body,
                required = false,
                done = contactsConfigured
            ),
            SetupStep(
                type = SetupStepType.Notifications,
                title = R.string.setup_item_notifications_title,
                body = R.string.setup_item_notifications_body,
                required = false,
                done = notificationsConfigured
            ),
            SetupStep(
                type = SetupStepType.Helper,
                title = R.string.setup_item_helper_title,
                body = R.string.setup_item_helper_body,
                required = false,
                done = helperConfigured
            )
        )
    }

    val defaultPage =
        remember(steps) {
            when {
                steps.all { it.done } -> steps.lastIndex
                else -> (steps.indexOfLast { it.done } + 1).coerceIn(0, steps.lastIndex)
            }
        }
    var persistedPage by rememberSaveable { mutableStateOf<Int?>(null) }
    val initialPage = (persistedPage ?: defaultPage).coerceIn(0, steps.lastIndex)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { steps.size }
    )
    val scope = rememberCoroutineScope()
    val currentStep = steps[pagerState.currentPage]
    val canAdvance =
        when (currentStep.type) {
            SetupStepType.Business -> canSaveBusiness
            else -> !currentStep.required || currentStep.done
        }
    val completedCount = steps.count { it.done }
    val progress = (completedCount.toFloat() / steps.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    var doneSnapshot by remember { mutableStateOf(steps.map { it.done }) }
    var isTransitioning by remember { mutableStateOf(false) }
    val moveToPage: (Int) -> Unit = { targetPage ->
        val boundedTarget = targetPage.coerceIn(0, steps.lastIndex)
        if (boundedTarget == pagerState.currentPage || isTransitioning || pagerState.isScrollInProgress) {
            Unit
        } else {
            scope.launch {
                isTransitioning = true
                runCatching { pagerState.animateScrollToPage(boundedTarget) }
                isTransitioning = false
            }
        }
    }

    LaunchedEffect(steps.map { it.done }, pagerState.currentPage) {
        val currentDone = steps[pagerState.currentPage].done
        val previousDone = doneSnapshot.getOrNull(pagerState.currentPage) ?: false
        if (!previousDone && currentDone && pagerState.currentPage < steps.lastIndex) {
            moveToPage(pagerState.currentPage + 1)
        }
        doneSnapshot = steps.map { it.done }
    }
    LaunchedEffect(pagerState.currentPage) {
        persistedPage = pagerState.currentPage
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.setup_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppCard {
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                Text(
                    text = stringResource(R.string.setup_step_progress, pagerState.currentPage + 1, steps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = stringResource(R.string.setup_completion_progress, completedCount, steps.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppCard {
                        Text(
                            text = stringResource(step.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(step.body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text =
                                if (step.done) {
                                    stringResource(R.string.setup_status_ready)
                                } else if (step.required) {
                                    stringResource(R.string.setup_step_required_hint)
                                } else {
                                    stringResource(R.string.setup_status_optional)
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        when (step.type) {
                            SetupStepType.Business -> {
                                OutlinedTextField(
                                    value = businessName,
                                    onValueChange = { businessName = it },
                                    label = { Text(stringResource(R.string.business_name_label)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.business_currency_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("KES", "USD", "EUR").forEach { option ->
                                        FilterChip(
                                            selected = currency == option,
                                            onClick = { currency = option },
                                            label = { Text(option) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = timezone,
                                    onValueChange = { timezone = it },
                                    label = { Text(stringResource(R.string.business_timezone_label)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            }

                            SetupStepType.Backup -> {
                                Text(
                                    text =
                                        if (backupTargetLabel.isNullOrBlank()) {
                                            stringResource(R.string.setup_backup_not_selected)
                                        } else {
                                            stringResource(R.string.setup_backup_selected, backupTargetLabel)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Button(
                                    onClick = onChooseBackupFile,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(stringResource(R.string.setup_backup_choose_folder_action))
                                }
                                Text(
                                    text = stringResource(R.string.setup_backup_auto_enable_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            SetupStepType.Contacts -> {
                                Text(
                                    text =
                                        when {
                                            contactsConfigured -> stringResource(R.string.setup_contacts_ready_hint)
                                            contactsPermissionGranted -> stringResource(R.string.setup_contacts_permission_granted_hint)
                                            else -> stringResource(R.string.setup_contacts_request_hint)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                Button(
                                    onClick = {
                                        if (contactsPermissionGranted) {
                                            onOpenContactsImport()
                                        } else {
                                            onRequestContactsPermission()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        if (contactsPermissionGranted) {
                                            stringResource(R.string.setup_contacts_import_action)
                                        } else {
                                            stringResource(R.string.import_contacts_grant_permission)
                                        }
                                    )
                                }

                                if (contactsPermissionPermanentlyDenied) {
                                    TextButton(
                                        onClick = onOpenAppSettings,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.permission_primer_open_settings))
                                    }
                                }
                            }

                            SetupStepType.Notifications -> {
                                Text(
                                    text =
                                        if (notificationsConfigured) {
                                            stringResource(R.string.setup_notifications_ready_hint)
                                        } else {
                                            stringResource(R.string.setup_notifications_request_hint)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                if (notificationsConfigured) {
                                    OutlinedButton(
                                        onClick = onDisableNotifications,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text(stringResource(R.string.setup_notifications_disable_action))
                                    }
                                } else {
                                    Button(
                                        onClick = onEnableNotifications,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text(stringResource(R.string.setup_action_enable))
                                    }
                                    if (notificationsPermissionPermanentlyDenied) {
                                        TextButton(
                                            onClick = onOpenAppSettings,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.permission_primer_open_settings))
                                        }
                                    }
                                }
                            }
                            SetupStepType.Helper -> {
                                Text(
                                    text =
                                        if (helperConfigured) {
                                            stringResource(R.string.setup_helper_ready_hint)
                                        } else {
                                            stringResource(R.string.setup_helper_request_hint)
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text =
                                        buildString {
                                            append(
                                                if (helperMicGranted) {
                                                    stringResource(R.string.setup_helper_status_mic_granted)
                                                } else {
                                                    stringResource(R.string.setup_helper_status_mic_missing)
                                                }
                                            )
                                            append(" • ")
                                            append(
                                                if (helperOverlayGranted) {
                                                    stringResource(R.string.setup_helper_status_overlay_granted)
                                                } else {
                                                    stringResource(R.string.setup_helper_status_overlay_missing)
                                                }
                                            )
                                        },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenHelperSetup,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(stringResource(R.string.setup_helper_open_action))
                                }
                            }
                        }

                        if (!step.required && !step.done) {
                            TextButton(
                                onClick = {
                                    if (page < steps.lastIndex) {
                                        moveToPage(page + 1)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isTransitioning
                            ) {
                                Text(stringResource(R.string.setup_step_skip_optional))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            moveToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0 && !isTransitioning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.setup_step_previous))
                }

                if (pagerState.currentPage < steps.lastIndex) {
                    Button(
                        onClick = {
                            if (currentStep.type == SetupStepType.Business) {
                                onSaveBusinessProfile(businessName, currency, timezone)
                            }
                            moveToPage(pagerState.currentPage + 1)
                        },
                        enabled = canAdvance && !isTransitioning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setup_step_next))
                    }
                } else {
                    Button(
                        onClick = onStartUsingApp,
                        enabled = onboardingState.businessProfileCompleted && !isTransitioning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setup_action_start))
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionPrimerScreen(
    title: String,
    body: String,
    whyTitle: String,
    whyLine: String,
    privacyTitle: String,
    privacyLine: String,
    continueLabel: String,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNotNow) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = whyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = whyLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = privacyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = privacyLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(continueLabel)
            }
            OutlinedButton(
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.permission_primer_not_now))
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    onboardingState: OnboardingState,
    onBack: () -> Unit,
    onSave: (name: String, currency: String, timezone: String) -> Unit
) {
    var businessName by rememberSaveable { mutableStateOf(onboardingState.businessName) }
    var currency by rememberSaveable {
        mutableStateOf(onboardingState.currency.ifBlank { "KES" })
    }
    var timezone by rememberSaveable {
        mutableStateOf(onboardingState.timezone.ifBlank { TimeZone.currentSystemDefault().id })
    }
    val canSave = businessName.trim().isNotEmpty() && currency.isNotBlank() && timezone.isNotBlank()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.business_profile_title)) },
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppCard {
                Text(
                    text = stringResource(R.string.business_profile_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text(stringResource(R.string.business_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            AppCard {
                Text(
                    text = stringResource(R.string.business_currency_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("KES", "USD", "EUR").forEach { option ->
                        FilterChip(
                            selected = currency == option,
                            onClick = { currency = option },
                            label = { Text(option) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = timezone,
                onValueChange = { timezone = it },
                label = { Text(stringResource(R.string.business_timezone_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ElevatedButton(
                onClick = { onSave(businessName, currency, timezone) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun IntroSlideCard(slide: IntroSlide, index: Int, total: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.intro_slide_position, index, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = slide.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntroValueRow(item: IntroValueItem) {
    AppCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChecklistItemCard(
    title: String,
    subtitle: String,
    done: Boolean,
    required: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit,
    statusText: String? = null,
    markDoneLabel: String? = null,
    onMarkDoneClick: (() -> Unit)? = null
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (required) stringResource(R.string.setup_required) else stringResource(R.string.setup_optional),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (required) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(actionLabel)
            }
            if (markDoneLabel != null && onMarkDoneClick != null) {
                TextButton(
                    onClick = onMarkDoneClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(markDoneLabel)
                }
            }
        }
    }
}

private data class SetupStep(
    val type: SetupStepType,
    val title: Int,
    val body: Int,
    val required: Boolean,
    val done: Boolean
)

private enum class SetupStepType {
    Business,
    Backup,
    Contacts,
    Notifications,
    Helper
}

private data class IntroValueItem(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private data class IntroSlide(
    val title: String,
    val body: String
)

@Composable
private fun IntroProgressDots(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == current) 10.dp else 8.dp)
                        .background(
                            color =
                                if (index == current) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
            )
        }
    }
}
