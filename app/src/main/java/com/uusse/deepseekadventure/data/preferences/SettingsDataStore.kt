package com.uusse.deepseekadventure.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uusse.deepseekadventure.core.network.apiJson
import com.uusse.deepseekadventure.domain.repository.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val keySettings = stringPreferencesKey("settings_json")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[keySettings].orEmpty()
        if (raw.isBlank()) AppSettings()
        else runCatching { apiJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.updateData { prefs ->
            val raw = prefs[keySettings].orEmpty()
            val current = if (raw.isBlank()) AppSettings()
            else runCatching { apiJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
            prefs.toMutablePreferences().apply {
                this[keySettings] = apiJson.encodeToString(transform(current))
            }.toPreferences()
        }
    }
}
