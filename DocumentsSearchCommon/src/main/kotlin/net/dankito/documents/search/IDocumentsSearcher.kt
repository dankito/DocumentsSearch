package net.dankito.documents.search

import kotlinx.coroutines.*
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig


interface IDocumentsSearcher {

	fun searchAsync(searchTerm: String): Deferred<SearchResult> = GlobalScope.async(Dispatchers.Default) {
		searchSuspendable(searchTerm)
	}

	suspend fun searchSuspendable(searchTerm: String): SearchResult {
		return withContext(Dispatchers.Default) {
			search(searchTerm)
		}
	}

	fun search(searchTerm: String): SearchResult


	fun getDocument(metadata: DocumentMetadata): Document?


	fun getAllDocumentMetadataForIndex(index: IndexConfig): List<DocumentMetadata>

}