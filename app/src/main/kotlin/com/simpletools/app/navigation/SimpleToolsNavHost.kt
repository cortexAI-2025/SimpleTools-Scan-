package com.simpletools.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.simpletools.core.common.AppConfig
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.feature.about.aboutScreen
import com.simpletools.feature.about.navigateToAbout
import com.simpletools.feature.onboarding.OnboardingRoute
import com.simpletools.feature.onboarding.onboardingScreen
import com.simpletools.feature.scanner.ScanDocumentRoute
import com.simpletools.feature.scanner.ScannerDependencies
import com.simpletools.feature.scanner.scannerScreens
import com.simpletools.feature.settings.navigateToPrivacy
import com.simpletools.feature.settings.navigateToSettings
import com.simpletools.feature.settings.settingsScreens

/**
 * Navigation of SimpleTools Scan: document (pages) → capture → crop, and settings
 * (privacy, about) from the top bar.
 */
@Composable
fun SimpleToolsNavHost(
    navController: NavHostController,
    startDestination: Any,
    appConfig: AppConfig,
    privacyDeclaration: PrivacyDeclaration,
    settingsRepository: SettingsRepository,
    scanner: ScannerDependencies,
    onOnboardingFinished: () -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        onboardingScreen(
            privacy = privacyDeclaration,
            onFinished = {
                onOnboardingFinished()
                navController.navigate(ScanDocumentRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
        scannerScreens(deps = scanner, navController = navController, onOpenSettings = navController::navigateToSettings)
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
