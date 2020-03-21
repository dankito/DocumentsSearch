package net.dankito.documents.search.model

import java.io.File


open class IndexConfig(
        var name: String,
        var directoriesToIndex: List<File>
) {

    internal constructor() : this("", listOf()) // for object deserializers


    override fun toString(): String {
        return "$name with directoriesToIndex: $directoriesToIndex"
    }

}