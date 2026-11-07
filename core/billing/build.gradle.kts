plugins {
    alias(libs.plugins.simpletools.android.library)
}

// Licensing contract shared by all SimpleTools apps: the BillingRepository interface, the
// entitlement rules (pure Kotlin, tested) and the LicenseStore contract. The Google Play
// implementation lives in :core:billing-play, so a product without premium ships no billing SDK.
android {
    namespace = "com.simpletools.core.billing"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
