package com.simpletools.feature.scanner.export

import com.simpletools.core.scanengine.JpegInfo
import com.simpletools.core.scanengine.JpegPdfWriter
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.data.ScanStorage
import java.io.IOException
import java.io.OutputStream

/**
 * Writes the pages to a PDF, one page at a time: each corrected JPEG is streamed from disk into
 * the file, so a long document of large photos needs no more memory than a short one.
 * The pages themselves are only read, so a failed export never damages the document.
 */
class PdfExporter(private val storage: ScanStorage) {

    @Throws(IOException::class)
    fun write(pages: List<ScanPage>, title: String, output: OutputStream, onPageWritten: (Int) -> Unit = {}) {
        if (pages.isEmpty()) throw IOException("No page to export")
        JpegPdfWriter(output, title).use { writer ->
            pages.forEachIndexed { index, page ->
                val file = storage.file(page.pageFile)
                if (!file.exists()) throw IOException("Page ${index + 1} is missing")
                val info = file.inputStream().use(JpegInfo::read)
                file.inputStream().use { writer.addPage(it, file.length(), info) }
                onPageWritten(index + 1)
            }
            writer.finish()
        }
        output.flush()
    }
}
