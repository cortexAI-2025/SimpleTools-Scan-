# Permissions

Rule: **a permission is declared only when an active feature needs it, and requested only when the
user triggers that feature.** The template itself declares none (not even `INTERNET`).

## Adding a permission (example: camera for SimpleTools QR)

1. Declare it in `app/src/main/AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-feature android:name="android.hardware.camera.any" android:required="false" />
   ```
2. List it in `simpletools.properties`:
   ```properties
   permissions.declared=android.permission.CAMERA
   ```
   `./gradlew :app:check` (and CI) runs `verify<Variant>Permissions`, which fails when the merged
   manifest requests a permission that is not listed — including one added silently by a library
   (remove it with `tools:node="remove"`) — or when a listed permission is no longer requested.
3. Request it from the feature, around the UI that needs it:
   ```kotlin
   PermissionGate(permission = AppPermission.CAMERA, onDismiss = onNavigateBack) {
       CameraPreview()
   }
   ```
   `PermissionGate` explains why (localized rationale) before the system dialog, and offers
   "Open settings" if the user refused permanently. For custom flows use
   `rememberPermissionController(AppPermission.CAMERA)` (`status`, `request()`, `openAppSettings()`).
4. The Privacy screen lists the permission and its purpose automatically (`PrivacyDeclaration`).

## Supported capabilities (`core:permissions` → `AppPermission`)

| Capability | Manifest permission | Runtime from |
|------------|--------------------|--------------|
| `CAMERA` | `CAMERA` | always (API 23+) |
| `MICROPHONE` | `RECORD_AUDIO` | always |
| `NOTIFICATIONS` | `POST_NOTIFICATIONS` | Android 13 — nothing to ask before |
| `ACTIVITY_RECOGNITION` | `ACTIVITY_RECOGNITION` | Android 10 — nothing to ask before |

To support another one, add an entry with its title/rationale strings (EN + FR) and a test.

## SimpleTools Scan

| Permission | Origin | Why |
|------------|--------|-----|
| `CAMERA` | declared by the app | photo of a page |
| `com.android.vending.BILLING` | Google Play Billing (`core:billing-play`) | one-time PRO purchase and restore |
| `INTERNET`, `ACCESS_NETWORK_STATE` | **removed** (`tools:node="remove"`) | added by ML Kit's usage-logging transport; the app never uses the network |

The removal is deliberate: the build verifies the merged manifest, so these permissions cannot come back
silently. If a future dependency needs the network, the build fails and the change must be documented.

## Files and documents

Do not request storage permissions. Use the Storage Access Framework
(`rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`), which gives access to
the file the user picked, with no permission. Sharing uses `Intent.ACTION_SEND` + a `FileProvider`.
