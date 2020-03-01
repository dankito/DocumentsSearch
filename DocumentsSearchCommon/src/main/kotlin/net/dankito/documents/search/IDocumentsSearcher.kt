package net.dankito.documents.search


interface IDocumentsSearcher {

	fun search(searchTerm: String): SearchResult

}