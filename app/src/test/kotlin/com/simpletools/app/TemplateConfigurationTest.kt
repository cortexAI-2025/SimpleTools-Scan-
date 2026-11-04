package com.simpletools.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the privacy-first defaults of the template build. */
@RunWith(AndroidJUnit4::class)
class TemplateConfigurationTest {

    @Test
    fun `template is local only, without ads, analytics or billing`() {
        val config = buildConfigAppConfig()

        assertTrue(config.privacy.localOnly)
        assertFalse(config.privacy.analytics)
        assertFalse(config.monetization.ads)
        assertFalse(config.monetization.premium)
        assertEquals(listOf("en", "fr"), config.supportedLanguages)
    }

    @Test
    fun `template requests no permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val external = info.requestedPermissions.orEmpty().filterNot { it.startsWith(context.packageName) }

        assertEquals(emptyList<String>(), external)
        assertTrue(buildConfigAppConfig().declaredPermissions.isEmpty())
    }
}
