package com.simpletools.feature.scanner.data

import com.simpletools.core.scanengine.ScanDocument
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Files of the document being scanned, all in the app's private storage (`filesDir/scan`):
 * nothing is visible to other apps or included in backups (`allowBackup=false`).
 *
 * ```
 * scan/document.json     pages, order, crops and the unfinished page
 * scan/images/*.jpg      upright photos and corrected pages
 * cache/scan-exports/    the last exported PDF, shared through FileProvider
 * ```
 */
class ScanStorage(root: File, cacheDir: File) {

    private val scanDir = File(root, "scan")
    val imagesDir = File(scanDir, "images")
    val exportsDir = File(cacheDir, EXPORTS_DIR)
    private val documentFile = File(scanDir, "document.json")

    fun file(name: String): File = File(imagesDir, name)

    /** A new, unique file in [imagesDir]. Only its name is stored in the document. */
    fun newImageFile(kind: String): File {
        imagesDir.mkdirs()
        return File(imagesDir, "$kind-${UUID.randomUUID()}.jpg")
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** The saved document, or an empty one when there is none or it cannot be read. */
    fun readDocument(): ScanDocument {
        if (!documentFile.exists()) return ScanDocument()
        val text = runCatching { documentFile.readText() }.getOrNull() ?: return ScanDocument()
        val document = ScanDocument.decodeOrNull(text) ?: return ScanDocument()
        // Drop pages whose image has disappeared (e.g. storage cleared by the system).
        val pages = document.pages.filter { file(it.pageFile).exists() && file(it.sourceFile).exists() }
        val draft = document.draft?.takeIf { file(it.sourceFile).exists() }
        return document.copy(pages = pages, draft = draft)
    }

    /** Atomic: a crash while writing keeps the previous version. */
    @Throws(IOException::class)
    fun writeDocument(document: ScanDocument) {
        scanDir.mkdirs()
        val tmp = File(scanDir, "document.json.tmp")
        tmp.writeText(document.encode())
        if (!tmp.renameTo(documentFile)) {
            documentFile.delete()
            if (!tmp.renameTo(documentFile)) throw IOException("Could not save the document")
        }
    }

    fun delete(name: String?) {
        if (name != null) file(name).delete()
    }

    /**
     * Removes images no longer used by [document] (left over after a crash). Recent files are
     * kept: they may belong to a photo being processed right now.
     */
    fun deleteUnreferenced(document: ScanDocument, olderThanMillis: Long = System.currentTimeMillis() - LEFTOVER_AGE_MS) {
        val keep = document.referencedFiles()
        imagesDir.listFiles()?.forEach { file ->
            if (file.name !in keep && file.lastModified() < olderThanMillis) file.delete()
        }
    }

    fun deleteAll() {
        imagesDir.deleteRecursively()
        exportsDir.deleteRecursively()
    }

    companion object {
        const val EXPORTS_DIR = "scan-exports"
        private const val LEFTOVER_AGE_MS = 60 * 60 * 1000L
    }
}
