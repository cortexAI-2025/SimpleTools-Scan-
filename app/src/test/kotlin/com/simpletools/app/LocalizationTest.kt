package com.simpletools.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.simpletools.feature.scanner.R as ScanR

@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `english is the default language`() {
        assertEquals("Scan a page", context.getString(ScanR.string.scan_scan_page))
        assertEquals("3 pages", context.resources.getQuantityString(ScanR.plurals.scan_page_count, 3, 3))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `french is used on french devices`() {
        assertEquals("Numériser une page", context.getString(ScanR.string.scan_scan_page))
        assertEquals("Exporter en PDF", context.getString(ScanR.string.scan_export_pdf))
        assertEquals("1 page", context.resources.getQuantityString(ScanR.plurals.scan_page_count, 1, 1))
        assertEquals("Paramètres", context.getString(com.simpletools.feature.settings.R.string.feature_settings_title))
    }

    @Test
    @Config(qualifiers = "de")
    fun `unsupported languages fall back to english`() {
        assertEquals("Scan a page", context.getString(ScanR.string.scan_scan_page))
        assertEquals("Settings", context.getString(com.simpletools.feature.settings.R.string.feature_settings_title))
    }

    @Test
    fun `product name comes from simpletools properties and is not translated`() {
        assertEquals("SimpleTools Scan", context.getString(R.string.app_name))
    }
}
