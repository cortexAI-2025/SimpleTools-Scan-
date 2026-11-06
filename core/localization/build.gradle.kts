plugins {
    alias(libs.plugins.simpletools.android.library)
}

android {
    namespace = "com.simpletools.core.localization"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
