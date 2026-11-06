package com.simpletools.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Accent identity of a SimpleTools product (`branding.accent` in simpletools.properties).
 * Every product shares the same neutral surfaces; only the accent changes.
 */
enum class SimpleToolsAccent(
    val key: String,
    internal val light: AccentTones,
    internal val dark: AccentTones,
) {
    Blue(
        key = "blue",
        light = AccentTones(Color(0xFF3457D5), Color(0xFFFFFFFF), Color(0xFFDCE1FF), Color(0xFF00164F)),
        dark = AccentTones(Color(0xFFB6C4FF), Color(0xFF00287D), Color(0xFF1A3EBA), Color(0xFFDCE1FF)),
    ),
    Teal(
        key = "teal",
        light = AccentTones(Color(0xFF006A6A), Color(0xFFFFFFFF), Color(0xFF6FF7F6), Color(0xFF002020)),
        dark = AccentTones(Color(0xFF4CDADA), Color(0xFF003737), Color(0xFF004F4F), Color(0xFF6FF7F6)),
    ),
    Green(
        key = "green",
        light = AccentTones(Color(0xFF2E6C33), Color(0xFFFFFFFF), Color(0xFFB1F2AD), Color(0xFF002106)),
        dark = AccentTones(Color(0xFF96D592), Color(0xFF00390E), Color(0xFF12531E), Color(0xFFB1F2AD)),
    ),
    Orange(
        key = "orange",
        light = AccentTones(Color(0xFF9A4600), Color(0xFFFFFFFF), Color(0xFFFFDBC8), Color(0xFF321300)),
        dark = AccentTones(Color(0xFFFFB68B), Color(0xFF532200), Color(0xFF763300), Color(0xFFFFDBC8)),
    ),
    Red(
        key = "red",
        light = AccentTones(Color(0xFFB3261E), Color(0xFFFFFFFF), Color(0xFFF9DEDC), Color(0xFF410E0B)),
        dark = AccentTones(Color(0xFFF2B8B5), Color(0xFF601410), Color(0xFF8C1D18), Color(0xFFF9DEDC)),
    ),
    Purple(
        key = "purple",
        light = AccentTones(Color(0xFF6750A4), Color(0xFFFFFFFF), Color(0xFFEADDFF), Color(0xFF21005D)),
        dark = AccentTones(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF)),
    ),
    ;

    companion object {
        fun fromKey(key: String): SimpleToolsAccent = entries.firstOrNull { it.key == key } ?: Blue
    }
}

internal data class AccentTones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

internal fun SimpleToolsAccent.colorScheme(darkTheme: Boolean): ColorScheme {
    val tones = if (darkTheme) dark else light
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = tones.primary,
        onPrimary = tones.onPrimary,
        primaryContainer = tones.primaryContainer,
        onPrimaryContainer = tones.onPrimaryContainer,
        secondary = tones.primary,
        onSecondary = tones.onPrimary,
        secondaryContainer = tones.primaryContainer,
        onSecondaryContainer = tones.onPrimaryContainer,
        inversePrimary = if (darkTheme) light.primary else dark.primary,
        surfaceTint = tones.primary,
    )
}
