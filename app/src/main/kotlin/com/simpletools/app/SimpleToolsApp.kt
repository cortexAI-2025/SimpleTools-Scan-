package com.simpletools.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.simpletools.app.navigation.SimpleToolsNavHost
import com.simpletools.core.common.AppConfig
import com.simpletools.core.privacy.PrivacyDeclaration
import com.simpletools.core.settings.SettingsRepository
import com.simpletools.feature.onboarding.OnboardingRoute
import com.simpletools.feature.scanner.ScanDocumentRoute
import com.simpletools.feature.scanner.ScannerDependencies

/** Root of the UI: chooses the start screen and hosts navigation. */
@Composable
fun SimpleToolsApp(
    appConfig: AppConfig,
    privacyDeclaration: PrivacyDeclaration,
    settingsRepository: SettingsRepository,
    scanner: ScannerDependencies,
    onboardingCompleted: Boolean,
    onOnboardingFinished: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Decided once: finishing onboarding navigates away instead of rebuilding the graph.
    val startDestination: Any = remember {
        if (appConfig.features.onboarding && !onboardingCompleted) OnboardingRoute else ScanDocumentRoute
    }
    SimpleToolsNavHost(
        navController = navController,
        startDestination = startDestination,
        appConfig = appConfig,
        privacyDeclaration = privacyDeclaration,
        settingsRepository = settingsRepository,
        scanner = scanner,
        onOnboardingFinished = onOnboardingFinished,
    )
}
