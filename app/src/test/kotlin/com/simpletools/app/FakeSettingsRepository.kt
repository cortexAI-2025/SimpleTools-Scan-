package com.simpletools.app

import com.simpletools.core.settings.SettingsRepository
import com.simpletools.core.settings.ThemeMode
import com.simpletools.core.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setThemeMode(themeMode: ThemeMode) = settings.update { it.copy(themeMode = themeMode) }

    override suspend fun setDynamicColor(enabled: Boolean) = settings.update { it.copy(dynamicColor = enabled) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        settings.update { it.copy(onboardingCompleted = completed) }
}
