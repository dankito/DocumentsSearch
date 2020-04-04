package net.dankito.documents.search.filesystem

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes


open class VisitedFile(
        val path: Path?,
        val attributes: BasicFileAttributes? = null,
        val error: Exception? = null
) {

    val visitSuccessful = path != null && error == null


    override fun toString(): String {
        return "${if (visitSuccessful) "Success" else "Error"} visiting file $path"
    }

}