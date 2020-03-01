package net.dankito.documents.search


interface IDocumentsSearcher {

	fun searchAsync(searchTerm: String, callback: (SearchResult) -> Unit)

}