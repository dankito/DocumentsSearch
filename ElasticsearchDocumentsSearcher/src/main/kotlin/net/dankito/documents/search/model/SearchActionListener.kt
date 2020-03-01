package net.dankito.documents.search.model

import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.SearchResponse


open class SearchActionListener(
		protected val responseCallback: (Exception?, SearchResponse?) -> Unit
) : ActionListener<SearchResponse> {

	override fun onFailure(e: Exception?) {
		responseCallback(e, null)
	}

	override fun onResponse(response: SearchResponse?) {
		responseCallback(null, response)
	}

}