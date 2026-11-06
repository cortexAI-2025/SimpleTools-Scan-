package com.simpletools.buildlogic

/**
 * Compares the permissions requested by the merged manifest with the ones a
 * product declares in `simpletools.properties`.
 *
 * Permissions scoped to the app itself (`<applicationId>.*`, e.g. the
 * `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` added by androidx.core) are
 * internal signature permissions and are ignored.
 */
object PermissionAudit {

    data class Result(val undeclared: List<String>, val unused: List<String>) {
        val isClean: Boolean get() = undeclared.isEmpty() && unused.isEmpty()
    }

    fun audit(requested: Collection<String>, declared: Collection<String>, applicationId: String): Result {
        val external = requested.filterNot { it.startsWith("$applicationId.") }.toSortedSet()
        val declaredSet = declared.toSortedSet()
        return Result(
            undeclared = (external - declaredSet).toList(),
            unused = (declaredSet - external).toList(),
        )
    }

    fun describe(result: Result): String = buildString {
        appendLine("Permission audit failed.")
        if (result.undeclared.isNotEmpty()) {
            appendLine("Requested by the merged manifest but not declared in permissions.declared:")
            result.undeclared.forEach { appendLine("  - $it") }
            appendLine("If a dependency added it unexpectedly, remove it with tools:node=\"remove\".")
        }
        if (result.unused.isNotEmpty()) {
            appendLine("Declared in permissions.declared but not requested by the manifest:")
            result.unused.forEach { appendLine("  - $it") }
        }
    }
}
