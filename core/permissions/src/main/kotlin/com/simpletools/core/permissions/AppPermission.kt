package com.simpletools.core.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes

/**
 * Sensitive capabilities a SimpleTools feature may need, with the user-facing
 * explanation shown before the system dialog.
 *
 * Supporting a permission here never declares it: a product must add it to its
 * manifest and to `permissions.declared`, and request it only from the feature
 * that uses it, at the moment the user triggers that feature.
 *
 * Files and documents are intentionally absent: use the Storage Access Framework
 * (ACTION_OPEN_DOCUMENT / ActivityResultContracts.OpenDocument), which needs no permission.
 */
enum class AppPermission(
    private val manifestPermission: String,
    /** First API level where this is a runtime permission; below it, it is granted at install or not needed. */
    private val runtimeSinceSdk: Int,
    @StringRes val titleRes: Int,
    @StringRes val rationaleRes: Int,
) {
    CAMERA(
        manifestPermission = Manifest.permission.CAMERA,
        runtimeSinceSdk = Build.VERSION_CODES.M,
        titleRes = R.string.core_permissions_camera_title,
        rationaleRes = R.string.core_permissions_camera_rationale,
    ),
    MICROPHONE(
        manifestPermission = Manifest.permission.RECORD_AUDIO,
        runtimeSinceSdk = Build.VERSION_CODES.M,
        titleRes = R.string.core_permissions_microphone_title,
        rationaleRes = R.string.core_permissions_microphone_rationale,
    ),
    NOTIFICATIONS(
        manifestPermission = Manifest.permission.POST_NOTIFICATIONS,
        runtimeSinceSdk = Build.VERSION_CODES.TIRAMISU,
        titleRes = R.string.core_permissions_notifications_title,
        rationaleRes = R.string.core_permissions_notifications_rationale,
    ),
    ACTIVITY_RECOGNITION(
        manifestPermission = Manifest.permission.ACTIVITY_RECOGNITION,
        runtimeSinceSdk = Build.VERSION_CODES.Q,
        titleRes = R.string.core_permissions_activity_title,
        rationaleRes = R.string.core_permissions_activity_rationale,
    ),
    ;

    /** The permission string to declare in the manifest. */
    val manifestName: String get() = manifestPermission

    /** Permissions to request at runtime on [sdkInt]; empty means nothing to ask. */
    fun runtimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= runtimeSinceSdk) listOf(manifestPermission) else emptyList()

    companion object {
        /** Maps a declared manifest permission back to its known capability, if any. */
        fun fromManifestName(name: String): AppPermission? = entries.firstOrNull { it.manifestPermission == name }
    }
}
