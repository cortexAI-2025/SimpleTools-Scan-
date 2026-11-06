package com.simpletools.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class AppFactoryConfigTest {

    private val valid = mapOf(
        "config.schemaVersion" to "1",
        "app.name" to "SimpleTools PDF",
        "app.applicationId" to "com.simpletools.pdf",
        "app.versionCode" to "3",
        "app.versionName" to "1.0.2",
        "branding.accent" to "red",
        "languages.default" to "en",
        "languages.supported" to "en, fr",
        "features.onboarding" to "false",
        "monetization.ads" to "false",
        "monetization.premium" to "true",
        "privacy.localOnly" to "true",
        "privacy.analytics" to "false",
        "privacy.policyUrl" to "https://example.org/privacy",
        "permissions.declared" to "android.permission.CAMERA",
    )

    @Test
    fun `parses a valid configuration`() {
        val config = AppFactoryConfig.parse(valid)

        assertEquals("SimpleTools PDF", config.appName)
        assertEquals("com.simpletools.pdf", config.applicationId)
        assertEquals(3, config.versionCode)
        assertEquals("red", config.accent)
        assertEquals(listOf("en", "fr"), config.supportedLanguages)
        assertFalse(config.onboarding)
        assertTrue(config.premium)
        assertEquals(listOf("android.permission.CAMERA"), config.declaredPermissions)
    }

    @Test
    fun `applies privacy-first defaults for optional keys`() {
        val minimal = valid.filterKeys { it.startsWith("app.") || it == "config.schemaVersion" }

        val config = AppFactoryConfig.parse(minimal)

        assertEquals("blue", config.accent)
        assertEquals(listOf("en", "fr"), config.supportedLanguages)
        assertTrue(config.localOnly)
        assertFalse(config.analytics)
        assertFalse(config.ads)
        assertFalse(config.premium)
        assertTrue(config.declaredPermissions.isEmpty())
    }

    @Test
    fun `rejects unsupported languages and a non English default`() {
        val errors = errorsFor(valid + mapOf("languages.default" to "fr", "languages.supported" to "fr,de"))

        assertTrue(errors.any { "languages.default" in it })
        assertTrue(errors.any { "'de'" in it })
        assertTrue(errors.any { "must include 'en'" in it })
    }

    @Test
    fun `rejects network access and ads in a local only app`() {
        val errors = errorsFor(
            valid + mapOf(
                "permissions.declared" to "android.permission.INTERNET",
                "monetization.ads" to "true",
                "privacy.analytics" to "true",
            ),
        )

        assertEquals(3, errors.size)
    }

    @Test
    fun `reports unknown keys and malformed values together`() {
        val errors = errorsFor(
            valid + mapOf(
                "app.applicationid" to "typo",
                "app.applicationId" to "pdf",
                "app.versionCode" to "zero",
                "features.onboarding" to "yes",
                "branding.accent" to "pink",
                "privacy.policyUrl" to "http://insecure.example",
            ),
        )

        assertEquals(6, errors.size)
    }

    @Test
    fun `the template configuration file is valid`() {
        val file = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { it.resolve(AppFactoryConfig.FILE_NAME) }
            .first { it.isFile }

        val config = AppFactoryConfig.load(file)

        assertTrue(config.localOnly)
        assertEquals("en", config.defaultLanguage)
    }

    private fun errorsFor(values: Map<String, String>): List<String> = try {
        AppFactoryConfig.parse(values)
        fail("Expected an invalid configuration")
        emptyList()
    } catch (e: InvalidAppConfigException) {
        e.errors
    }
}
