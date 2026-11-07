package com.simpletools.feature.settings

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.settings.SettingsRepository
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object PrivacyRoute

fun NavController.navigateToSettings() = navigate(SettingsRoute) { launchSingleTop = true }

fun NavController.navigateToPrivacy() = navigate(PrivacyRoute) { launchSingleTop = true }

/** Registers the Settings and Privacy screens. */
fun NavGraphBuilder.settingsScreens(
    settingsRepository: SettingsRepository,
    privacy: PrivacyDeclaration,
    onNavigateBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPro: (() -> Unit)? = null,
) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settingsRepository))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsScreenRoute(
            uiState = uiState,
            onThemeModeSelected = viewModel::setThemeMode,
            onDynamicColorChanged = viewModel::setDynamicColor,
            onNavigateBack = onNavigateBack,
            onOpenPrivacy = onOpenPrivacy,
            onOpenAbout = onOpenAbout,
            onOpenPro = onOpenPro,
        )
    }
    composable<PrivacyRoute> {
        PrivacyScreen(declaration = privacy, onNavigateBack = onNavigateBack)
    }
}
