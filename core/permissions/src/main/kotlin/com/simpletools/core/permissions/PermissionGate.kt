package com.simpletools.core.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsTextButton
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing

/**
 * Shows [content] when [permission] is granted; otherwise explains why it is needed and
 * lets the user decide. The system dialog is only shown after the user taps "Allow".
 *
 * Place it around the feature that needs the permission (e.g. the camera preview),
 * never around the whole app.
 */
@Composable
fun PermissionGate(
    permission: AppPermission,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val controller = rememberPermissionController(permission)
    if (controller.isGranted) {
        content()
    } else {
        PermissionRationale(
            permission = permission,
            status = controller.status,
            onRequest = controller::request,
            onOpenSettings = controller::openAppSettings,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/** Stateless explanation screen, reusable when a product drives the permission flow itself. */
@Composable
fun PermissionRationale(
    permission: AppPermission,
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(permission.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(permission.rationaleRes),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (status == PermissionStatus.PermanentlyDenied) {
                Text(
                    text = stringResource(R.string.core_permissions_permanently_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                SimpleToolsPrimaryButton(
                    text = stringResource(R.string.core_permissions_open_settings),
                    onClick = onOpenSettings,
                )
            } else {
                SimpleToolsPrimaryButton(
                    text = stringResource(R.string.core_permissions_allow),
                    onClick = onRequest,
                )
            }
            if (onDismiss != null) {
                SimpleToolsTextButton(text = stringResource(R.string.core_permissions_not_now), onClick = onDismiss)
            }
        }
    }
}
