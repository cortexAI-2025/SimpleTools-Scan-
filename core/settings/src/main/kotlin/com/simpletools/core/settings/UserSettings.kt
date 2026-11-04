package com.simpletools.core.settings

/** Appearance chosen by the user. Stored by [storageKey] so enum renames never corrupt preferences. */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromStorageKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/** Settings shared by every SimpleTools app. Product specific preferences belong to the product module. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You wallpaper colors (Android 12+). Off by default to keep the product identity. */
    val dynamicColor: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

/** Whether the UI should be dark for this mode, given the current system appearance. */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
