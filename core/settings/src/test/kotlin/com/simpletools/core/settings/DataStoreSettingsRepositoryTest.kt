package com.simpletools.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) {
            File(folder.root, DataStoreSettingsRepository.FILE_NAME)
        }
    }
    private val repository by lazy { DataStoreSettingsRepository(dataStore) }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `defaults are privacy friendly and follow the system theme`() = runBlocking {
        assertEquals(UserSettings(), repository.settings.first())
    }

    @Test
    fun `persists theme, dynamic color and onboarding`() = runBlocking {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setDynamicColor(true)
        repository.setOnboardingCompleted(true)

        val settings = repository.settings.first()

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertTrue(settings.dynamicColor)
        assertTrue(settings.onboardingCompleted)
    }

    @Test
    fun `unknown stored theme falls back to system`() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "sepia" }

        assertEquals(ThemeMode.SYSTEM, repository.settings.first().themeMode)
    }

    @Test
    fun `storage keys are stable`() {
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.storageKey })
    }

    @Test
    fun `theme mode resolves dark appearance`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }
}
