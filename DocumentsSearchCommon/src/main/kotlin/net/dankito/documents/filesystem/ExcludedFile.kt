package net.dankito.documents.filesystem

import java.nio.file.Path


open class ExcludedFile(
        val path: Path,
        val reason: ExcludeReason
) {

    override fun toString(): String {
        return "$reason: $path"
    }

}