package com.simpletools.feature.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.settings.ThemeMode
import com.simpletools.core.settings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var selectedTheme: ThemeMode? = null
    private var privacyOpened = false

    private fun setContent(languageName: String? = null) {
        composeRule.setContent {
            SimpleToolsTheme {
                SettingsScreen(
                    uiState = SettingsUiState.Ready(UserSettings(themeMode = ThemeMode.SYSTEM)),
                    showDynamicColor = false,
                    currentLanguageName = languageName,
                    onThemeModeSelected = { selectedTheme = it },
                    onDynamicColorChanged = {},
                    onOpenLanguageSettings = {},
                    onNavigateBack = {},
                    onOpenPrivacy = { privacyOpened = true },
                    onOpenAbout = {},
                )
            }
        }
    }

    @Test
    fun `shows the current theme and reports a new choice`() {
        setContent()

        composeRule.onNodeWithText("System default").assertIsSelected()
        composeRule.onNodeWithText("Dark").performScrollTo().performClick()

        assertEquals(ThemeMode.DARK, selectedTheme)
    }

    @Test
    fun `opens the privacy screen`() {
        setContent()

        composeRule.onNodeWithText("Privacy").performScrollTo().performClick()

        assertEquals(true, privacyOpened)
    }

    @Test
    fun `language row is hidden when per-app language is unavailable`() {
        setContent(languageName = null)

        composeRule.onNodeWithText("App language").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `is translated in french`() {
        setContent(languageName = "Français")

        composeRule.onNodeWithText("Paramètres").assertExists()
        composeRule.onNodeWithText("Langue de l’application").assertExists()
    }
}
