package net.dankito.documents.search

import net.dankito.documents.search.index.*
import net.dankito.documents.search.model.Document
import net.dankito.utils.IThreadPool
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.io.File


open class LuceneDocumentsSearcher(
		protected val indexPath: File
) : IDocumentsSearcher, AutoCloseable {

	companion object {
		private val log = LoggerFactory.getLogger(LuceneDocumentsSearcher::class.java)
	}


	protected val directory: Directory

	protected val analyzer: Analyzer

	protected val searcher = Searcher()

	protected val mapper = FieldMapper()

	protected val queries = QueryBuilder()



	init {
		directory = FSDirectory.open(indexPath.toPath())

		analyzer = StandardAnalyzer()
	}


	override fun close() {
		analyzer.close()

		directory.close()
	}


	override fun search(searchTerm: String): SearchResult {
		try {
			val query = createDocumentsQuery(searchTerm)

			val searchResults = searcher.search(directory, query,
					sortFields = listOf(SortField(DocumentFields.UrlFieldName, SortField.Type.STRING)))

			return SearchResult(true, null, mapSearchResults(searchResults))
		} catch (e: Exception) {
			log.error("Could not query for term '$searchTerm'", e)

			return SearchResult(false, e)
		}
	}


	protected open fun createDocumentsQuery(searchTerm: String): Query {
		return queries.createQueriesForSingleTerms(searchTerm) { singleTerm ->
			listOf(
				queries.fulltextQuery(DocumentFields.ContentFieldName, singleTerm),
				queries.contains(DocumentFields.FilenameFieldName, singleTerm)
			)
		}
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