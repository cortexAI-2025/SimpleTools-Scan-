package com.simpletools.core.scanengine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

/**
 * A recognised word with its box, in pixels of the corrected page JPEG (origin top-left).
 * The boxes are what makes a searchable PDF possible: the text layer is placed where the word is.
 */
@Serializable
data class OcrWord(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

@Serializable
data class OcrLine(val words: List<OcrWord>) {
    val text: String get() = words.joinToString(" ") { it.text }
}

/** The text of one page. [width] and [height] are the size of the page JPEG the boxes refer to. */
@Serializable
data class OcrPage(
    val width: Int,
    val height: Int,
    val lines: List<OcrLine>,
) {
    val text: String get() = lines.joinToString("\n") { it.text }

    val isEmpty: Boolean get() = lines.all { it.words.isEmpty() }

    companion object {
        /** Stored as one JSON file per page; a file that cannot be read means "no text". */
        fun decodeOrNull(text: String): OcrPage? = runCatching {
            Json.decodeFromString(serializer(), text)
        }.getOrNull()

        fun encode(page: OcrPage): String = Json.encodeToString(serializer(), page)
    }
}

/** A search result: [pageIndex] is 0-based, [snippet] is the line where the match was found. */
data class OcrHit(val pageIndex: Int, val snippet: String)

object OcrSearch {

    /**
     * Lower case, without accents, with single spaces: "Émission  Été" matches "emission ete".
     * Used on both sides of a search so that French text is found without typing accents.
     */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Every line of every page containing [query] (all its words, in any order). */
    fun search(pages: List<OcrPage>, query: String): List<OcrHit> {
        val terms = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        val hits = mutableListOf<OcrHit>()
        pages.forEachIndexed { index, page ->
            page.lines.forEach { line ->
                val normalizedLine = normalize(line.text)
                if (terms.all { it in normalizedLine }) hits += OcrHit(index, line.text)
            }
        }
        return hits
    }

    /**
     * Plain text for the TXT export: pages in order, each page's lines one per row, pages separated by
     * a blank line. A page without recognised text is kept as an empty block so numbering stays right.
     */
    fun toPlainText(pages: List<OcrPage>): String = pages.joinToString("\n\n") { it.text }
}
