package com.simpletools.feature.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AboutRoute

fun NavController.navigateToAbout() = navigate(AboutRoute) { launchSingleTop = true }

fun NavGraphBuilder.aboutScreen(
    versionName: String,
    privacyPolicyUrl: String?,
    onNavigateBack: () -> Unit,
) {
    composable<AboutRoute> {
        AboutScreen(versionName = versionName, privacyPolicyUrl = privacyPolicyUrl, onNavigateBack = onNavigateBack)
    }
}
