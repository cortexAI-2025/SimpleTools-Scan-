package com.simpletools.feature.scanner

import android.content.Context
import com.simpletools.feature.scanner.data.ScanRepository
import com.simpletools.feature.scanner.data.ScanStorage
import com.simpletools.feature.scanner.export.PdfExporter
import com.simpletools.feature.scanner.image.ImageProcessor
import com.simpletools.feature.scanner.ocr.OcrEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * What the scanner screens need, created once by the app's AppContainer.
 *
 * [isProUnlocked] answers whether the PRO features (contrast adjustment, text recognition,
 * searchable PDF) may run. It reads the licence state; the screens never grant it themselves.
 */
class ScannerDependencies(
    val appContext: Context,
    val storage: ScanStorage,
    val repository: ScanRepository,
    val imageProcessor: ImageProcessor,
    val exporter: PdfExporter,
    val ocr: OcrEngine,
    private val proUnlocked: () -> Boolean,
) {
    fun isProUnlocked(): Boolean = proUnlocked()

    companion object {
        fun create(context: Context, proUnlocked: () -> Boolean = { false }): ScannerDependencies {
            val app = context.applicationContext
            val storage = ScanStorage(app.filesDir, app.cacheDir)
            return ScannerDependencies(
                appContext = app,
                storage = storage,
                repository = ScanRepository(storage, CoroutineScope(SupervisorJob())),
                imageProcessor = ImageProcessor(),
                exporter = PdfExporter(storage),
                ocr = OcrEngine(),
                proUnlocked = proUnlocked,
            )
        }
    }
}
