package com.simpletools.core.permissions

/** What the UI should do for a permission. */
sealed interface PermissionStatus {
    data object Granted : PermissionStatus

    /** Not granted yet: explain why, then ask. */
    data object NotRequested : PermissionStatus

    /** The user refused once: explain again before asking. */
    data object Denied : PermissionStatus

    /** The system will not show the dialog anymore: offer to open the app settings. */
    data object PermanentlyDenied : PermissionStatus

    companion object {
        /**
         * Android does not expose "permanently denied" directly: `shouldShowRationale` is false
         * both before the first request and after a permanent refusal. Remembering whether we
         * already asked disambiguates the two cases.
         */
        fun resolve(granted: Boolean, shouldShowRationale: Boolean, hasRequested: Boolean): PermissionStatus = when {
            granted -> Granted
            shouldShowRationale -> Denied
            hasRequested -> PermanentlyDenied
            else -> NotRequested
        }
    }
}
