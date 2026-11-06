package com.simpletools.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A product-facing feature: Compose UI + type-safe navigation + ViewModel,
 * built on the shared design system. Used by `feature:*` and product domain modules.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("simpletools.android.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-serialization-core"))
        }
    }
}
