package com.simpletools.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.simpletools.core.privacy.PrivacyDeclaration
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

/**
 * A single welcome screen, shown once when `features.onboarding=true`.
 * [onFinished] must persist completion and navigate to the main utility.
 */
fun NavGraphBuilder.onboardingScreen(
    privacy: PrivacyDeclaration,
    onFinished: () -> Unit,
) {
    composable<OnboardingRoute> {
        OnboardingScreen(privacy = privacy, onFinished = onFinished)
    }
}
