package net.dankito.documents.search.model


open class DocumentSearchResult(
		index: String,
		type: String,
		id: String,
		score: Float,
		source: SearchResultDocumentSource
) : SearchResult<SearchResultDocumentSource>(index, type, id, score, source) {

	internal constructor() : this("", "", "", Float.NaN, SearchResultDocumentSource())

}