package com.simpletools.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Configures the `:app` module of a SimpleTools product from `simpletools.properties`:
 * identity, languages, feature flags, signing, shrinking, generated resources
 * and the permission audit.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val config = AppFactoryConfig.load(rootProject.file(AppFactoryConfig.FILE_NAME))
        val signing = ReleaseSigning.from(this)

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("compileSdk")

            defaultConfig {
                applicationId = config.applicationId
                minSdk = libs.versionInt("minSdk")
                targetSdk = libs.versionInt("targetSdk")
                versionCode = config.versionCode
                versionName = config.versionName
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                buildConfigField("String", "BRAND_ACCENT", config.accent.quoted())
                buildConfigField("String", "SUPPORTED_LANGUAGES", config.supportedLanguages.joinToString(",").quoted())
                buildConfigField("boolean", "FEATURE_ONBOARDING", config.onboarding.toString())
                buildConfigField("boolean", "MONETIZATION_ADS", config.ads.toString())
                buildConfigField("boolean", "MONETIZATION_PREMIUM", config.premium.toString())
                buildConfigField("boolean", "PRIVACY_LOCAL_ONLY", config.localOnly.toString())
                buildConfigField("boolean", "PRIVACY_ANALYTICS", config.analytics.toString())
                buildConfigField("String", "PRIVACY_POLICY_URL", config.privacyPolicyUrl.quoted())
                buildConfigField("String", "DECLARED_PERMISSIONS", config.declaredPermissions.joinToString(",").quoted())
            }

            androidResources {
                // Ship only the product languages (also strips extra translations from libraries).
                localeFilters += config.supportedLanguages
                // Generates locales_config.xml for the Android 13+ per-app language setting.
                generateLocaleConfig = true
            }

            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }

            buildFeatures {
                compose = true
                buildConfig = true
            }

            signingConfigs {
                if (signing != null) {
                    create("release") {
                        storeFile = file(signing.storeFile)
                        storePassword = signing.storePassword
                        keyAlias = signing.keyAlias
                        keyPassword = signing.keyPassword
                    }
                }
            }

            buildTypes {
                getByName("debug") {
                    applicationIdSuffix = ".debug"
                    versionNameSuffix = "-debug"
                }
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                    // Unsigned when no signing secrets are provided (see docs/RELEASE.md).
                    signingConfig = signingConfigs.findByName("release")
                }
            }

            packaging {
                resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
            }

            testOptions {
                unitTests.isIncludeAndroidResources = true
            }

            lint {
                configureSimpleToolsLint()
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val variantName = variant.name.capitalized()

                val generateResources = tasks.register<GenerateAppConfigResourcesTask>(
                    "generate${variantName}AppConfigResources",
                ) {
                    appName.set(config.appName)
                }
                variant.sources.res?.addGeneratedSourceDirectory(
                    generateResources,
                    GenerateAppConfigResourcesTask::outputDirectory,
                )

                val verifyPermissions = tasks.register<VerifyPermissionsTask>("verify${variantName}Permissions") {
                    group = "verification"
                    description = "Checks that the $variantName manifest requests only the permissions declared in ${AppFactoryConfig.FILE_NAME}."
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    declaredPermissions.set(config.declaredPermissions)
                    applicationId.set(variant.applicationId)
                    report.set(layout.buildDirectory.file("reports/permissions/${variant.name}.txt"))
                }
                tasks.named("check") { dependsOn(verifyPermissions) }
            }
        }

        addUnitTestDependencies()
    }

    private fun String.quoted(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
