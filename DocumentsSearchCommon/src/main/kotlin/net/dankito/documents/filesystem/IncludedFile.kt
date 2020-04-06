package net.dankito.documents.filesystem

import net.dankito.utils.extensions.basicFileAttributes
import net.dankito.utils.filesystem.VisitedFile
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes


open class IncludedFile(
        val path: Path,
        val attributes: BasicFileAttributes? = path.basicFileAttributes
) {

    constructor(path: Path, visitedFile: VisitedFile) : this(path, visitedFile.attributes)


    override fun toString(): String {
        return "$path"
    }

}