package net.dankito.documents.search

import kotlinx.coroutines.*
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata


interface IDocumentsSearcher {

	fun searchAsync(searchTerm: String): Deferred<SearchResult> = GlobalScope.async(Dispatchers.IO) {
		searchSuspendable(searchTerm)
	}

	suspend fun searchSuspendable(searchTerm: String): SearchResult {
		return withContext(Dispatchers.IO) {
			search(searchTerm)
		}
	}

	fun search(searchTerm: String): SearchResult


	suspend fun getDocumentSuspendable(metadata: DocumentMetadata): Document? {
		return withContext(Dispatchers.IO) {
			getDocument(metadata)
		}
	}

	fun getDocument(metadata: DocumentMetadata): Document?

}