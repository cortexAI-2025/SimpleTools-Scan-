# App Factory: creating a new SimpleTools app

A SimpleTools product = **this template** + **`simpletools.properties`** + **its own feature module(s)**.
Shared infrastructure is never copied or rewritten.

## 1. `simpletools.properties` (schema v1)

The single file a product edits to define its identity. It is parsed and validated by
`build-logic/convention/.../AppFactoryConfig.kt`; any invalid value fails the build with the list of
problems (unknown keys included, to catch typos).

| Key | Default | Effect |
|-----|---------|--------|
| `config.schemaVersion` | required (`1`) | Version of this schema |
| `app.name` | required | Launcher label / product name (generated `@string/app_name`, not translated) |
| `app.applicationId` | required | Play Store identity, e.g. `com.simpletools.pdf` |
| `app.versionCode` / `app.versionName` | required | Release versions |
| `branding.accent` | `blue` | `blue`, `teal`, `green`, `orange`, `red`, `purple` → `SimpleToolsAccent` |
| `languages.default` | `en` | Must be `en` (English is canonical) |
| `languages.supported` | `en,fr` | Subset of `en,fr`; other languages are stripped from the APK (`localeFilters`) |
| `features.onboarding` | `true` | One welcome screen on first launch |
| `monetization.ads` | `false` | Requires `privacy.localOnly=false` and an ads provider module |
| `monetization.premium` | `false` | Requires a billing provider module |
| `privacy.localOnly` | `true` | Forbids `INTERNET`, analytics and ads |
| `privacy.analytics` | `false` | Requires `privacy.localOnly=false` |
| `privacy.policyUrl` | empty | `https://` URL shown in About/Privacy (required before Play release) |
| `permissions.declared` | empty | Exact list of manifest permissions (audited at build time) |

The values reach the code through `BuildConfig` → `AppConfig` (built once in `AppContainer`), so shared
modules never read `BuildConfig` directly. The YAML shown in the README maps 1:1 to these keys; a
properties file was chosen because Gradle reads it natively (no parser dependency) and it is trivial
to generate from a future bootstrap script.

## 2. Create the product repository

1. Create the new repository from a **tagged** version of this template (GitHub "Use this template",
   or clone + push to a new remote).
2. Edit `simpletools.properties`:
   ```properties
   app.name=SimpleTools PDF
   app.applicationId=com.simpletools.pdf
   app.versionCode=1
   app.versionName=1.0.0
   branding.accent=red
   features.onboarding=false
   ```
3. Replace the placeholder `app/.../home/HomeScreen.kt` by your feature module
   ([ADD_FEATURE_MODULE.md](ADD_FEATURE_MODULE.md)) and make it the start destination.
4. Remove the modules the product does not use from `settings.gradle.kts` and `app/build.gradle.kts`
   (e.g. `:feature:onboarding` if disabled — optional, R8 strips unused code anyway).
5. Run `./gradlew testDebugUnitTest :app:lintDebug :app:check :app:assembleDebug`.

## 3. Changing the package / applicationId

* **Installed identity** = `app.applicationId` in `simpletools.properties`. This is all a product
  needs to change. It must be unique on Google Play and **never change after the first release**.
* **Kotlin namespace** (`android.namespace = "com.simpletools.app"` in `app/build.gradle.kts`) is only
  the package of the generated `R`/`BuildConfig` classes and of the source files. Products can keep it.
  To rename it anyway: change `namespace`, move `app/src/main/kotlin/com/simpletools/app/` accordingly
  and update the `package` lines (Android Studio → Refactor → Rename package does both).
* Debug builds append `.debug` to the application ID.

## 4. Branding

| What | Where |
|------|-------|
| Name | `app.name` |
| Accent color (UI) | `branding.accent` |
| Launcher icon background | `app/src/main/res/values/colors.xml` → `ic_launcher_background` |
| Launcher glyph | `app/src/main/res/drawable/ic_launcher_foreground.xml` (vector, keep inside the 66dp safe zone; also used as the themed/monochrome icon) |
| Window background before first frame | `window_background` in `values/colors.xml` and `values-night/colors.xml` |

Need a color not offered by `branding.accent`? Add an entry to `SimpleToolsAccent`
(`core/designsystem/.../theme/Color.kt`) and to `AppFactoryConfig.ACCENTS`, so every product can reuse it.

## 5. Roadmap of the factory

Phase 2 can add a `bootstrap` script/workflow that takes the YAML from the README, writes
`simpletools.properties`, renames the namespace, and enables modules. The validation it relies on
already exists and is tested (`AppFactoryConfigTest`).
