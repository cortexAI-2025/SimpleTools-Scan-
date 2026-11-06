package com.simpletools.feature.scanner

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.simpletools.feature.scanner.capture.CaptureRoute
import com.simpletools.feature.scanner.capture.CaptureViewModel
import com.simpletools.feature.scanner.document.DocumentRoute
import com.simpletools.feature.scanner.document.DocumentViewModel
import com.simpletools.feature.scanner.edit.EditRoute
import com.simpletools.feature.scanner.edit.EditViewModel
import kotlinx.serialization.Serializable

/** Main screen: the pages of the document. Start destination of SimpleTools Scan. */
@Serializable
data object ScanDocumentRoute

/** Camera; [replacePageId] is set when retaking an existing page. */
@Serializable
data class ScanCaptureRoute(val replacePageId: String? = null)

/** Crop editor of the document's unfinished page. */
@Serializable
data object ScanEditRoute

/**
 * Document → capture → crop → back to the document. Crop and capture never stay in the back
 * stack once a page is saved, so Back from the document always leaves the app.
 */
fun NavGraphBuilder.scannerScreens(
    deps: ScannerDependencies,
    navController: NavController,
    onOpenSettings: () -> Unit,
) {
    composable<ScanDocumentRoute> {
        DocumentRoute(
            viewModel = viewModel(factory = DocumentViewModel.factory(deps)),
            imageProcessor = deps.imageProcessor,
            onScanPage = { navController.navigate(ScanCaptureRoute()) },
            onRetakePage = { pageId -> navController.navigate(ScanCaptureRoute(replacePageId = pageId)) },
            onOpenDraft = { navController.navigate(ScanEditRoute) { launchSingleTop = true } },
            onOpenSettings = onOpenSettings,
        )
    }
    composable<ScanCaptureRoute> { entry ->
        val route = entry.toRoute<ScanCaptureRoute>()
        CaptureRoute(
            viewModel = viewModel(factory = CaptureViewModel.factory(deps, route.replacePageId)),
            isRetake = route.replacePageId != null,
            onPhotoReady = {
                navController.navigate(ScanEditRoute) {
                    popUpTo<ScanCaptureRoute> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateBack = { navController.navigateUp() },
        )
    }
    composable<ScanEditRoute> {
        EditRoute(
            viewModel = viewModel(factory = EditViewModel.factory(deps)),
            onFinished = { navController.popBackStack(ScanDocumentRoute, inclusive = false) },
        )
    }
}
