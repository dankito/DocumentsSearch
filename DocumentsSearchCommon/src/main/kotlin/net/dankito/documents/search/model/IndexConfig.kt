package net.dankito.documents.search.model

import java.io.File


open class IndexConfig constructor(
        var name: String,
        var directoriesToIndex: List<File>,
        var includeRules: List<String> = listOf(),
        var excludeRules: List<String> = listOf(),
        var ignoreFilesLargerThanCountBytes: Long? = null,
        var ignoreFilesSmallerThanCountBytes: Long? = null
) {

    internal constructor() : this("", listOf()) // for object deserializers


    var id: String = ""

    val isIdSet: Boolean
        get() = id.isNotBlank()


    override fun toString(): String {
        return "$name with directoriesToIndex: $directoriesToIndex"
    }

}