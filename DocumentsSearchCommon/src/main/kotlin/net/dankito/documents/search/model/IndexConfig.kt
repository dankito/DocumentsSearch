package net.dankito.documents.search.model


open class IndexConfig constructor(
        var name: String,
        var directoriesToIndex: List<IndexedDirectoryConfig>
) {

    internal constructor() : this("", listOf()) // for object deserializers


    var id: String = ""

    val isIdSet: Boolean
        get() = id.isNotBlank()


    override fun toString(): String {
        return "$name with directoriesToIndex: $directoriesToIndex"
    }

}