package com.zeynbakers.order_management_system

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.licensing.AuthGate
import com.zeynbakers.order_management_system.core.onboarding.OnboardingPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        var shouldKeepSplash = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { shouldKeepSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchIntent.value = intent
        lifecycleScope.launch {
            val onboardingCompleted =
                OnboardingPreferences(this@MainActivity).readState().onboardingCompleted
            val startDestination = if (onboardingCompleted) AppRoutes.Calendar else AppRoutes.Intro
            shouldKeepSplash = false
            setContent {
                AuthGate {
                    MainAppContent(
                        activity = this@MainActivity,
                        launchIntentState = launchIntent,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchIntent.value = intent
    }
}
