package net.dankito.documents.search.model

import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.SearchResponse
import java.util.concurrent.CancellationException


open class SearchActionListener(
		protected val responseCallback: (Exception?, SearchResponse?) -> Unit
) : ActionListener<SearchResponse> {

	override fun onFailure(e: Exception) {
		if (e is CancellationException == false) { // don't return an error if we triggered cancellation
			responseCallback(e, null)
		}
	}

	override fun onResponse(response: SearchResponse) {
		responseCallback(null, response)
	}

}