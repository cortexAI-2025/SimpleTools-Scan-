plugins {
    alias(libs.plugins.simpletools.android.feature)
}

android {
    namespace = "com.simpletools.feature.onboarding"
}

dependencies {
    implementation(projects.core.privacy)
}
