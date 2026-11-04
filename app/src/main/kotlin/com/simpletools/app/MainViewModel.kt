package com.simpletools.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.core.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val settings: UserSettings) : MainUiState
}

class MainViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map<UserSettings, MainUiState>(MainUiState::Ready)
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = viewModelFactory {
            initializer { MainViewModel(settingsRepository) }
        }
    }
}
