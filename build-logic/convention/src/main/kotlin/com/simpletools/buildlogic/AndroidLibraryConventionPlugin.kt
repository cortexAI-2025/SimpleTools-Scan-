package com.simpletools.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Base configuration for every Android library module (`core:*`, `feature:*`). */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.versionInt("compileSdk")

            defaultConfig {
                minSdk = libs.versionInt("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }

            testOptions {
                targetSdk = libs.versionInt("targetSdk")
                unitTests.isIncludeAndroidResources = true
            }

            lint {
                targetSdk = libs.versionInt("targetSdk")
                configureSimpleToolsLint()
            }
        }

        addUnitTestDependencies()
    }
}
