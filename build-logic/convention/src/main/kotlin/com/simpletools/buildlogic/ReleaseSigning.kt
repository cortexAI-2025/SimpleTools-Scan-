package com.simpletools.buildlogic

import org.gradle.api.Project

/**
 * Release signing credentials, read from environment variables (CI secrets) or
 * Gradle properties (`~/.gradle/gradle.properties`, never the repository).
 * Returns null when they are not all provided: the release build is then unsigned.
 */
internal data class ReleaseSigning(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
) {
    companion object {
        const val STORE_FILE = "SIMPLETOOLS_KEYSTORE_FILE"
        const val STORE_PASSWORD = "SIMPLETOOLS_KEYSTORE_PASSWORD"
        const val KEY_ALIAS = "SIMPLETOOLS_KEY_ALIAS"
        const val KEY_PASSWORD = "SIMPLETOOLS_KEY_PASSWORD"

        fun from(project: Project): ReleaseSigning? {
            fun read(name: String): String? = project.providers.environmentVariable(name)
                .orElse(project.providers.gradleProperty(name))
                .orNull
                ?.takeIf(String::isNotBlank)

            return ReleaseSigning(
                storeFile = read(STORE_FILE) ?: return null,
                storePassword = read(STORE_PASSWORD) ?: return null,
                keyAlias = read(KEY_ALIAS) ?: return null,
                keyPassword = read(KEY_PASSWORD) ?: return null,
            )
        }
    }
}
