# Building the template

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 or newer (CI uses Temurin 17) |
| Android SDK | Platform 37 (`compileSdk`), Build-Tools 36 — Android Studio or the Gradle plugin installs them |
| Gradle | Provided by the wrapper (`./gradlew`, Gradle 9.8.0) — do not install Gradle manually |
| Android Studio | A recent stable release supporting AGP 9.4 |

Point Gradle to the SDK with `ANDROID_HOME` or a `local.properties` file (never committed):

```properties
sdk.dir=/path/to/Android/sdk
```

## Everyday commands

```bash
./gradlew testDebugUnitTest          # unit tests of every module (JVM + Robolectric, no emulator)
./gradlew :build-logic:convention:test  # App Factory configuration tests
./gradlew :app:lintDebug             # Android Lint for the app and all its modules
./gradlew :app:check                 # includes the permission audit
./scripts/check-hardcoded-strings.sh # no UI text in Kotlin
./scripts/check-no-secrets.sh        # no signing material tracked by git
```

## Debug APK

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device: `./gradlew :app:installDebug` (or `adb install -r app/build/outputs/apk/debug/app-debug.apk`).

The debug build uses the application ID suffix `.debug`, so it can be installed next to the Play Store version.

CI uploads this APK as the `simpletools-debug-apk` artifact on every pull request and push to `main`.

## Release AAB (Google Play)

```bash
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Release builds are minified and resource-shrunk with R8. Without signing credentials the bundle is
**unsigned** (CI builds it this way on every PR to prove the release path works). To produce a signed
bundle, provide the four signing values described in [RELEASE.md](RELEASE.md).

A release APK (`./gradlew :app:assembleRelease`) is also possible but Google Play expects the AAB.
