package com.simpletools.core.localization

import androidx.annotation.StringRes

/**
 * Languages of the SimpleTools family. English is canonical (values/), French is
 * secondary (values-fr/). Adding a language is a product decision: add an entry here,
 * the `values-xx` resources everywhere, and allow it in build-logic (AppFactoryConfig).
 */
enum class AppLanguage(val tag: String, @StringRes val nativeNameRes: Int) {
    ENGLISH("en", R.string.core_localization_language_en),
    FRENCH("fr", R.string.core_localization_language_fr),
    ;

    companion object {
        val DEFAULT = ENGLISH

        fun fromTag(tag: String): AppLanguage? =
            entries.firstOrNull { it.tag.equals(tag.substringBefore('-').substringBefore('_'), ignoreCase = true) }

        /**
         * Language the UI is displayed in for the user's ordered locale preferences:
         * the first supported one, otherwise English (the resource fallback).
         */
        fun resolve(preferredTags: List<String>, supported: List<AppLanguage> = entries): AppLanguage =
            preferredTags.firstNotNullOfOrNull { tag -> fromTag(tag)?.takeIf { it in supported } } ?: DEFAULT
    }
}
