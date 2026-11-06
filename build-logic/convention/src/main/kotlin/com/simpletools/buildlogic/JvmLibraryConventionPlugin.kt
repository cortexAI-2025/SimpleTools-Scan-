package com.simpletools.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin module (no Android dependency): fast to build, trivially unit-testable. */
class JvmLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
            }
        }

        addUnitTestDependencies()

        // Same entry point as Android modules, so `./gradlew testDebugUnitTest` covers every module.
        tasks.register("testDebugUnitTest") {
            group = "verification"
            description = "Runs the unit tests (alias of `test` for parity with Android modules)."
            dependsOn("test")
        }
    }
}
