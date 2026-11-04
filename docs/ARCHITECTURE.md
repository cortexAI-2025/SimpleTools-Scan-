# Architecture

```text
app                      entry point: MainActivity, SimpleToolsApp, navigation, AppContainer (DI)
core/common        JVM   AppConfig (runtime product configuration)
core/designsystem  AND   SimpleToolsTheme (M3, light/dark, dynamic color, accents), spacing, buttons, icons
core/ui            AND   top bar, settings rows, empty/loading/error states, external links
core/settings      JVM   UserSettings + SettingsRepository (Preferences DataStore)
core/permissions   AND   AppPermission, PermissionStatus, PermissionGate / rememberPermissionController
core/privacy       AND   PrivacyDeclaration (derived from config) + PrivacySummary UI
core/localization  AND   AppLanguage (EN default, FR), per-app language settings
core/analytics     JVM   optional — interface + no-op
core/ads           JVM   optional — interface + no-op + AdPolicy
core/billing       JVM   optional — interface + no-op
feature/onboarding AND   single welcome screen (features.onboarding)
feature/settings   AND   theme, wallpaper colors, language, privacy screen
feature/about      AND   name, version, privacy policy link
build-logic              convention plugins + AppFactoryConfig + permission audit
```

Dependency direction: `app → feature → core`. Features never depend on each other or on `app`;
they expose `NavGraphBuilder.xxxScreen(...)` + `@Serializable` routes and receive dependencies as
parameters.

## Key choices

* **Convention plugins** (`simpletools.android.application|library|compose|feature`,
  `simpletools.jvm.library`) keep module build files to a few lines and SDK levels in one place
  (`gradle/libs.versions.toml`).
* **AGP 9 built-in Kotlin** for Android modules; Kotlin/JVM for pure modules.
* **Manual DI** (`AppContainer`) instead of Hilt: no annotation processing, faster builds, smaller app.
  Revisit only if a product grows beyond a handful of dependencies.
* **No AppCompat, no icon library, no splash library**: Compose + platform APIs cover the needs.
  Icons are vector drawables in `core:designsystem`.
* **Navigation Compose** with type-safe routes; a flat graph (main utility → settings → privacy/about).
* **DataStore** for preferences; Room only inside a product module that needs structured data.
* **Robolectric** Compose tests run as unit tests: CI needs no emulator.
* **Privacy by construction**: no `INTERNET` permission, `allowBackup=false`, permission audit at
  build time, config validation rejecting contradictions (local-only + ads/analytics/INTERNET).

## Versions

| | |
|---|---|
| AGP | 9.4.0 |
| Gradle | 9.8.0 |
| Kotlin | 2.4.20 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
