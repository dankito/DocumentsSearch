package net.dankito.documents.search

import net.dankito.documents.search.config.DocumentFields
import net.dankito.documents.search.config.LuceneConfig
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.utils.lucene.search.FieldMapper
import net.dankito.utils.lucene.search.QueryBuilder
import net.dankito.utils.lucene.search.SearchResults
import net.dankito.utils.lucene.search.Searcher
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.slf4j.LoggerFactory
import java.io.File


open class LuceneDocumentsSearcher(
		protected val indexPath: File
) : IDocumentsSearcher, AutoCloseable {

	companion object {
		private val log = LoggerFactory.getLogger(LuceneDocumentsSearcher::class.java)
	}


	protected val metadataSearcher = Searcher(File(indexPath, LuceneConfig.MetadataDirectoryName))

	protected val contentSearcher = Searcher(File(indexPath, LuceneConfig.ContentDirectoryName))

	protected val mapper = FieldMapper()

	protected val queries = QueryBuilder()



	override fun close() {
		metadataSearcher.close()

		contentSearcher.close()
	}


	override fun search(searchTerm: String): SearchResult {
		try {
			val query = createDocumentsQuery(searchTerm)

			val searchResults = metadataSearcher.search(query,
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
				queries.contains(DocumentFields.FilenameFieldName, singleTerm),
				queries.startsWith(DocumentFields.ContainingDirectoryFieldName, singleTerm),
				queries.contains(DocumentFields.MetadataTitleFieldName, singleTerm),
				queries.contains(DocumentFields.MetadataAuthorFieldName, singleTerm),
				queries.contains(DocumentFields.MetadataSeriesFieldName, singleTerm)
			)
		}
	}


	override fun getDocument(metadata: DocumentMetadata): Document? {
		try {
			val searchResults = contentSearcher.search(TermQuery(Term(DocumentFields.IdFieldName, metadata.id)), 1)

			if (searchResults.hits.isNotEmpty()) {
				val doc = searchResults.hits[0].document

				val content = mapper.string(doc, DocumentFields.ContentFieldName)

				return Document(content, metadata)
			}
		} catch (e: Exception) {
			log.error("Could not get Document for metadata $metadata", e)
		}

		return null
	}


	override fun getAllDocumentMetadataForIndex(index: IndexConfig): List<DocumentMetadata> {
		try {
			val query = queries.allDocuments()

			val searchResults = metadataSearcher.search(query, 10_000_000)

			return mapSearchResults(searchResults)
		} catch (e: Exception) {
			log.error("Could not get all documents metadata for index '$index'", e)

			return listOf()
		}
	}


	protected open fun mapSearchResults(searchResults: SearchResults): List<DocumentMetadata> {
		return searchResults.hits.map {
			val doc = it.document

			DocumentMetadata(
					mapper.string(doc, DocumentFields.IdFieldName),
					mapper.string(doc, DocumentFields.UrlFieldName),
					mapper.long(doc, DocumentFields.SizeFieldName),
					mapper.string(doc, DocumentFields.ChecksumFieldName),
					mapper.date(doc, DocumentFields.CreatedAtFieldName),
					mapper.date(doc, DocumentFields.LastModifiedFieldName),
					mapper.date(doc, DocumentFields.LastAccessedFieldName),
					mapper.nullableString(doc, DocumentFields.ContentTypeFieldName),
					mapper.nullableString(doc, DocumentFields.MetadataTitleFieldName),
					mapper.nullableString(doc, DocumentFields.MetadataAuthorFieldName),
					series = mapper.nullableString(doc, DocumentFields.MetadataSeriesFieldName)
			)
		}
	}

}