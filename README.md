# SimpleTools Scan

> Photographiez vos pages, recadrez-les, réunissez-les en un PDF. Hors ligne, sans compte, sans publicité.
> *Photograph pages, crop them, combine them into one PDF. Offline, no account, no ads.*

Fifth app of the SimpleTools family, built on
[SimpleTools-Android-Template](https://github.com/cortexAI-2025/SimpleTools-Android-Template) (`5d36cb5`).

| | |
|---|---|
| Application ID | `com.simpletools.scan` (debug: `com.simpletools.scan.debug`) |
| Permissions | `CAMERA` only, asked when the user takes a photo. **No `INTERNET`.** |
| Languages | English (default), French |
| Storage | Photos, pages and the document in private app storage (`filesDir/scan`) |

## Scope (V1.3: FREE + PRO)

| FREE, free for life | PRO, one purchase of 3,99 € (no subscription) |
|---|---|
| Camera capture, photo import | Offline text recognition (OCR), French and English |
| Automatic page detection, perspective correction, rotation, manual corners | Copy and search the recognised text, TXT export |
| Colour, grey, black and white filters | Searchable PDF (invisible text layer aligned on the image) |
| Multi-page documents, reorder, retake, delete | Contrast adjustment per page |
| PDF export, save, share, *Open in SimpleTools PDF* | |
| Works offline, no account, no ads, no scan limit | |

Not in V1.3 (planned, not shipped): batch processing, scanning profiles, PDF compression settings, folders,
advanced search, automatic naming, local backup and restore, advanced sharpness, orientation detection.

| Permissions | Network |
|---|---|
| `CAMERA` (asked when a photo is taken), `com.android.vending.BILLING` (PRO purchase) | None. `INTERNET` and `ACCESS_NETWORK_STATE` are removed from the merged manifest. |

## Modules added to the template

- `core:scanengine` — pure Kotlin: crop geometry and homography, document model, edge detection
  (`EdgeDetector`), Otsu threshold and filters, OCR model and search, streaming JPEG→PDF writer with an
  optional invisible text layer.
- `feature:scanner` — capture, crop editor (detection, filters, PRO contrast), document, export, PRO text
  recognition (ML Kit, bundled model).
- `core:billing` — licence contract and entitlement rules (`Entitlement`), shared by SimpleTools apps.
- `core:billing-play` — Google Play Billing for the one-time product `simpletools_scan_pro_lifetime`.
- `feature:premium` — SimpleTools Scan PRO screen.

## Checks

```bash
./gradlew :build-logic:convention:test testDebugUnitTest :app:lintDebug \
  :app:verifyDebugPermissions :app:verifyReleasePermissions :app:assembleDebug :app:bundleRelease
# Emulator E2E (airplane mode), with a device/emulator connected:
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest && scripts/run-e2e.sh
```

Manual test on a real phone: [docs/TEST_GALAXY_A14.md](docs/TEST_GALAXY_A14.md).
Purchase set-up in the Play Console: [docs/PLAY_CONSOLE.md](docs/PLAY_CONSOLE.md).
Release notes (FR/EN): [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md).

---

# Template documentation


> A modular Android app factory built with Kotlin and Jetpack Compose for creating lightweight, fast, privacy-first utility applications.

## Vision

SimpleTools is not a single application. It is a reusable Android foundation designed to build a family of focused utility apps quickly and consistently.

The product philosophy is simple:

- One app = one clear everyday job.
- Fast launch and small application footprint.
- No mandatory account unless a feature genuinely requires one.
- Minimal permissions.
- Local-first processing whenever possible.
- Clean, accessible interface.
- No unnecessary features or dark patterns.
- Shared engineering foundation across the entire SimpleTools family.

Typical products derived from this repository may include:

- SimpleTools PDF — PDF reader and document utilities.
- SimpleTools Scan — document scanner.
- SimpleTools Steps — pedometer.
- SimpleTools Alarm — alarm clock, timer and stopwatch.
- SimpleTools QR — QR and barcode scanner.
- SimpleTools Recorder — voice recorder.
- SimpleTools Calculator — calculator and unit converter.
- SimpleTools Compass — compass and sensor tools.
- SimpleTools Level — spirit level.
- SimpleTools Images — image compression and basic image utilities.

## Quick start

```bash
git clone https://github.com/cortexAI-2025/SimpleTools-Android-Template.git
cd SimpleTools-Android-Template
./gradlew testDebugUnitTest :app:lintDebug :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:bundleRelease
# AAB: app/build/outputs/bundle/release/app-release.aab (unsigned without signing secrets)
```

Requirements: JDK 17+, Android SDK (platform 37). Gradle comes from the wrapper.

A new product is defined by one file, [`simpletools.properties`](simpletools.properties), plus its own
feature module(s). See the documentation:

| Topic | Document |
|-------|----------|
| Build, APK, AAB | [docs/BUILD.md](docs/BUILD.md) |
| Signing, GitHub secrets, Google Play | [docs/RELEASE.md](docs/RELEASE.md) |
| New app, applicationId/package, branding, configuration keys | [docs/APP_FACTORY.md](docs/APP_FACTORY.md) |
| Adding a product module | [docs/ADD_FEATURE_MODULE.md](docs/ADD_FEATURE_MODULE.md) |
| Adding an EN/FR string | [docs/LOCALIZATION.md](docs/LOCALIZATION.md) |
| Adding a permission | [docs/PERMISSIONS.md](docs/PERMISSIONS.md) |
| Enabling analytics / ads / billing | [docs/MONETIZATION.md](docs/MONETIZATION.md) |
| Modules and technical choices | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |

## Language strategy

**English is the primary and default application language. French is the secondary supported language.**

Android resources must follow this rule:

```text
res/
├── values/
│   └── strings.xml        # English — source/default language
└── values-fr/
    └── strings.xml        # French translation
```

All user-facing text must be stored in Android string resources. Do not hard-code UI copy in Kotlin/Compose code.

New features must be written in English first and then translated into French. If a French translation is missing, Android must safely fall back to English.

The initial SimpleTools product family supports **EN + FR only**. Additional languages should not be added until there is a product decision to support them.

## Technology

Target architecture:

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX
- Coroutines / Flow
- Navigation Compose
- DataStore for lightweight local preferences
- Room only where structured local persistence is required
- Gradle Kotlin DSL
- Version Catalog for dependency management
- GitHub Actions for automated validation and builds

Dependencies should remain minimal. A library must solve a concrete problem before being added.

## Architecture

The repository should evolve toward the following modular structure:

```text
SimpleTools-Android-Template/
├── app/
│   ├── navigation/
│   ├── MainActivity
│   └── SimpleToolsApp
│
├── core/
│   ├── common/
│   ├── designsystem/
│   ├── ui/
│   ├── permissions/
│   ├── settings/
│   ├── privacy/
│   ├── localization/
│   ├── analytics/
│   ├── ads/
│   └── billing/
│
├── feature/
│   ├── onboarding/
│   ├── settings/
│   ├── premium/        # planned: added with the first paid product
│   └── about/
│
├── build-logic/
├── gradle/
├── .github/
│   └── workflows/
└── README.md
```

### Implementation notes (v0.1)

What the first implementation settled:

- `core:common`, `core:settings`, `core:analytics`, `core:ads` and `core:billing` are **pure Kotlin
  (JVM) modules**; the others are Android libraries.
- `core:analytics`, `core:ads` and `core:billing` contain **interfaces and no-op implementations only**.
  No third-party SDK is shipped; real providers are added as separate modules by the products that
  need them ([docs/MONETIZATION.md](docs/MONETIZATION.md)).
- `feature:premium` is not created yet: it will be added with the first product that sells premium.
- Navigation lives in `app/navigation`; features expose type-safe routes and `NavGraphBuilder` extensions.
- Dependencies are wired manually in `AppContainer` (no DI framework).
- The App Factory configuration is `simpletools.properties` (validated at build time), the Gradle-native
  equivalent of the YAML example below.

### `app`

Application entry point. It assembles the modules enabled for a specific SimpleTools product and owns top-level navigation.

### `core:designsystem`

Shared visual language:

- Material 3 theme
- typography
- spacing
- reusable components
- icons
- light/dark mode
- accessibility conventions

Individual apps may have their own icon and accent identity while keeping the same SimpleTools UX principles.

### `core:ui`

Reusable Compose components that are not specific to one product.

### `core:permissions`

Centralized permission handling. Apps should request only permissions required by an active feature and should explain why a permission is needed before requesting sensitive access.

### `core:settings`

Shared preferences and settings infrastructure.

### `core:privacy`

Privacy-related components and policies. SimpleTools should prefer on-device processing and data minimization.

### `core:localization`

Localization conventions and helpers. English remains the canonical/default language; French is secondary.

### `core:analytics`

Optional analytics abstraction. Product functionality must not depend on analytics being enabled.

### `core:ads`

Optional advertising abstraction. Ads must not be required by the base template.

### `core:billing`

Optional Google Play Billing integration for premium features or ad removal.

### `feature:*`

Reusable product-facing features such as onboarding, settings, premium and about screens.

## Product modules

A generated application adds only the domain modules it needs.

Examples:

```text
SimpleTools PDF
base template
+ pdf-reader
+ pdf-search
+ recent-files
+ share

SimpleTools Steps
base template
+ step-counter
+ distance
+ history

SimpleTools Alarm
base template
+ alarm
+ timer
+ stopwatch
+ notifications

SimpleTools QR
base template
+ camera
+ qr-scanner
+ history
+ share
```

This separation is important: the PDF application should not ship pedometer code, and the pedometer should not ship PDF dependencies.

## App Factory concept

The long-term objective is to make this repository an **application factory**, not merely a starter project.

A product should eventually be describable with a small configuration file, for example:

```yaml
app:
  name: SimpleTools Steps
  package: com.simpletools.steps

languages:
  default: en
  supported:
    - en
    - fr

features:
  step_counter: true
  history: true
  notifications: false

monetization:
  ads: false
  premium: false

privacy:
  local_only: true
```

The factory can then use this configuration to bootstrap branding, package identifiers, enabled modules, language resources, settings and build configuration.

The exact configuration schema is expected to evolve. Do not treat the example above as a stable API until the factory implementation is versioned.

## Creating a new SimpleTools app

The intended workflow is:

1. Start from a tagged stable version of this template.
2. Create a dedicated repository for the new product.
3. Define the application name and unique Android application ID.
4. Configure branding and launcher icon.
5. Enable only the required shared modules.
6. Add the product-specific feature module(s).
7. Add English strings first.
8. Add the corresponding French translations.
9. Add only the Android permissions required by those features.
10. Run unit tests, lint and build checks.
11. Generate a debug APK for internal testing.
12. Generate a signed Android App Bundle (AAB) for Google Play release.

Apps derived from the template should remain independent products while sharing conventions and reusable infrastructure.

## UI/UX principles

SimpleTools applications should feel immediately understandable.

A user should normally reach the application's main utility directly after launch. Avoid unnecessary onboarding, dashboards and navigation layers.

Design priorities:

- primary action immediately visible
- minimal number of taps
- readable typography
- large touch targets
- dark mode support
- screen-reader-friendly semantics
- sensible dynamic font scaling
- clear empty/error/loading states
- no misleading buttons
- no forced registration
- no advertising that interferes with the core task

## Privacy and security

Privacy is part of the product architecture.

Default rules:

- Process data locally when practical.
- Collect no data merely because it can be collected.
- Request the minimum Android permissions.
- Never commit API keys, signing keys or credentials.
- Keep secrets outside source control.
- Do not upload documents, recordings, photos or sensor data without an explicit feature requirement and clear user action.
- Prefer platform APIs over invasive background services.
- Document any third-party SDK that collects or transmits data.

Each released application must have its own accurate privacy disclosures matching its actual behavior.

## Monetization

The template may support monetization, but monetization is optional.

Possible models:

- free application
- advertising-supported application
- one-time premium upgrade
- ad removal purchase
- premium feature unlock

The initial architecture should keep ads and billing behind interfaces/modules so that a completely free utility does not need to ship those dependencies.

Subscription models should be used only when the application provides recurring value that reasonably justifies them.

## Accessibility

Every SimpleTools app should target practical accessibility from the beginning:

- semantic labels for interactive controls
- sufficient contrast
- scalable text
- keyboard/accessibility navigation where relevant
- no information communicated exclusively through color
- descriptive labels for meaningful icons
- localization-safe layouts

## Performance

Utility apps should be lightweight.

Engineering decisions should prioritize:

- fast cold start
- low memory usage
- low battery impact
- minimal background work
- minimal dependency count
- no unnecessary network calls
- release shrinking/optimization where appropriate

Performance regressions should be treated as product regressions.

## Testing

The common template should progressively include:

- unit tests for business logic
- Compose UI tests for critical shared screens
- localization checks
- lint
- build validation
- permission-related tests where practical

Every derived app should additionally test its own core utility.

## CI/CD

GitHub Actions should ultimately validate every pull request and main-branch update.

Target pipeline:

```text
Pull Request
    ↓
Gradle validation
    ↓
Lint
    ↓
Unit tests
    ↓
Debug build
    ↓
APK artifact

Release / tag
    ↓
Release build
    ↓
Signed AAB
    ↓
Google Play release workflow
```

Signing credentials and Play publishing credentials must be stored as protected secrets and must never be committed to the repository.

## APK and AAB

Two Android artifacts are important:

- **APK** — useful for direct installation and internal testing.
- **AAB** — preferred release artifact for Google Play distribution.

Development builds should make APK generation easy. Production releases should generate a properly signed AAB.

## Repository strategy

Use this repository as the canonical engineering template/factory.

Recommended structure across GitHub:

```text
SimpleTools-Android-Template
├── → SimpleTools-PDF
├── → SimpleTools-Scanner
├── → SimpleTools-Steps
├── → SimpleTools-Alarm
├── → SimpleTools-QR
├── → SimpleTools-Recorder
├── → SimpleTools-Calculator
└── → SimpleTools-Compass
```

Do not turn the template into a monolithic application containing every utility. The shared foundation belongs here; each public utility should ultimately have its own repository and application ID.

## Initial roadmap

### Phase 1 — Foundation

- Kotlin/Compose Android project
- modular Gradle structure
- Material 3 design system
- navigation
- settings
- EN default localization
- FR secondary localization
- privacy foundation
- permission abstraction
- tests
- GitHub Actions

### Phase 2 — Factory

- app configuration model
- package/application ID customization
- branding configuration
- feature flags/modules
- generator/bootstrap workflow
- automatic validation

### Phase 3 — Pilot apps

Use three different utilities to prove that the architecture is genuinely reusable:

1. **SimpleTools PDF**
2. **SimpleTools QR**
3. **SimpleTools Steps**

PDF validates file/document workflows, QR validates camera/permission workflows, and Steps validates Android sensor/background behavior.

### Phase 4 — Monetization and release tooling

After the product foundation is stable:

- optional ads module
- optional billing module
- release signing
- Play Store metadata workflow
- automated AAB release pipeline

## Definition of done for the template

The template can be considered production-ready when a developer can create a new SimpleTools utility without copying and rewriting the common infrastructure.

At minimum:

- project builds from a clean checkout
- English works as the default language
- French localization works correctly
- shared theme/settings/navigation are reusable
- optional modules remain optional
- no secrets exist in source control
- CI passes
- debug APK can be produced
- release AAB path is documented and tested
- a new utility can be bootstrapped with minimal product-specific code

## First validation product

**SimpleTools PDF** is the recommended first derived application.

Its job should initially remain narrow:

> Open. Read. Search. Share.

V1 scope (agreed):

| Included in V1 | Excluded from V1 |
|----------------|------------------|
| Open (system file picker, no storage permission) | AI features |
| Read (fast page rendering) | Account / sign-in |
| Search in document text | Cloud storage or sync |
| Share | PDF conversion |
| Recent documents | Editing / annotation |
| Page navigation (go to page) | |
| Zoom | |
| Dark UI | |
| Remember last page per document | |
| Document information (title, pages, size…) | |

The goal of the first version is an extremely fast and lightweight PDF reader. Excluded items should not be added unless validated by actual user demand.

---

## Project status

**v0.1 — Phase 1 (Foundation) implemented.** Modular Kotlin/Compose project, Material 3 design system,
navigation, DataStore settings, EN/FR localization, privacy and permission foundations, optional
analytics/ads/billing abstractions, tests (JVM + Robolectric) and GitHub Actions (lint, tests, debug APK,
unsigned release AAB, signed AAB workflow on tags).

Phase 2 has started with `simpletools.properties`: identity, branding accent, languages, feature flags,
monetization and privacy flags, and an audited permission list. A bootstrap/generator script is the
next factory step, to be shaped by the first pilot app (SimpleTools PDF).

**Primary language:** English  
**Secondary language:** French  
**Platform:** Android  
**Core stack:** Kotlin + Jetpack Compose  
**Product philosophy:** lightweight, useful, privacy-first.
