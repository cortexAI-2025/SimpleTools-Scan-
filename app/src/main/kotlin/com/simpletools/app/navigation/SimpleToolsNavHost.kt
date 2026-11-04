package com.simpletools.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.simpletools.app.home.HomeRoute
import com.simpletools.app.home.homeScreen
import com.simpletools.core.common.AppConfig
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.feature.about.aboutScreen
import com.simpletools.feature.about.navigateToAbout
import com.simpletools.feature.onboarding.OnboardingRoute
import com.simpletools.feature.onboarding.onboardingScreen
import com.simpletools.feature.settings.navigateToPrivacy
import com.simpletools.feature.settings.navigateToSettings
import com.simpletools.feature.settings.settingsScreens

/**
 * Top-level navigation. Kept intentionally flat: main utility → settings → privacy/about.
 * A product registers its own screens here (see docs/ADD_FEATURE_MODULE.md).
 */
@Composable
fun SimpleToolsNavHost(
    navController: NavHostController,
    startDestination: Any,
    appConfig: AppConfig,
    privacyDeclaration: PrivacyDeclaration,
    settingsRepository: SettingsRepository,
    onOnboardingFinished: () -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        onboardingScreen(
            privacy = privacyDeclaration,
            onFinished = {
                onOnboardingFinished()
                navController.navigate(HomeRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
        homeScreen(onOpenSettings = navController::navigateToSettings)
        settingsScreens(
            settingsRepository = settingsRepository,
            privacy = privacyDeclaration,
            onNavigateBack = navController::navigateUp,
            onOpenPrivacy = navController::navigateToPrivacy,
            onOpenAbout = navController::navigateToAbout,
        )
        aboutScreen(
            versionName = appConfig.versionName,
            privacyPolicyUrl = appConfig.privacy.policyUrl,
            onNavigateBack = navController::navigateUp,
        )
    }
}
