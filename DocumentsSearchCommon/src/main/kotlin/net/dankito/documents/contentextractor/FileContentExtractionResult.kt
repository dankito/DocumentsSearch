package net.dankito.documents.contentextractor


open class FileContentExtractionResult(
        val successful: Boolean,
        val error: Exception? = null,
        val content: String?,
        val contentType: String? = null,
        val title: String? = null,
        val author: String? = null,
        val length: Int? = null,
        val category: String? = null,
        val language: String? = null,
        val series: String? = null,
        val keywords: List<String> = listOf()
) {

    override fun toString(): String {
        if (error != null) {
            return "Error: $error"
        }

        return "Success: $content"
    }

}