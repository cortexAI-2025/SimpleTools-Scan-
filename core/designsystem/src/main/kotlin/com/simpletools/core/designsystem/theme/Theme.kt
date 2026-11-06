package com.simpletools.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Material You is available from Android 12. */
val isDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The SimpleTools theme: Material 3, light/dark, optional dynamic color, product accent.
 *
 * @param darkTheme resolved dark mode (system or user choice).
 * @param dynamicColor use wallpaper colors when the device supports it.
 * @param accent product identity used when dynamic color is off/unsupported.
 */
@Composable
fun SimpleToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accent: SimpleToolsAccent = SimpleToolsAccent.Blue,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && isDynamicColorSupported -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> accent.colorScheme(darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SimpleToolsTypography,
        content = content,
    )
}
