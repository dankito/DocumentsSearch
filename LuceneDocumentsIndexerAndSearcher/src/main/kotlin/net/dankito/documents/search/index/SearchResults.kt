package net.dankito.documents.search.index

import org.apache.lucene.search.TopDocs


open class SearchResults(val totalHits: Long, val hits: List<SearchResult>, val topDocs: TopDocs) { // TODO: remove again?

	open val countRetrievedHits: Int = hits.size


	override fun toString(): String {
		return "$countRetrievedHits (of total $totalHits) hits"
	}

}