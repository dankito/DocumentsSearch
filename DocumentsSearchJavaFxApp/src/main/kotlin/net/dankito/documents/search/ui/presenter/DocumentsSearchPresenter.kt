package net.dankito.documents.search.ui.presenter

import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.LuceneDocumentsSearcher
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.model.Cancellable
import net.dankito.utils.ThreadPool


open class DocumentsSearchPresenter {

	protected val threadPool = ThreadPool()

	protected val documentsSearcher: IDocumentsSearcher = LuceneDocumentsSearcher(threadPool)

	protected var lastSearchCancellable: Cancellable? = null


	open fun searchDocumentsAsync(searchTerm: String, callback: (SearchResult) -> Unit) {
		lastSearchCancellable?.cancel()

		lastSearchCancellable = documentsSearcher.searchAsync(searchTerm, callback)
	}

}