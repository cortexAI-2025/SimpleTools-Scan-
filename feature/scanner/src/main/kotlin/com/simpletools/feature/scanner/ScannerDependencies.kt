package com.simpletools.feature.scanner

import android.content.Context
import com.simpletools.feature.scanner.data.ScanRepository
import com.simpletools.feature.scanner.data.ScanStorage
import com.simpletools.feature.scanner.export.PdfExporter
import com.simpletools.feature.scanner.image.ImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** What the scanner screens need, created once by the app's AppContainer. */
class ScannerDependencies(
    val appContext: Context,
    val storage: ScanStorage,
    val repository: ScanRepository,
    val imageProcessor: ImageProcessor,
    val exporter: PdfExporter,
) {
    companion object {
        fun create(context: Context): ScannerDependencies {
            val app = context.applicationContext
            val storage = ScanStorage(app.filesDir, app.cacheDir)
            return ScannerDependencies(
                appContext = app,
                storage = storage,
                repository = ScanRepository(storage, CoroutineScope(SupervisorJob())),
                imageProcessor = ImageProcessor(),
                exporter = PdfExporter(storage),
            )
        }
    }
}
