package net.dankito.documents.search

import net.dankito.documents.search.model.DocumentMetadata


open class SearchResult(
		val successful: Boolean,
		val error: Exception?,
		val hits: List<DocumentMetadata> = listOf()
) {


	open val hasError: Boolean
		get() = error != null


	override fun toString(): String {
		if (successful) {
			return "Successful: ${hits.size} hits"
		}
		else {
			return "Error: $error"
		}
	}

}