package com.simpletools.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.feature.scanner.ScannerDependencies
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleToolsAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launch() {
        val config = buildConfigAppConfig()
        val scanner = ScannerDependencies.create(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            SimpleToolsTheme {
                SimpleToolsApp(
                    appConfig = config,
                    privacyDeclaration = PrivacyDeclaration.from(config),
                    settingsRepository = FakeSettingsRepository(),
                    scanner = scanner,
                    onboardingCompleted = false,
                    onOnboardingFinished = {},
                )
            }
        }
    }

    @Test
    fun `the scanner opens directly, without onboarding`() {
        launch()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Scan your first page")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Scan a page").assertExists()
    }

    @Test
    fun `scan a page asks for the camera before using it`() {
        launch()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Scan a page")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Scan a page").performClick()

        composeRule.onNodeWithText("Camera access").assertExists()
        composeRule.onNodeWithText("Allow camera").assertExists()
        composeRule.onNodeWithText("Import a photo").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Scan your first page").assertExists()
    }

    @Test
    fun `settings, privacy and about are reachable and back returns to the document`() {
        launch()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertExists()

        composeRule.onNodeWithText("Privacy").performScrollTo().performClick()
        composeRule.onNodeWithText("No account required").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("About").performScrollTo().performClick()
        composeRule.onNodeWithText("Fast. Simple. Useful.").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Scan your first page")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
