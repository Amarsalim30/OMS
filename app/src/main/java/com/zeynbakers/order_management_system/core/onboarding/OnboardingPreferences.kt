package com.zeynbakers.order_management_system.core.onboarding

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

data class OnboardingState(
    val onboardingCompleted: Boolean = false,
    val introCompleted: Boolean = false,
    val businessName: String = "",
    val currency: String = "KES",
    val timezone: String = "",
    val backupSetupDone: Boolean = false,
    val contactsSetupDone: Boolean = false,
    val notificationsSetupDone: Boolean = false,
    val helperSetupDone: Boolean = false
) {
    val businessProfileCompleted: Boolean
        get() = businessName.isNotBlank() && currency.isNotBlank() && timezone.isNotBlank()
}

class OnboardingPreferences(private val context: Context) {
    private object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val IntroCompleted = booleanPreferencesKey("intro_completed")
        val BusinessName = stringPreferencesKey("business_name")
        val Currency = stringPreferencesKey("currency")
        val Timezone = stringPreferencesKey("timezone")
        val BackupDone = booleanPreferencesKey("backup_done")
        val ContactsDone = booleanPreferencesKey("contacts_done")
        val NotificationsDone = booleanPreferencesKey("notifications_done")
        val HelperDone = booleanPreferencesKey("helper_done")
    }

    val state: Flow<OnboardingState> =
        context.onboardingDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map(::mapState)

    suspend fun readState(): OnboardingState = state.first()

    suspend fun setIntroCompleted(completed: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.IntroCompleted] = completed
        }
    }

    suspend fun saveBusinessProfile(name: String, currency: String, timezone: String) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.BusinessName] = name.trim()
            prefs[Keys.Currency] = currency.trim().ifBlank { "KES" }
            prefs[Keys.Timezone] = timezone.trim()
        }
    }

    suspend fun setBackupSetupDone(done: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.BackupDone] = done
        }
    }

    suspend fun setContactsSetupDone(done: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.ContactsDone] = done
        }
    }

    suspend fun setNotificationsSetupDone(done: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.NotificationsDone] = done
        }
    }

    suspend fun setHelperSetupDone(done: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.HelperDone] = done
        }
    }

    suspend fun markOnboardingCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.OnboardingCompleted] = true
        }
    }

    private fun mapState(prefs: Preferences): OnboardingState {
        return OnboardingState(
            onboardingCompleted = prefs[Keys.OnboardingCompleted] ?: false,
            introCompleted = prefs[Keys.IntroCompleted] ?: false,
            businessName = prefs[Keys.BusinessName] ?: "",
            currency = prefs[Keys.Currency] ?: "KES",
            timezone = prefs[Keys.Timezone] ?: "",
            backupSetupDone = prefs[Keys.BackupDone] ?: false,
            contactsSetupDone = prefs[Keys.ContactsDone] ?: false,
            notificationsSetupDone = prefs[Keys.NotificationsDone] ?: false,
            helperSetupDone = prefs[Keys.HelperDone] ?: false
        )
    }
}
