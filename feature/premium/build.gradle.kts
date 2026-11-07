plugins {
    alias(libs.plugins.simpletools.android.feature)
}

android {
    namespace = "com.simpletools.feature.premium"
}

dependencies {
    implementation(projects.core.billing)
}
