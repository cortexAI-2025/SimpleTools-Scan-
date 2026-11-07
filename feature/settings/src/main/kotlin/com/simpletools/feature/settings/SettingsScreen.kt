package com.simpletools.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.isDynamicColorSupported
import com.simpletools.core.localization.LanguageSettings
import com.simpletools.core.settings.ThemeMode
import com.simpletools.core.settings.UserSettings
import com.simpletools.core.ui.LoadingState
import com.simpletools.core.ui.SettingsItem
import com.simpletools.core.ui.SettingsRadioItem
import com.simpletools.core.ui.SettingsSectionHeader
import com.simpletools.core.ui.SettingsSwitchItem
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.core.ui.startActivitySafely

/** Connects [SettingsScreen] to platform services (language settings). */
@Composable
internal fun SettingsScreenRoute(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPro: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val language = remember(configuration) {
        LanguageSettings.currentLanguage(context)
    }
    val languageSettingsIntent = remember(context) { LanguageSettings.appLanguageSettingsIntent(context) }
    SettingsScreen(
        uiState = uiState,
        showDynamicColor = isDynamicColorSupported,
        currentLanguageName = languageSettingsIntent?.let { stringResource(language.nativeNameRes) },
        onThemeModeSelected = onThemeModeSelected,
        onDynamicColorChanged = onDynamicColorChanged,
        onOpenLanguageSettings = { languageSettingsIntent?.let(context::startActivitySafely) },
        onNavigateBack = onNavigateBack,
        onOpenPrivacy = onOpenPrivacy,
        onOpenAbout = onOpenAbout,
        onOpenPro = onOpenPro,
    )
}

/**
 * Shared settings: appearance, language, privacy, about. Products add their own section
 * only when they have a real setting to offer — keep this screen short.
 *
 * @param currentLanguageName null hides the language row (per-app language needs Android 13+).
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    showDynamicColor: Boolean,
    currentLanguageName: String?,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPro: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SimpleToolsTopBar(title = stringResource(R.string.feature_settings_title), onNavigateBack = onNavigateBack)
        },
    ) { padding ->
        when (uiState) {
            SettingsUiState.Loading -> LoadingState(Modifier.padding(padding))
            is SettingsUiState.Ready -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                SettingsContent(
                    settings = uiState.settings,
                    showDynamicColor = showDynamicColor,
                    currentLanguageName = currentLanguageName,
                    onThemeModeSelected = onThemeModeSelected,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onOpenLanguageSettings = onOpenLanguageSettings,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenAbout = onOpenAbout,
                    onOpenPro = onOpenPro,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettings,
    showDynamicColor: Boolean,
    currentLanguageName: String?,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPro: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .widthIn(max = MaxContentWidth)
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSectionHeader(stringResource(R.string.feature_settings_appearance))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                SettingsRadioItem(
                    title = stringResource(mode.labelRes),
                    selected = settings.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                )
            }
        }
        if (showDynamicColor) {
            SettingsSwitchItem(
                title = stringResource(R.string.feature_settings_dynamic_color),
                summary = stringResource(R.string.feature_settings_dynamic_color_summary),
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColorChanged,
                icon = SimpleToolsIcons.Palette,
            )
        }

        if (currentLanguageName != null) {
            SettingsSectionHeader(stringResource(R.string.feature_settings_language))
            SettingsItem(
                title = stringResource(R.string.feature_settings_app_language),
                summary = currentLanguageName,
                icon = SimpleToolsIcons.Language,
                trailingIcon = SimpleToolsIcons.OpenInNew,
                onClick = onOpenLanguageSettings,
            )
        }

        if (onOpenPro != null) {
            SettingsSectionHeader(stringResource(R.string.feature_settings_pro_section))
            SettingsItem(
                title = stringResource(R.string.feature_settings_pro_title),
                summary = stringResource(R.string.feature_settings_pro_summary),
                icon = SimpleToolsIcons.Info,
                onClick = onOpenPro,
            )
        }

        SettingsSectionHeader(stringResource(R.string.feature_settings_information))
        SettingsItem(
            title = stringResource(R.string.feature_settings_privacy),
            summary = stringResource(R.string.feature_settings_privacy_summary),
            icon = SimpleToolsIcons.Lock,
            onClick = onOpenPrivacy,
        )
        SettingsItem(
            title = stringResource(R.string.feature_settings_about),
            icon = SimpleToolsIcons.Info,
            onClick = onOpenAbout,
        )
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
        ThemeMode.DARK -> R.string.feature_settings_theme_dark
    }
