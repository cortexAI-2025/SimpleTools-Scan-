package com.simpletools.core.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Tracks and requests one [AppPermission] from Compose. Obtain it with [rememberPermissionController]. */
@Stable
class PermissionController internal constructor(
    val permission: AppPermission,
    val status: PermissionStatus,
    private val onRequest: () -> Unit,
    private val onOpenSettings: () -> Unit,
) {
    val isGranted: Boolean get() = status == PermissionStatus.Granted

    /** Shows the system dialog. Call it from a user action, after explaining why. */
    fun request() = onRequest()

    /** Opens this app's system settings page, for [PermissionStatus.PermanentlyDenied]. */
    fun openAppSettings() = onOpenSettings()
}

@Composable
fun rememberPermissionController(permission: AppPermission): PermissionController {
    val context = LocalContext.current
    val runtimePermissions = remember(permission) { permission.runtimePermissions() }
    var hasRequested by rememberSaveable(permission) { mutableStateOf(false) }
    // Bumped when returning to the screen (e.g. from the settings app) to re-read the state.
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshKey++ }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasRequested = true
        refreshKey++
    }

    val status = remember(permission, hasRequested, refreshKey) {
        val granted = runtimePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        val activity = context.findActivity()
        val shouldShowRationale = activity != null &&
            runtimePermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        PermissionStatus.resolve(granted, shouldShowRationale, hasRequested)
    }

    return PermissionController(
        permission = permission,
        status = status,
        onRequest = {
            if (runtimePermissions.isNotEmpty()) launcher.launch(runtimePermissions.toTypedArray())
        },
        onOpenSettings = { context.openAppSettings() },
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
