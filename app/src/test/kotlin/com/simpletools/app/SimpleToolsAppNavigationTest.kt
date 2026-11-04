package com.simpletools.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.common.AppConfig
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.privacy.PrivacyDeclaration
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleToolsAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var onboardingFinished = false

    private fun launch(onboardingEnabled: Boolean, onboardingCompleted: Boolean = false) {
        val config = buildConfigAppConfig().let {
            it.copy(features = it.features.copy(onboarding = onboardingEnabled))
        }
        composeRule.setContent {
            SimpleToolsTheme {
                SimpleToolsApp(
                    appConfig = config,
                    privacyDeclaration = PrivacyDeclaration.from(config),
                    settingsRepository = FakeSettingsRepository(),
                    onboardingCompleted = onboardingCompleted,
                    onOnboardingFinished = { onboardingFinished = true },
                )
            }
        }
    }

    @Test
    fun `first launch shows onboarding then the main utility`() {
        launch(onboardingEnabled = true)

        composeRule.onNodeWithText("Get started").performClick()

        assertTrue(onboardingFinished)
        composeRule.onNodeWithText("Your tool starts here").assertExists()
        composeRule.onNodeWithText("Get started").assertDoesNotExist()
    }

    @Test
    fun `completed onboarding opens the main utility directly`() {
        launch(onboardingEnabled = true, onboardingCompleted = true)

        composeRule.onNodeWithText("Your tool starts here").assertExists()
    }

    @Test
    fun `settings, privacy and about are reachable and back returns home`() {
        launch(onboardingEnabled = false)

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertExists()

        composeRule.onNodeWithText("Privacy").performScrollTo().performClick()
        composeRule.onNodeWithText("No account required").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("About").performScrollTo().performClick()
        composeRule.onNodeWithText("Fast. Simple. Useful.").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Your tool starts here").assertExists()
    }
}
