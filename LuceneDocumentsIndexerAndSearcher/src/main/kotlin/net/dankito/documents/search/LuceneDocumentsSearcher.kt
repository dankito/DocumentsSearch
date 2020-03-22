package net.dankito.documents.search

import net.dankito.documents.search.index.DocumentFields
import net.dankito.documents.search.index.FieldMapper
import net.dankito.documents.search.index.SearchResults
import net.dankito.documents.search.index.Searcher
import net.dankito.documents.search.model.Cancellable
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.SimpleCancellable
import net.dankito.utils.IThreadPool
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.io.File


open class LuceneDocumentsSearcher(
		protected val indexPath: File,
		protected val threadPool: IThreadPool
) : IDocumentsSearcher, AutoCloseable {

	companion object {
		private val log = LoggerFactory.getLogger(LuceneDocumentsSearcher::class.java)
	}


	protected val directory: Directory

	protected val analyzer: Analyzer

	protected val searcher = Searcher()

	protected val mapper = FieldMapper()



	init {
		directory = FSDirectory.open(indexPath.toPath())

		analyzer = StandardAnalyzer()
	}


	override fun close() {
		analyzer.close()

		directory.close()
	}


	override fun searchAsync(searchTerm: String, callback: (SearchResult) -> Unit): Cancellable {
		val cancellable = SimpleCancellable()

		threadPool.runAsync {
			// if SearchResult is null then search has been cancelled
			search(searchTerm, cancellable)?.let { searchResult ->
				callback(searchResult)
			}
		}

		return cancellable
	}

	protected open fun search(searchTerm: String, cancellable: SimpleCancellable): SearchResult? {
		try {
			val query = createDocumentsQuery(searchTerm)
			if (cancellable.isCancelled) return null

			val searchResults = searcher.search(directory, query)
			if (cancellable.isCancelled) return null

			val result = SearchResult(true, null, mapSearchResults(searchResults))
			if (cancellable.isCancelled) return null

			return result
		} catch (e: Exception) {
			log.error("Could not query for term '$searchTerm'", e)

			return SearchResult(false, e)
		}
	}


	protected open fun createDocumentsQuery(searchTerm: String): Query {
		val contentQuery = PhraseQuery(DocumentFields.ContentFieldName, searchTerm)
		val filenameQuery = WildcardQuery(Term(DocumentFields.FilenameFieldName, "*${searchTerm.toLowerCase()}*"))

		return BooleanQuery.Builder()
				.add(contentQuery, BooleanClause.Occur.SHOULD)
				.add(filenameQuery, BooleanClause.Occur.SHOULD)
				.build()
	}

	protected open fun mapSearchResults(searchResults: SearchResults): List<Document> {
		return searchResults.hits.map {
			val doc = it.document
			val url = mapper.string(doc, DocumentFields.UrlFieldName)

			Document(
				url,
				url,
				mapper.string(doc, DocumentFields.ContentFieldName),
				mapper.long(doc, DocumentFields.FileSizeFieldName),
				mapper.date(doc, DocumentFields.CreatedAtFieldName),
				mapper.date(doc, DocumentFields.LastModifiedFieldName),
				mapper.date(doc, DocumentFields.LastAccessedFieldName)
			)
		}
	}

}