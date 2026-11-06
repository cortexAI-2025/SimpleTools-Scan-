package com.simpletools.core.privacy

import com.simpletools.core.common.AppConfig
import com.simpletools.core.permissions.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyDeclarationTest {

    private fun config(permissions: List<String> = emptyList(), localOnly: Boolean = true) = AppConfig(
        applicationId = "com.simpletools.qr",
        versionName = "1.0",
        versionCode = 1,
        accent = "blue",
        supportedLanguages = listOf("en", "fr"),
        features = AppConfig.Features(onboarding = true),
        monetization = AppConfig.Monetization(ads = false, premium = false),
        privacy = AppConfig.Privacy(localOnly = localOnly, analytics = false, policyUrl = null),
        declaredPermissions = permissions,
    )

    @Test
    fun `template declaration is fully local`() {
        val declaration = PrivacyDeclaration.from(config())

        assertTrue(declaration.localOnly)
        assertFalse(declaration.requiresAccount)
        assertFalse(declaration.analytics)
        assertFalse(declaration.ads)
        assertTrue(declaration.permissions.isEmpty())
    }

    @Test
    fun `declared permissions are explained when known`() {
        val declaration = PrivacyDeclaration.from(
            config(permissions = listOf("android.permission.CAMERA", "android.permission.VIBRATE")),
        )

        assertEquals(listOf(AppPermission.CAMERA), declaration.permissions)
        assertEquals(listOf("android.permission.VIBRATE"), declaration.otherPermissions)
    }
}
