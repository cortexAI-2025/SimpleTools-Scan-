pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SimpleTools-Scan"

include(":app")

// Shared foundation
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:settings")
include(":core:permissions")
include(":core:privacy")
include(":core:localization")

// Optional integrations — interfaces + no-op defaults only, no third-party SDK.
include(":core:analytics")
include(":core:ads")
include(":core:billing")

// Reusable product-facing features
include(":feature:onboarding")
include(":feature:settings")
include(":feature:about")

// SimpleTools Scan
include(":core:scanengine") // pure Kotlin: geometry, document model, PDF writer
include(":feature:scanner") // capture, crop, pages, export
