package net.dankito.documents.search.ui.presenter

import net.dankito.documents.search.ElasticsearchDocumentsSearcher
import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.SearchResult


open class DocumentsSearchPresenter {

	// TODO: make Elasticsearch settings configurable
	protected val documentsSearcher: IDocumentsSearcher = ElasticsearchDocumentsSearcher("192.168.178.32", 6200)


	open fun searchDocumentsAsync(searchTerm: String, callback: (SearchResult) -> Unit) {
		callback(documentsSearcher.search(searchTerm))
	}

}