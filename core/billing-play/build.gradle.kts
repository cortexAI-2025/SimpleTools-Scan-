plugins {
    alias(libs.plugins.simpletools.android.library)
}

// Google Play Billing implementation of :core:billing. Reusable by any SimpleTools app:
// only the product id and the LicenseStore are provided by the app.
android {
    namespace = "com.simpletools.core.billing.play"
}

dependencies {
    api(projects.core.billing)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.coroutines.core)
}
