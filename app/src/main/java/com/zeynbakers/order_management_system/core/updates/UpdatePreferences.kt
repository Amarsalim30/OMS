package com.zeynbakers.order_management_system.core.updates

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.updateDataStore by preferencesDataStore(name = "update_prefs")

class UpdatePreferences(context: Context) {
    private val dataStore = context.applicationContext.updateDataStore

    private object Keys {
        val LastVersionSeen = stringPreferencesKey("last_version_seen")
    }

    val lastVersionSeen: Flow<String?> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs -> prefs[Keys.LastVersionSeen] }

    suspend fun shouldShowUpdate(versionName: String): Boolean {
        val lastSeen = lastVersionSeen.firstOrNull()
        return lastSeen == null || lastSeen != versionName
    }

    suspend fun markVersionSeen(versionName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LastVersionSeen] = versionName
        }
    }
}
