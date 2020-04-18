package net.dankito.documents.search

import kotlinx.coroutines.CancellationException
import net.dankito.documents.search.model.*
import net.dankito.utils.serialization.JacksonJsonSerializer
import org.apache.http.HttpHost
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory


open class ElasticsearchDocumentsSearcher(
		elasticsearchHost: String,
		elasticsearchPort: Int,
		useHttps: Boolean = false
) : IDocumentsSearcher {

	companion object {
		private val log = LoggerFactory.getLogger(ElasticsearchDocumentsSearcher::class.java)
	}


	protected val deserializer = JacksonJsonSerializer()

	protected val client = RestHighLevelClient(RestClient.builder(
					HttpHost(elasticsearchHost, elasticsearchPort, if (useHttps) "https" else "http")))


	protected open fun requestOptions() = RequestOptions.DEFAULT


	override fun search(searchTerm: String): SearchResult {
		try {
			val searchRequest = createSearchRequest(searchTerm) // TODO: may set index to 'dokumente'

			val response = client.search(searchRequest, requestOptions())

			if (response.status() == RestStatus.OK) {
				val hits = getDocumentsFromSearchResponse(response)

				return SearchResult(true, null, hits)
			}
			else {
				return SearchResult(false, Exception("Search engine returned error code ${response.status()}")) // TODO: find better error message
			}
		} catch (e: Exception) {
			if (e is CancellationException == false) {
				log.error("Could not search for '$searchTerm'", e)
			}

			return SearchResult(false, e)
		}
	}


	protected open fun createSearchRequest(searchTerm: String): SearchRequest {
		val searchSourceBuilder = SearchSourceBuilder()

		if (searchTerm.isBlank()) {
			searchSourceBuilder.query(QueryBuilders.matchAllQuery())
		} else {
			searchSourceBuilder.query(QueryBuilders.termQuery("content", searchTerm))
			// TODO: add searching for filename, but this is indexed as keyword so we cannot do a 'contains' search -> change to text
//			searchSourceBuilder.query(QueryBuilders.wildcardQuery("file.filename", "*$searchTerm*"))
		}

		searchSourceBuilder.size(10000)

		val searchRequest = SearchRequest() // TODO: may set index to 'dokumente'
		searchRequest.source(searchSourceBuilder)
		return searchRequest
	}


	protected open fun getDocumentsFromSearchResponse(response: SearchResponse): List<DocumentMetadata> {
		val hits = deserializeDocumentSearchResults(response)

		return hits.map { mapToDocument(it) }
	}

	protected open fun mapToDocument(searchResult: DocumentSearchResult): DocumentMetadata {
		val source = searchResult.source
		val file = source.file

		val url = file.url.replace("file://", "")

		return DocumentMetadata(searchResult.id, url, file.filesize, "", // FsCrawler doesn't store checksum
				file.last_modified)
	}

	protected open fun deserializeDocumentSearchResults(response: SearchResponse): List<DocumentSearchResult> {
		val hits = response.hits.hits

		return hits.map { deserializeDocumentSearchResult(it) }
	}

	protected open fun deserializeDocumentSearchResult(hit: SearchHit): DocumentSearchResult {
		// TODO: source can theoretically be null. But can this happen as we (implicitly) tell Elasticsearch to return source?
		val source = deserializer.deserializeObject(hit.sourceAsString, SearchResultDocumentSource::class.java)

		return DocumentSearchResult(hit.index, hit.type, hit.id, hit.score, source)
	}


	override fun getDocument(metadata: DocumentMetadata): Document? {
		// TODO: may implement some day
		return null
	}


	override fun getAllDocumentMetadataForIndex(index: IndexConfig): List<DocumentMetadata> {
		// TODO: may implement some day
		return listOf()
	}

}