package net.dankito.documents.search

import net.dankito.documents.search.model.Document


open class SearchResult(
		val successful: Boolean,
		val error: Exception?,
		val hits: List<Document> = listOf()
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