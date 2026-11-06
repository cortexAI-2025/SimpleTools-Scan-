package com.simpletools.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Adds Jetpack Compose (Material 3) to an Android library module. */
class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("simpletools.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = platform(libs.lib("androidx-compose-bom"))
            add("implementation", bom)
            add("implementation", libs.lib("androidx-compose-ui"))
            add("implementation", libs.lib("androidx-compose-foundation"))
            add("implementation", libs.lib("androidx-compose-material3"))
            add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))

            // Compose UI tests run on the JVM with Robolectric (no emulator needed in CI).
            add("testImplementation", bom)
            add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.lib("androidx-test-ext-junit"))
            add("testImplementation", libs.lib("robolectric"))
            add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
        }
    }
}
