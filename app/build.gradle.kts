plugins {
    alias(libs.plugins.simpletools.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // Kotlin package of the app code. The installed identity (applicationId) comes from
    // simpletools.properties, so derived products usually keep this namespace unchanged.
    namespace = "com.simpletools.app"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.settings)
    implementation(projects.core.privacy)
    implementation(projects.core.analytics)
    implementation(projects.core.ads)
    implementation(projects.core.billing)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(projects.feature.about)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
