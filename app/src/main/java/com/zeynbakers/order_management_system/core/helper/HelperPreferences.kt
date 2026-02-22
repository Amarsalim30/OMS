package com.zeynbakers.order_management_system.core.helper

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.helperDataStore by preferencesDataStore(name = "helper_prefs")

data class HelperSettingsState(
    val enabled: Boolean = false,
    val fallbackOnly: Boolean = false,
    val themePreset: HelperThemePreset = HelperThemePreset.Brand,
    val smartHideEnabled: Boolean = true,
    val idlePeekSeconds: Int = 4,
    val idlePeekAlphaPercent: Int = 42,
    val bubbleX: Int = Int.MIN_VALUE,
    val bubbleY: Int = Int.MIN_VALUE,
    val dockLeft: Boolean = false
) {
    val hasSavedBubblePosition: Boolean
        get() = bubbleX != Int.MIN_VALUE && bubbleY != Int.MIN_VALUE
}

class HelperPreferences(private val context: Context) {
    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val FallbackOnly = booleanPreferencesKey("fallback_only")
        val ThemePreset = stringPreferencesKey("theme_preset")
        val SmartHideEnabled = booleanPreferencesKey("smart_hide_enabled")
        val IdlePeekSeconds = intPreferencesKey("idle_peek_seconds")
        val IdlePeekAlphaPercent = intPreferencesKey("idle_peek_alpha_percent")
        val BubbleX = intPreferencesKey("bubble_x")
        val BubbleY = intPreferencesKey("bubble_y")
        val DockLeft = booleanPreferencesKey("dock_left")
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

    suspend fun setThemePreset(preset: HelperThemePreset) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.ThemePreset] = preset.wireValue
        }
    }

    suspend fun setSmartHideEnabled(enabled: Boolean) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.SmartHideEnabled] = enabled
        }
    }

    suspend fun setIdlePeekSeconds(seconds: Int) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.IdlePeekSeconds] = seconds.coerceIn(2, 12)
        }
    }

    suspend fun setIdlePeekAlphaPercent(percent: Int) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.IdlePeekAlphaPercent] = percent.coerceIn(20, 85)
        }
    }

    suspend fun saveBubblePosition(x: Int, y: Int, dockLeft: Boolean) {
        context.helperDataStore.edit { prefs ->
            prefs[Keys.BubbleX] = x
            prefs[Keys.BubbleY] = y
            prefs[Keys.DockLeft] = dockLeft
        }
    }

    suspend fun clearBubblePosition() {
        context.helperDataStore.edit { prefs ->
            prefs.remove(Keys.BubbleX)
            prefs.remove(Keys.BubbleY)
            prefs.remove(Keys.DockLeft)
        }
    }

    private fun mapState(prefs: Preferences): HelperSettingsState {
        return HelperSettingsState(
            enabled = prefs[Keys.Enabled] ?: false,
            fallbackOnly = prefs[Keys.FallbackOnly] ?: false,
            themePreset = HelperThemePreset.fromWireValue(prefs[Keys.ThemePreset]),
            smartHideEnabled = prefs[Keys.SmartHideEnabled] ?: true,
            idlePeekSeconds = (prefs[Keys.IdlePeekSeconds] ?: 4).coerceIn(2, 12),
            idlePeekAlphaPercent = (prefs[Keys.IdlePeekAlphaPercent] ?: 42).coerceIn(20, 85),
            bubbleX = prefs[Keys.BubbleX] ?: Int.MIN_VALUE,
            bubbleY = prefs[Keys.BubbleY] ?: Int.MIN_VALUE,
            dockLeft = prefs[Keys.DockLeft] ?: false
        )
    }
}
