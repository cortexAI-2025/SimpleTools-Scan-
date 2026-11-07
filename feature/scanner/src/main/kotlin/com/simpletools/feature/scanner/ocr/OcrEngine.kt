package com.simpletools.feature.scanner.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.simpletools.core.scanengine.OcrLine
import com.simpletools.core.scanengine.OcrPage
import com.simpletools.core.scanengine.OcrWord
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Text recognition on the device: ML Kit with the Latin model bundled in the app (French and English).
 * No image or text leaves the phone; there is no download at run time.
 */
class OcrEngine {

    private var recognizer: TextRecognizer? = null

    private fun client(): TextRecognizer = recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .also { recognizer = it }

    /** Recognises the words of [bitmap]. Boxes are in the bitmap's pixels. Call from a background thread. */
    suspend fun recognize(bitmap: Bitmap): OcrPage {
        val text = suspendCancellableCoroutine { continuation ->
            client().process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        }
        return toPage(text, bitmap.width, bitmap.height)
    }

    fun close() {
        recognizer?.let { runCatching { it.close() } }
        recognizer = null
    }

    companion object {
        /** Maps ML Kit lines to OCR lines, dropping words without a box (they cannot be placed in a PDF). */
        internal fun toPage(text: Text, width: Int, height: Int): OcrPage {
            val lines = text.textBlocks.flatMap { it.lines }.map { line ->
                OcrLine(
                    line.elements.mapNotNull { element ->
                        val box = element.boundingBox ?: return@mapNotNull null
                        val word = element.text.trim()
                        if (word.isEmpty()) null else OcrWord(word, box.left, box.top, box.right, box.bottom)
                    },
                )
            }.filter { it.words.isNotEmpty() }
            return OcrPage(width = width, height = height, lines = lines)
        }
    }
}
