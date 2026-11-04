# Adding a product (domain) module

Example: the PDF reader of SimpleTools PDF.

## 1. Create the module

```
feature/pdfreader/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/simpletools/feature/pdfreader/
    │   ├── PdfReaderNavigation.kt
    │   ├── PdfReaderScreen.kt
    │   └── PdfReaderViewModel.kt
    ├── main/res/values/strings.xml      # English
    ├── main/res/values-fr/strings.xml   # French
    └── test/kotlin/...
```

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.simpletools.android.feature) // Compose, M3, navigation, ViewModel, design system
}

android {
    namespace = "com.simpletools.feature.pdfreader"
}

dependencies {
    implementation(projects.core.permissions) // only if needed
    // Product-specific libraries go here, never in core modules.
}
```

Pure logic with no Android dependency (parsers, calculators, converters) belongs in a
`simpletools.jvm.library` module: faster builds and plain JUnit tests.

Register it in `settings.gradle.kts`: `include(":feature:pdfreader")`.

## 2. Expose navigation

```kotlin
@Serializable
data object PdfReaderRoute

fun NavGraphBuilder.pdfReaderScreen(onOpenSettings: () -> Unit) {
    composable<PdfReaderRoute> { PdfReaderScreen(onOpenSettings = onOpenSettings) }
}
```

Routes are type-safe (`@Serializable`), so the module needs no knowledge of the app.

## 3. Wire it in the app

* `app/build.gradle.kts`: `implementation(projects.feature.pdfreader)`
* `navigation/SimpleToolsNavHost.kt`: call `pdfReaderScreen(onOpenSettings = navController::navigateToSettings)`
* `SimpleToolsApp.kt`: use `PdfReaderRoute` instead of `HomeRoute` as start destination,
  then delete the `home/` placeholder.

Keep the main utility reachable right after launch, with Settings as a top bar action.

## 4. Dependencies

Feature modules receive what they need as parameters (repositories, `AppConfig`) from `AppContainer`;
build ViewModels with `viewModelFactory { initializer { ... } }` as in `feature:settings`.
Product data that needs structured local storage may add Room **in that product module only**.

## 5. Checklist

- [ ] English strings in `values/`, French in `values-fr/` ([LOCALIZATION.md](LOCALIZATION.md))
- [ ] Permissions declared only if used ([PERMISSIONS.md](PERMISSIONS.md))
- [ ] Unit tests for the core utility logic
- [ ] `./gradlew testDebugUnitTest :app:lintDebug :app:check :app:assembleDebug`
