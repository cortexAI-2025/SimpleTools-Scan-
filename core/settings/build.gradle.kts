plugins {
    alias(libs.plugins.simpletools.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences.core)
}
