package net.dankito.documents.search

import net.dankito.documents.search.model.Cancellable


interface IDocumentsSearcher {

	fun searchAsync(searchTerm: String, callback: (SearchResult) -> Unit): Cancellable

}