plugins {
    alias(libs.plugins.simpletools.android.compose)
}

android {
    namespace = "com.simpletools.core.permissions"
}

dependencies {
    implementation(projects.core.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
