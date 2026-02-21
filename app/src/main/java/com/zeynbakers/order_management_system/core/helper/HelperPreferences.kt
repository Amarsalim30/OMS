package com.zeynbakers.order_management_system.core.helper

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.helperDataStore by preferencesDataStore(name = "helper_prefs")

data class HelperSettingsState(
    val enabled: Boolean = false,
    val fallbackOnly: Boolean = false
)

class HelperPreferences(private val context: Context) {
    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val FallbackOnly = booleanPreferencesKey("fallback_only")
    }

    val state: Flow<HelperSettingsState> =
        context.helperDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map(::mapState)

    suspend fun readState(): HelperSettingsState = state.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.Enabled] = enabled
        }
    }

    suspend fun setFallbackOnly(enabled: Boolean) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.FallbackOnly] = enabled
        }
    }

    private fun mapState(prefs: Preferences): HelperSettingsState {
        return HelperSettingsState(
            enabled = prefs[Keys.Enabled] ?: false,
            fallbackOnly = prefs[Keys.FallbackOnly] ?: false
        )
    }
}
