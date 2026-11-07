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

/**
 * Guards the promises of SimpleTools Scan: no account, no cloud, no ads, no tracking, offline.
 * The only purchase is the one-time PRO unlock through Google Play Billing (no subscription).
 */
@RunWith(AndroidJUnit4::class)
class TemplateConfigurationTest {

    @Test
    fun `app is local only, without ads or analytics, with the PRO purchase only`() {
        val config = buildConfigAppConfig()

        assertTrue(config.applicationId.startsWith("com.simpletools.scan"))
        assertTrue(config.privacy.localOnly)
        assertFalse(config.privacy.analytics)
        assertFalse(config.monetization.ads)
        assertTrue(config.monetization.premium)
        assertFalse(config.features.onboarding)
        assertEquals(listOf("en", "fr"), config.supportedLanguages)
    }

    @Test
    fun `only the camera and the Play purchase permission are requested, never internet`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val external = info.requestedPermissions.orEmpty().filterNot { it.startsWith(context.packageName) }.toSet()
        val expected = setOf("android.permission.CAMERA", "com.android.vending.BILLING")

        assertEquals(expected, external)
        assertFalse(external.contains("android.permission.INTERNET"))
        assertFalse(external.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertEquals(expected, buildConfigAppConfig().declaredPermissions.toSet())
    }
}
