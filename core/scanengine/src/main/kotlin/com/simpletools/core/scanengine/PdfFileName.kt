package com.simpletools.core.scanengine

/** Rules for the user-editable name of the exported PDF. */
object PdfFileName {

    const val EXTENSION = ".pdf"
    const val MAX_LENGTH = 80

    // Reserved on Android/FAT/Windows file systems, plus control characters.
    private val forbidden = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")
    private val spaces = Regex("\\s+")

    /** The name without extension, safe for every file system; empty when nothing usable remains. */
    fun sanitize(input: String): String {
        var name = input.trim()
        if (name.endsWith(EXTENSION, ignoreCase = true)) name = name.dropLast(EXTENSION.length)
        name = forbidden.replace(name, " ")
        name = spaces.replace(name, " ").trim().trim('.').trim()
        return name.take(MAX_LENGTH).trim()
    }

    fun isValid(input: String): Boolean = sanitize(input).isNotEmpty()

    /** "name.pdf" from what the user typed, or [fallback] when the input is unusable. */
    fun withExtension(input: String, fallback: String): String =
        (sanitize(input).ifEmpty { sanitize(fallback) }.ifEmpty { "scan" }) + EXTENSION
}
