plugins {
    alias(libs.plugins.simpletools.android.compose)
}

android {
    namespace = "com.simpletools.core.privacy"
}

dependencies {
    api(projects.core.common)
    api(projects.core.permissions)
    implementation(projects.core.ui)
}
