import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.simpletools.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.simpletools.android.application.get().pluginId
            implementationClass = "com.simpletools.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.simpletools.android.library.get().pluginId
            implementationClass = "com.simpletools.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.simpletools.android.compose.get().pluginId
            implementationClass = "com.simpletools.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.simpletools.android.feature.get().pluginId
            implementationClass = "com.simpletools.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.simpletools.jvm.library.get().pluginId
            implementationClass = "com.simpletools.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
