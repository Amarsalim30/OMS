package com.zeynbakers.order_management_system

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
            val startDestination =
                runCatching {
                    val onboardingState = OnboardingPreferences(this@MainActivity).readState()
                    when {
                        onboardingState.onboardingCompleted -> AppRoutes.Calendar
                        onboardingState.introCompleted -> AppRoutes.SetupChecklist
                        else -> AppRoutes.Intro
                    }
                }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to read onboarding state; falling back to intro route", error)
                    }
                    .getOrDefault(AppRoutes.Intro)
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

    private companion object {
        const val TAG = "MainActivity"
    }
}
