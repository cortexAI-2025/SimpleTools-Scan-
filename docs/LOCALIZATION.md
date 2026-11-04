# Localization (EN default, FR secondary)

* `res/values/strings.xml` — **English**, canonical, written first. Android falls back to it for any
  other locale.
* `res/values-fr/strings.xml` — **French** translation.
* No other language until a product decision. `languages.supported` can only contain `en` and `fr`,
  and the build strips any other language shipped by libraries (`localeFilters`).

## Adding a string

1. Add it to the module's `src/main/res/values/strings.xml`, prefixed with the module name to avoid
   collisions across modules:
   ```xml
   <string name="feature_pdfreader_open">Open a PDF</string>
   ```
2. Add the French translation to `src/main/res/values-fr/strings.xml`:
   ```xml
   <string name="feature_pdfreader_open">Ouvrir un PDF</string>
   ```
3. Use it from Compose: `stringResource(R.string.feature_pdfreader_open)`.
   Formatting: `<string name="x">Version %1$s</string>` + `stringResource(R.string.x, version)`.
   Plurals: `<plurals>` + `pluralStringResource`.

Never write UI text in Kotlin. Brand names and endonyms ("Français") use `translatable="false"`.

French typography: use the typographic apostrophe `’` and a space before `:`, `?`, `!`.

## What enforces it

* Lint `MissingTranslation` / `ExtraTranslation` are **errors** (`./gradlew :app:lintDebug`).
* `scripts/check-hardcoded-strings.sh` fails on literal UI text in Kotlin (CI).
* `LocalizationTest` (app) checks EN default, FR on French devices and EN fallback for others;
  feature tests can run in French with `@Config(qualifiers = "fr")`.

## App language

On Android 13+ the build generates `locales_config.xml` (`generateLocaleConfig`), so users can pick
the app language in system settings; the Settings screen links to it (`LanguageSettings`). Older
versions follow the device language. This avoids shipping AppCompat just for language switching.
