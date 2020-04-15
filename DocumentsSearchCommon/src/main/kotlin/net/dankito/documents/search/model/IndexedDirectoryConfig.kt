package net.dankito.documents.search.model

import java.io.File


open class IndexedDirectoryConfig(
        val directory: File,
        var includeRules: List<String> = listOf(),
        var excludeRules: List<String> = listOf(),
        var ignoreFilesLargerThanCountBytes: Long? = null,
        var ignoreFilesSmallerThanCountBytes: Long? = null

) : IndexPartConfig() {

    internal constructor() : this(File(".")) // for object deserializers


    override fun toString(): String {
        return directory.absolutePath
    }

}