plugins {
    alias(libs.plugins.simpletools.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

// Pure Kotlin core of SimpleTools Scan: crop geometry (perspective, rotation), the document
// model and a streaming JPEG → PDF writer. No Android dependency, tested with plain JUnit.
dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Test only: an independent PDF reader checks that generated files open and render.
    testImplementation(libs.pdfbox)
}
