plugins {
    alias(libs.plugins.simpletools.android.feature)
}

android {
    namespace = "com.simpletools.feature.settings"
}

dependencies {
    implementation(projects.core.settings)
    implementation(projects.core.localization)
    implementation(projects.core.privacy)
}
