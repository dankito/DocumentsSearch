package net.dankito.documents.search.model


open class IndexConfig(
        var name: String,
        var indexParts: List<IndexPartConfig>
) {

    internal constructor() : this("", listOf()) // for object deserializers


    var id: String = ""

    val isIdSet: Boolean
        get() = id.isNotBlank()


    override fun toString(): String {
        return "$name with directoriesToIndex: $indexParts"
    }

}