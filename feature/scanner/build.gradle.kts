plugins {
    alias(libs.plugins.simpletools.android.feature)
}

android {
    namespace = "com.simpletools.feature.scanner"
}

dependencies {
    api(projects.core.scanengine)
    implementation(projects.core.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    // Camera: CameraX, fully on-device. No network, no telemetry.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.androidx.test.core)
}
