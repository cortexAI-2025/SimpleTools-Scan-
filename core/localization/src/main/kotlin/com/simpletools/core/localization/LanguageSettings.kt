package com.simpletools.core.localization

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.os.ConfigurationCompat

/**
 * Per-app language support without extra dependencies.
 *
 * On Android 13+, the build generates `locales_config.xml` and the user picks the app
 * language in the system settings (the official, consistent UX). Older versions simply
 * follow the device language. English remains the fallback for any unsupported locale.
 */
object LanguageSettings {

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isPerAppLanguageAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Language currently used by the UI. */
    fun currentLanguage(context: Context, supported: List<AppLanguage> = AppLanguage.entries): AppLanguage {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        val tags = (0 until locales.size()).mapNotNull { locales[it]?.toLanguageTag() }
        return AppLanguage.resolve(tags, supported)
    }

    /** Opens the system per-app language screen (Android 13+). */
    fun appLanguageSettingsIntent(context: Context): Intent? {
        if (!isPerAppLanguageAvailable) return null
        return Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
}
