package com.simpletools.core.privacy

import com.simpletools.core.common.AppConfig
import com.simpletools.core.permissions.AppPermission

/**
 * What the product actually does with data, derived from its configuration so the
 * in-app privacy summary can never drift from the build.
 */
data class PrivacyDeclaration(
    val localOnly: Boolean,
    val analytics: Boolean,
    val ads: Boolean,
    /** Declared permissions with a known user-facing explanation. */
    val permissions: List<AppPermission>,
    /** Declared permissions without a known explanation (shown by name). */
    val otherPermissions: List<String>,
    val policyUrl: String?,
) {
    /** SimpleTools never requires an account in the template. */
    val requiresAccount: Boolean get() = false

    companion object {
        fun from(config: AppConfig): PrivacyDeclaration {
            val known = config.declaredPermissions.mapNotNull(AppPermission::fromManifestName)
            return PrivacyDeclaration(
                localOnly = config.privacy.localOnly,
                analytics = config.privacy.analytics,
                ads = config.monetization.ads,
                permissions = known,
                otherPermissions = config.declaredPermissions - known.map { it.manifestName }.toSet(),
                policyUrl = config.privacy.policyUrl,
            )
        }
    }
}
