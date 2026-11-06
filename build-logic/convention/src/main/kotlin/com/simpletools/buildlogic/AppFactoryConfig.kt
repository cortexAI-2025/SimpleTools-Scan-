package com.simpletools.buildlogic

import java.io.File
import java.util.Properties

/**
 * Typed view of `simpletools.properties`, the single file a derived SimpleTools
 * product edits to define its identity, languages, features, monetization and
 * privacy posture.
 *
 * Parsing is strict on purpose: unknown keys, malformed values and contradictory
 * settings (e.g. `privacy.localOnly=true` with the INTERNET permission) fail the
 * build with every problem listed at once, so a factory/generator can rely on it.
 *
 * This class has no Gradle or Android dependency so it can be unit tested and
 * reused by future tooling (bootstrap scripts, generators).
 */
data class AppFactoryConfig(
    val schemaVersion: Int,
    val appName: String,
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
    val accent: String,
    val defaultLanguage: String,
    val supportedLanguages: List<String>,
    val onboarding: Boolean,
    val ads: Boolean,
    val premium: Boolean,
    val localOnly: Boolean,
    val analytics: Boolean,
    val privacyPolicyUrl: String,
    val declaredPermissions: List<String>,
) {
    companion object {
        const val FILE_NAME = "simpletools.properties"
        const val SCHEMA_VERSION = 1

        /** EN is the canonical language, FR the secondary one. No other language until a product decision. */
        val SUPPORTED_LANGUAGES = listOf("en", "fr")
        const val DEFAULT_LANGUAGE = "en"

        val ACCENTS = listOf("blue", "teal", "green", "orange", "red", "purple")

        /** Permissions that imply network traffic, forbidden when `privacy.localOnly=true`. */
        val NETWORK_PERMISSIONS = setOf("android.permission.INTERNET")

        private val APPLICATION_ID = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        private val PERMISSION_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

        private val KNOWN_KEYS = setOf(
            "config.schemaVersion",
            "app.name",
            "app.applicationId",
            "app.versionCode",
            "app.versionName",
            "branding.accent",
            "languages.default",
            "languages.supported",
            "features.onboarding",
            "monetization.ads",
            "monetization.premium",
            "privacy.localOnly",
            "privacy.analytics",
            "privacy.policyUrl",
            "permissions.declared",
        )

        fun load(file: File): AppFactoryConfig {
            if (!file.isFile) {
                throw InvalidAppConfigException(listOf("Missing ${file.path}. Every SimpleTools app needs a $FILE_NAME file."))
            }
            val properties = Properties().apply { file.reader(Charsets.UTF_8).use(::load) }
            return parse(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
        }

        fun parse(values: Map<String, String>): AppFactoryConfig {
            val errors = mutableListOf<String>()
            val raw = values.mapValues { it.value.trim() }

            (raw.keys - KNOWN_KEYS).sorted().forEach { errors += "Unknown key '$it' (typo?)." }

            fun required(key: String): String {
                val value = raw[key]
                if (value.isNullOrEmpty()) errors += "'$key' is required."
                return value.orEmpty()
            }

            fun optional(key: String): String = raw[key].orEmpty()

            fun boolean(key: String, default: Boolean): Boolean = when (val value = raw[key]) {
                null, "" -> default
                "true" -> true
                "false" -> false
                else -> {
                    errors += "'$key' must be 'true' or 'false' (was '$value')."
                    default
                }
            }

            fun list(key: String): List<String> =
                optional(key).split(',').map(String::trim).filter(String::isNotEmpty)

            val schemaVersion = required("config.schemaVersion").toIntOrNull()
            if (schemaVersion != SCHEMA_VERSION) {
                errors += "'config.schemaVersion' must be $SCHEMA_VERSION."
            }

            val appName = required("app.name")

            val applicationId = required("app.applicationId")
            if (applicationId.isNotEmpty() && !APPLICATION_ID.matches(applicationId)) {
                errors += "'app.applicationId' must look like 'com.simpletools.pdf' (was '$applicationId')."
            }

            val versionCodeRaw = required("app.versionCode")
            val versionCode = versionCodeRaw.toIntOrNull()
            if (versionCodeRaw.isNotEmpty() && (versionCode == null || versionCode !in 1..2_100_000_000)) {
                errors += "'app.versionCode' must be an integer between 1 and 2100000000."
            }

            val versionName = required("app.versionName")

            val accent = optional("branding.accent").ifEmpty { ACCENTS.first() }
            if (accent !in ACCENTS) {
                errors += "'branding.accent' must be one of ${ACCENTS.joinToString()} (was '$accent')."
            }

            val defaultLanguage = optional("languages.default").ifEmpty { DEFAULT_LANGUAGE }
            if (defaultLanguage != DEFAULT_LANGUAGE) {
                errors += "'languages.default' must be '$DEFAULT_LANGUAGE': English is the canonical SimpleTools language."
            }

            val supportedLanguages = list("languages.supported").ifEmpty { SUPPORTED_LANGUAGES }
            (supportedLanguages - SUPPORTED_LANGUAGES.toSet()).forEach {
                errors += "Language '$it' is not supported yet (allowed: ${SUPPORTED_LANGUAGES.joinToString()})."
            }
            if (DEFAULT_LANGUAGE !in supportedLanguages) {
                errors += "'languages.supported' must include '$DEFAULT_LANGUAGE'."
            }

            val localOnly = boolean("privacy.localOnly", default = true)
            val analytics = boolean("privacy.analytics", default = false)
            if (localOnly && analytics) {
                errors += "'privacy.analytics=true' contradicts 'privacy.localOnly=true'."
            }

            val privacyPolicyUrl = optional("privacy.policyUrl")
            if (privacyPolicyUrl.isNotEmpty() && !privacyPolicyUrl.startsWith("https://")) {
                errors += "'privacy.policyUrl' must be an https:// URL."
            }

            val declaredPermissions = list("permissions.declared")
            declaredPermissions.filterNot(PERMISSION_NAME::matches).forEach {
                errors += "'$it' in 'permissions.declared' is not a valid permission name."
            }
            declaredPermissions.groupBy { it }.filterValues { it.size > 1 }.keys.forEach {
                errors += "Permission '$it' is declared twice."
            }
            if (localOnly) {
                declaredPermissions.filter { it in NETWORK_PERMISSIONS }.forEach {
                    errors += "'$it' contradicts 'privacy.localOnly=true'."
                }
            }

            val ads = boolean("monetization.ads", default = false)
            if (ads && localOnly) {
                errors += "'monetization.ads=true' requires network access and contradicts 'privacy.localOnly=true'."
            }

            val config = AppFactoryConfig(
                schemaVersion = schemaVersion ?: 0,
                appName = appName,
                applicationId = applicationId,
                versionCode = versionCode ?: 0,
                versionName = versionName,
                accent = accent,
                defaultLanguage = defaultLanguage,
                supportedLanguages = supportedLanguages.distinct(),
                onboarding = boolean("features.onboarding", default = true),
                ads = ads,
                premium = boolean("monetization.premium", default = false),
                localOnly = localOnly,
                analytics = analytics,
                privacyPolicyUrl = privacyPolicyUrl,
                declaredPermissions = declaredPermissions.distinct(),
            )
            if (errors.isNotEmpty()) throw InvalidAppConfigException(errors)
            return config
        }
    }
}

class InvalidAppConfigException(val errors: List<String>) : IllegalArgumentException(
    "Invalid ${AppFactoryConfig.FILE_NAME}:\n" + errors.joinToString("\n") { "  - $it" },
)
