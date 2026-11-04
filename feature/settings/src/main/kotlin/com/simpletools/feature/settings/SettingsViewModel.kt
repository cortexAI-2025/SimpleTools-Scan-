package com.simpletools.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.core.settings.ThemeMode
import com.simpletools.core.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: UserSettings) : SettingsUiState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map<UserSettings, SettingsUiState>(SettingsUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository) }
        }
    }
}
