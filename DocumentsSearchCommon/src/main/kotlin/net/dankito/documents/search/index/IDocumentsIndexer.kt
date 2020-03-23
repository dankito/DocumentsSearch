package net.dankito.documents.search.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.documents.search.model.Document


interface IDocumentsIndexer {

	suspend fun indexSuspendable(documentToIndex: Document) {
		withContext(Dispatchers.IO) {
			index(documentToIndex)
		}
	}

	fun index(documentToIndex: Document)

}