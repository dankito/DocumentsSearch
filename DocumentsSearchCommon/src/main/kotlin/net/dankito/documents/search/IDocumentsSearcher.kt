package net.dankito.documents.search

import kotlinx.coroutines.*


interface IDocumentsSearcher {

	fun searchAsync(searchTerm: String): Deferred<SearchResult> = GlobalScope.async(Dispatchers.IO) {
		searchSuspendable(searchTerm)
	}

	suspend fun searchSuspendable(searchTerm: String): SearchResult {
		return withContext(Dispatchers.IO) {
			return@withContext search(searchTerm)
		}
	}

	fun search(searchTerm: String): SearchResult

}