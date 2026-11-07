package com.simpletools.core.scanengine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A validated page of the document. File names are relative to the app's private scan
 * directory: [sourceFile] is the upright photo (kept to adjust the crop again later),
 * [pageFile] the corrected JPEG that goes into the PDF.
 */
@Serializable
data class ScanPage(
    val id: String,
    val sourceFile: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val pageFile: String,
    val pageWidth: Int,
    val pageHeight: Int,
    val quad: CropQuad,
    val rotation: Rotation = Rotation.None,
    /** Colour treatment of the page; documents saved before filters existed are [ScanFilter.Color]. */
    val filter: ScanFilter = ScanFilter.Color,
    /** Contrast adjustment in percent (see [PageContrast]); 0 for free documents. */
    val contrast: Int = 0,
)

/**
 * The page being cropped. It is part of the saved document so that an unfinished crop
 * survives the app being closed.
 *
 * @param replacesPageId set when retaking or re-cropping an existing page: saving replaces that
 *   page at the same position.
 * @param ownsSource true when [sourceFile] is a new photo that must be deleted if the draft is
 *   discarded; false when re-cropping the photo of an existing page.
 */
@Serializable
data class PageDraft(
    val id: String,
    val sourceFile: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val quad: CropQuad,
    val rotation: Rotation = Rotation.None,
    val replacesPageId: String? = null,
    val ownsSource: Boolean = true,
    val filter: ScanFilter = ScanFilter.Color,
    val contrast: Int = 0,
)

@Serializable
data class ScanDocument(
    val pages: List<ScanPage> = emptyList(),
    val draft: PageDraft? = null,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    val isEmpty: Boolean get() = pages.isEmpty() && draft == null

    fun indexOf(pageId: String): Int = pages.indexOfFirst { it.id == pageId }

    /** Moves the page at [from] to [to], shifting the pages in between. Out of range is a no-op. */
    fun movePage(from: Int, to: Int): ScanDocument {
        if (from !in pages.indices || to !in pages.indices || from == to) return this
        val list = pages.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(pages = list)
    }

    fun removePage(pageId: String): ScanDocument = copy(pages = pages.filterNot { it.id == pageId })

    /**
     * Saves [page] from the current draft: it replaces the page the draft was editing (same
     * position), or is appended at the end. The draft is cleared.
     */
    fun commit(page: ScanPage): ScanDocument {
        val target = draft?.replacesPageId
        val index = target?.let(::indexOf) ?: -1
        val list = pages.toMutableList()
        if (index >= 0) list[index] = page else list.add(page)
        return copy(pages = list, draft = null)
    }

    /** Files referenced by the document; anything else in the scan directory is a leftover. */
    fun referencedFiles(): Set<String> = buildSet {
        pages.forEach { add(it.sourceFile); add(it.pageFile) }
        draft?.let { add(it.sourceFile) }
    }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val SCHEMA_VERSION = 1

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** Returns null when [text] is not a readable document (corrupted or from a newer version). */
        fun decodeOrNull(text: String): ScanDocument? = runCatching {
            json.decodeFromString(serializer(), text)
        }.getOrNull()?.takeIf { it.schemaVersion <= SCHEMA_VERSION }
    }
}
