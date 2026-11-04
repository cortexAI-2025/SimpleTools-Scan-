package com.simpletools.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the promises of SimpleTools Scan: no account, no cloud, no ads, no tracking, offline. */
@RunWith(AndroidJUnit4::class)
class TemplateConfigurationTest {

    @Test
    fun `app is local only, without ads, analytics or billing`() {
        val config = buildConfigAppConfig()

        assertTrue(config.applicationId.startsWith("com.simpletools.scan"))
        assertTrue(config.privacy.localOnly)
        assertFalse(config.privacy.analytics)
        assertFalse(config.monetization.ads)
        assertFalse(config.monetization.premium)
        assertFalse(config.features.onboarding)
        assertEquals(listOf("en", "fr"), config.supportedLanguages)
    }

    @Test
    fun `only the camera permission is requested, never internet`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val external = info.requestedPermissions.orEmpty().filterNot { it.startsWith(context.packageName) }

        assertEquals(listOf("android.permission.CAMERA"), external)
        assertFalse(external.contains("android.permission.INTERNET"))
        assertEquals(listOf("android.permission.CAMERA"), buildConfigAppConfig().declaredPermissions)
    }
}
