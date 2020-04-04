package net.dankito.documents.search.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata


interface IDocumentsIndexer {

	suspend fun indexSuspendable(documentToIndex: Document) {
		withContext(Dispatchers.IO) {
			index(documentToIndex)
		}
	}

	fun index(documentToIndex: Document)


	fun remove(document: DocumentMetadata)


	fun optimizeIndex()

}