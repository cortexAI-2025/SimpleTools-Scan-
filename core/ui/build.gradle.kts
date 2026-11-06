plugins {
    alias(libs.plugins.simpletools.android.compose)
}

android {
    namespace = "com.simpletools.core.ui"
}

dependencies {
    api(projects.core.designsystem)
}
