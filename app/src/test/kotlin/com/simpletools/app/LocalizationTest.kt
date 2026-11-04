package com.simpletools.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `english is the default language`() {
        assertEquals("Your tool starts here", context.getString(R.string.home_placeholder_title))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `french is used on french devices`() {
        assertEquals("Votre outil commence ici", context.getString(R.string.home_placeholder_title))
        assertEquals("Paramètres", context.getString(com.simpletools.feature.settings.R.string.feature_settings_title))
    }

    @Test
    @Config(qualifiers = "de")
    fun `unsupported languages fall back to english`() {
        assertEquals("Your tool starts here", context.getString(R.string.home_placeholder_title))
        assertEquals("Settings", context.getString(com.simpletools.feature.settings.R.string.feature_settings_title))
    }

    @Test
    fun `product name comes from simpletools properties and is not translated`() {
        assertEquals("SimpleTools", context.getString(R.string.app_name))
    }
}
