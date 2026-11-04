package com.simpletools.core.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `english is the default language`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.DEFAULT)
        assertEquals("en", AppLanguage.entries.first().tag)
    }

    @Test
    fun `regional tags map to their language`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr-CA"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr_BE"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertNull(AppLanguage.fromTag("de-DE"))
    }

    @Test
    fun `first supported preferred language wins`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.resolve(listOf("de-DE", "fr-FR", "en-US")))
    }

    @Test
    fun `unsupported locales fall back to english`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(listOf("de-DE", "es-ES")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(emptyList()))
    }

    @Test
    fun `a language not enabled by the product is ignored`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(listOf("fr-FR"), supported = listOf(AppLanguage.ENGLISH)))
    }
}
