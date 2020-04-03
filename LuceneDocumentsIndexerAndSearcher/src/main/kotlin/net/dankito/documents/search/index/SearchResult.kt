package net.dankito.documents.search.index

import org.apache.lucene.document.Document


open class SearchResult(val score: Float, val documentId: Int, val document: Document) { // TODO: remove again?

	override fun toString(): String {
		return "Score $score for $document"
	}

}