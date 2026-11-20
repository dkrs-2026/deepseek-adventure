package com.uusse.deepseekadventure.data.repository

import com.uusse.deepseekadventure.core.security.SecurePrefs
import com.uusse.deepseekadventure.data.preferences.SettingsDataStore
import com.uusse.deepseekadventure.domain.repository.AppSettings
import com.uusse.deepseekadventure.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore,
    private val securePrefs: SecurePrefs,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) = dataStore.update(transform)

    override suspend fun saveApiKey(key: String) = securePrefs.setApiKey(key)

    override fun apiKey(): String = securePrefs.getApiKey()

    override fun maskedApiKey(): String = securePrefs.masked()

    override fun hasApiKey(): Boolean = securePrefs.hasKey()
}
