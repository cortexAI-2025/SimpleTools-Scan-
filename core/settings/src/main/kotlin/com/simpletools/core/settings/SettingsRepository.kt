package com.simpletools.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)
}

/** [SettingsRepository] persisted locally with Preferences DataStore. Nothing leaves the device. */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            UserSettings(
                themeMode = ThemeMode.fromStorageKey(preferences[Keys.THEME_MODE]),
                dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: false,
                onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.storageKey }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    companion object {
        const val FILE_NAME = "simpletools_settings.preferences_pb"

        /**
         * Creates the repository stored in [directory] (on Android: `context.filesDir`).
         * Call it once per process: DataStore forbids two instances on the same file.
         * DataStore stays an implementation detail of this module.
         */
        fun create(directory: File): SettingsRepository = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { File(directory, FILE_NAME) },
            ),
        )
    }
}
