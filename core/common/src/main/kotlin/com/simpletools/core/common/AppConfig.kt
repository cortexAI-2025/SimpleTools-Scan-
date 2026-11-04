package com.simpletools.core.common

/**
 * Runtime view of the product configuration (`simpletools.properties`).
 *
 * The app module builds it from the generated BuildConfig and passes it to the
 * modules that need it, so shared code never reads BuildConfig directly.
 */
data class AppConfig(
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val accent: String,
    val supportedLanguages: List<String>,
    val features: Features,
    val monetization: Monetization,
    val privacy: Privacy,
    val declaredPermissions: List<String>,
) {
    data class Features(
        val onboarding: Boolean,
    )

    data class Monetization(
        val ads: Boolean,
        val premium: Boolean,
    )

    data class Privacy(
        val localOnly: Boolean,
        val analytics: Boolean,
        /** Public privacy policy, or null when not configured yet. */
        val policyUrl: String?,
    )

    companion object {
        /** Splits the comma separated values generated in BuildConfig. */
        fun parseList(value: String): List<String> =
            value.split(',').map(String::trim).filter(String::isNotEmpty)
    }
}
