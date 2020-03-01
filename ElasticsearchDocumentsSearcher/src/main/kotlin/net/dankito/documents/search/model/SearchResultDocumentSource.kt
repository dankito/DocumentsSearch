package net.dankito.documents.search.model


open class SearchResultDocumentSource(
	val content: String,
	val file: SearchResultDocumentFileSource
) {

	internal constructor() : this("", SearchResultDocumentFileSource()) // for object deserializers


	override fun toString(): String {
		return "$file:\n$content"
	}

}