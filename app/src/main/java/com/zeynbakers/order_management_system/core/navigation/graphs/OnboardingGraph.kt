package com.zeynbakers.order_management_system.core.navigation.graphs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.backup.BackupPreferences
import com.zeynbakers.order_management_system.core.backup.BackupScheduler
import com.zeynbakers.order_management_system.core.backup.BackupTargetType
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.onboarding.BusinessProfileScreen
import com.zeynbakers.order_management_system.core.onboarding.IntroPagerScreen
import com.zeynbakers.order_management_system.core.onboarding.OnboardingState
import com.zeynbakers.order_management_system.core.onboarding.OnboardingPreferences
import com.zeynbakers.order_management_system.core.onboarding.PermissionPrimerScreen
import com.zeynbakers.order_management_system.core.onboarding.SetupChecklistScreen
import com.zeynbakers.order_management_system.core.onboarding.SplashScreen
import com.zeynbakers.order_management_system.core.notifications.NotificationPreferences
import com.zeynbakers.order_management_system.core.notifications.NotificationScheduler
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

internal fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController
) {
    composable(AppRoutes.Splash) {
        val prefs = remember { OnboardingPreferences(navController.context) }
        SplashScreen(
            onShouldEnterHome = { prefs.readState().onboardingCompleted },
            onOpenHome = {
                navController.navigate(AppRoutes.Calendar) {
                    popUpTo(AppRoutes.Splash) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onOpenIntro = {
                navController.navigate(AppRoutes.Intro) {
                    popUpTo(AppRoutes.Splash) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppRoutes.Intro) {
        val prefs = remember { OnboardingPreferences(navController.context) }
        val scope = rememberCoroutineScope()
        IntroPagerScreen(
            onSkip = {
                scope.launch { prefs.setIntroCompleted(true) }
                navController.navigate(AppRoutes.SetupChecklist) {
                    popUpTo(AppRoutes.Intro) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onFinish = {
                scope.launch { prefs.setIntroCompleted(true) }
                navController.navigate(AppRoutes.SetupChecklist) {
                    popUpTo(AppRoutes.Intro) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppRoutes.SetupChecklist) {
        val context = navController.context
        val prefs = remember { OnboardingPreferences(context) }
        val backupPrefs = remember { BackupPreferences(context) }
        val notificationPrefs = remember { NotificationPreferences(context) }
        val scope = rememberCoroutineScope()
        val activity = context.findActivity()
        val lifecycleOwner = LocalLifecycleOwner.current
        val state by prefs.state.collectAsState(initial = OnboardingState())
        var backupState by remember { mutableStateOf(backupPrefs.readState()) }
        var notificationsConfigured by remember { mutableStateOf(notificationPrefs.readSettings().enabled) }
        var contactsPermissionGranted by remember { mutableStateOf(hasContactsPermission(context)) }
        var contactsPermissionRequested by rememberSaveable { mutableStateOf(false) }
        var notificationsPermissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
        var notificationsPermissionRequested by rememberSaveable { mutableStateOf(false) }

        val contactsPermissionPermanentlyDenied =
            !contactsPermissionGranted &&
                contactsPermissionRequested &&
                activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.READ_CONTACTS
                    )
                } == true
        val notificationsPermissionPermanentlyDenied =
            requiresNotificationPermission() &&
                !notificationsPermissionGranted &&
                notificationsPermissionRequested &&
                activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } == true

        val contactsPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                contactsPermissionRequested = true
                contactsPermissionGranted = granted
                if (granted) {
                    navController.navigate(AppRoutes.ImportContacts) {
                        launchSingleTop = true
                    }
                }
            }
        val notificationsPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                notificationsPermissionRequested = true
                notificationsPermissionGranted = granted
                if (granted) {
                    notificationPrefs.setEnabled(true)
                    notificationsConfigured = true
                    scope.launch { prefs.setNotificationsSetupDone(true) }
                }
            }
        val backupFileLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                val persistFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val persisted =
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, persistFlags)
                        true
                    }.getOrDefault(false)
                if (!persisted) return@rememberLauncherForActivityResult

                val displayName =
                    runCatching { DocumentFile.fromSingleUri(context, uri)?.name?.takeIf { it.isNotBlank() } }
                        .getOrNull()
                backupPrefs.setFileTargetSelection(
                    uri = uri.toString(),
                    displayName = displayName,
                    authority = uri.authority
                )
                backupPrefs.setAutoEnabled(true)
                BackupScheduler.ensureScheduled(context)
                backupState = backupPrefs.readState()
                scope.launch { prefs.setBackupSetupDone(true) }
            }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    contactsPermissionGranted = hasContactsPermission(context)
                    notificationsPermissionGranted = hasNotificationPermission(context)
                    notificationsConfigured = notificationPrefs.readSettings().enabled
                    backupState = backupPrefs.readState()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val hasBackupFile =
            backupState.targetType == BackupTargetType.SafFile && backupState.targetUri?.isNotBlank() == true
        val backupConfigured = hasBackupFile
        val contactsConfigured = state.contactsSetupDone
        val backupTargetLabel =
            when {
                !hasBackupFile -> null
                !backupState.targetDisplayName.isNullOrBlank() -> backupState.targetDisplayName
                !backupState.targetUri.isNullOrBlank() -> backupState.targetUri
                else -> null
            }

        SetupChecklistScreen(
            onboardingState = state,
            backupConfigured = backupConfigured,
            backupTargetLabel = backupTargetLabel,
            contactsConfigured = contactsConfigured,
            contactsPermissionGranted = contactsPermissionGranted,
            contactsPermissionPermanentlyDenied = contactsPermissionPermanentlyDenied,
            notificationsConfigured = notificationsConfigured,
            notificationsPermissionPermanentlyDenied = notificationsPermissionPermanentlyDenied,
            onSaveBusinessProfile = { name, currency, timezone ->
                scope.launch {
                    prefs.saveBusinessProfile(
                        name = name,
                        currency = currency,
                        timezone = timezone.ifBlank { TimeZone.currentSystemDefault().id }
                    )
                }
            },
            onChooseBackupFile = {
                backupFileLauncher.launch("intialsetupbackupsave.oms")
            },
            onRequestContactsPermission = {
                if (contactsPermissionGranted) {
                    navController.navigate(AppRoutes.ImportContacts) {
                        launchSingleTop = true
                    }
                } else {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            onOpenContactsImport = {
                navController.navigate(AppRoutes.ImportContacts) {
                    launchSingleTop = true
                }
            },
            onEnableNotifications = {
                if (!requiresNotificationPermission() || notificationsPermissionGranted) {
                    NotificationScheduler.setEnabled(context, true)
                    notificationsConfigured = notificationPrefs.readSettings().enabled
                    scope.launch { prefs.setNotificationsSetupDone(true) }
                } else {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDisableNotifications = {
                NotificationScheduler.setEnabled(context, false)
                notificationsConfigured = notificationPrefs.readSettings().enabled
                scope.launch { prefs.setNotificationsSetupDone(false) }
            },
            onOpenAppSettings = { openAppSettings(context) },
            onStartUsingApp = {
                navController.navigate(AppRoutes.FirstRunTutorial) {
                    popUpTo(AppRoutes.SetupChecklist) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppRoutes.BusinessProfile) {
        val prefs = remember { OnboardingPreferences(navController.context) }
        val scope = rememberCoroutineScope()
        val state by prefs.state.collectAsState(initial = OnboardingState())
        BusinessProfileScreen(
            onboardingState = state,
            onBack = { navController.popBackStack() },
            onSave = { name, currency, timezone ->
                scope.launch {
                    prefs.saveBusinessProfile(
                        name = name,
                        currency = currency,
                        timezone = timezone.ifBlank { TimeZone.currentSystemDefault().id }
                    )
                    navController.popBackStack()
                }
            }
        )
    }

    composable(AppRoutes.ContactsPermissionPrimer) {
        val context = LocalContext.current
        var hasPermission by remember { mutableStateOf(hasContactsPermission(context)) }
        var permissionRequested by rememberSaveable { mutableStateOf(false) }
        val activity = context.findActivity()
        val permanentlyDenied =
            !hasPermission &&
                permissionRequested &&
                activity?.let {
                    !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.READ_CONTACTS
                    )
                } == true

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permissionRequested = true
                hasPermission = granted
                if (granted) {
                    navController.navigate(AppRoutes.ImportContacts) {
                        popUpTo(AppRoutes.ContactsPermissionPrimer) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

        PermissionPrimerScreen(
            title = stringResource(R.string.permission_primer_contacts_title),
            body = stringResource(R.string.permission_primer_contacts_body),
            whyTitle = stringResource(R.string.permission_primer_why_title),
            whyLine = stringResource(R.string.permission_primer_contacts_why_line),
            privacyTitle = stringResource(R.string.permission_primer_privacy_title),
            privacyLine = stringResource(R.string.permission_primer_contacts_privacy_line),
            continueLabel = stringResource(R.string.permission_primer_continue),
            onContinue = {
                if (hasPermission) {
                    navController.navigate(AppRoutes.ImportContacts) {
                        popUpTo(AppRoutes.ContactsPermissionPrimer) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    launcher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            onNotNow = { navController.popBackStack() },
            secondaryActionLabel =
                if (permanentlyDenied) stringResource(R.string.permission_primer_open_settings) else null,
            onSecondaryAction =
                if (permanentlyDenied) {
                    { openAppSettings(context) }
                } else {
                    null
                }
        )
    }

    composable(AppRoutes.NotificationsPermissionPrimer) {
        PermissionPrimerScreen(
            title = stringResource(R.string.permission_primer_notifications_title),
            body = stringResource(R.string.permission_primer_notifications_body),
            whyTitle = stringResource(R.string.permission_primer_why_title),
            whyLine = stringResource(R.string.permission_primer_notifications_why_line),
            privacyTitle = stringResource(R.string.permission_primer_privacy_title),
            privacyLine = stringResource(R.string.permission_primer_notifications_privacy_line),
            continueLabel = stringResource(R.string.permission_primer_continue),
            onContinue = {
                navController.navigate(AppRoutes.Notifications) {
                    popUpTo(AppRoutes.NotificationsPermissionPrimer) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNotNow = { navController.popBackStack() }
        )
    }

    composable(AppRoutes.FirstRunTutorial) {
        val prefs = remember { OnboardingPreferences(navController.context) }
        var routed by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!routed) {
                routed = true
                prefs.markOnboardingCompleted()
                navController.navigate(AppRoutes.CalendarTutorial) {
                    popUpTo(AppRoutes.FirstRunTutorial) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

private fun hasContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (requiresNotificationPermission()) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun requiresNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT >= 33
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
