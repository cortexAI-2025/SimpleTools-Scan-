package com.simpletools.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpletools.core.designsystem.theme.SimpleToolsAccent
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.settings.UserSettings
import com.simpletools.core.settings.isDark

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as SimpleToolsApplication).container

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(container.settingsRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val accent = SimpleToolsAccent.fromKey(container.appConfig.accent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            // Settings are read from a local file in a few ms; until then the themed window
            // background is shown, which avoids flashing the wrong theme or start screen.
            val state = uiState
            if (state is MainUiState.Ready) {
                AppContent(settings = state.settings, accent = accent)
            }
        }
    }

    @Composable
    private fun AppContent(settings: UserSettings, accent: SimpleToolsAccent) {
        val darkTheme = settings.themeMode.isDark(isSystemInDarkTheme())

        DisposableEffect(darkTheme) {
            val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose {}
        }

        SimpleToolsTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor, accent = accent) {
            SimpleToolsApp(
                appConfig = container.appConfig,
                privacyDeclaration = container.privacyDeclaration,
                settingsRepository = container.settingsRepository,
                onboardingCompleted = settings.onboardingCompleted,
                onOnboardingFinished = viewModel::completeOnboarding,
            )
        }
    }
}
