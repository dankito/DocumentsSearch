package net.dankito.utils.lucene.search

import org.apache.lucene.document.Document


open class SearchResult(val score: Float, val document: Document) {

	override fun toString(): String {
		return "Score $score for $document"
	}

}