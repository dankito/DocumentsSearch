package net.dankito.documents.search.model


abstract class SearchResult<T>(
		index: String,
		type: String,
		id: String,
		score: Float,
		val source: T
) : SearchResultWithoutSource(index, type, id, score) {


	override fun toString(): String {
		return "$score $source"
	}

}