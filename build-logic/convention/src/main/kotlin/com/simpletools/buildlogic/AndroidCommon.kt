package com.simpletools.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Lint policy shared by every Android module. Translation problems are errors. */
internal fun Lint.configureSimpleToolsLint() {
    abortOnError = true
    warningsAsErrors = false
    checkDependencies = true
    error += setOf("MissingTranslation", "ExtraTranslation", "MissingQuantity")
    // Dependency updates are handled deliberately through the version catalog.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
}

internal fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.lib("junit"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
    }
}
