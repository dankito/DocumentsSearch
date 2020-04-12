package net.dankito.documents.search.index

import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata


interface IDocumentsIndexer {

	fun index(documentToIndex: Document)


	fun remove(document: DocumentMetadata) {
		remove(document.url)
	}

	fun remove(documentUrl: String)


	fun optimizeIndex()

}