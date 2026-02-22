package com.zeynbakers.order_management_system

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zeynbakers.order_management_system.core.navigation.AppRoutes
import com.zeynbakers.order_management_system.core.onboarding.OnboardingPreferences
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val launchIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val onboardingCompleted = runBlocking { OnboardingPreferences(this@MainActivity).readState().onboardingCompleted }
        val startDestination = if (onboardingCompleted) AppRoutes.Calendar else AppRoutes.Intro
        enableEdgeToEdge()
        launchIntent.value = intent
        setContent {
            MainAppContent(
                activity = this,
                launchIntentState = launchIntent,
                startDestination = startDestination
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchIntent.value = intent
    }
}
