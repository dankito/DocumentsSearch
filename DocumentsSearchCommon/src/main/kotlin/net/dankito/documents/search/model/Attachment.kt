package net.dankito.documents.search.model


open class Attachment(
        val name: String,
        val size: Int,
        val contentType: String,
        val content: String
) {

    override fun toString(): String {
        return name
    }

}