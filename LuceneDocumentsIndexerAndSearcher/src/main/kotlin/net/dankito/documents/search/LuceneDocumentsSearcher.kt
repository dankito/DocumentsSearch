package net.dankito.documents.search

import net.dankito.documents.search.config.DocumentFields
import net.dankito.documents.search.config.LuceneConfig
import net.dankito.documents.search.model.Attachment
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
				queries.contains(DocumentFields.MetadataSeriesFieldName, singleTerm),
				queries.contains(DocumentFields.RecipientFieldName, singleTerm),
				queries.contains(DocumentFields.AttachmentNameFieldName, singleTerm),
				queries.contains(DocumentFields.AttachmentContentTypeFieldName, singleTerm),
				queries.contains(DocumentFields.AttachmentContentFieldName, singleTerm)
			)
		}
	}


	override fun getDocument(metadata: DocumentMetadata): Document? {
		try {
			val searchResults = contentSearcher.search(TermQuery(Term(DocumentFields.IdFieldName, metadata.id)), 1)

			if (searchResults.hits.isNotEmpty()) {
				val result = searchResults.hits[0]

				val content = mapper.string(result, DocumentFields.ContentFieldName)

				val recipients = mapper.nullableStringList(result, DocumentFields.RecipientFieldName)

				val attachments = mapAttachments(result)

				return Document(content, metadata, recipients, attachments)
			}
		} catch (e: Exception) {
			log.error("Could not get Document for metadata $metadata", e)
		}

		return null
	}

	private fun mapAttachments(result: net.dankito.utils.lucene.search.SearchResult): List<Attachment>? {
		val attachmentNames = mapper.nullableStringList(result, DocumentFields.AttachmentNameFieldName)
		val attachmentSizes = mapper.nullableIntList(result, DocumentFields.AttachmentSizeFieldName)
		val attachmentContentTypes = mapper.nullableStringList(result, DocumentFields.AttachmentContentTypeFieldName)
		val attachmentContents = mapper.nullableStringList(result, DocumentFields.AttachmentContentFieldName)

		return attachmentNames?.mapIndexedNotNull { index, name ->
			val size = attachmentSizes?.get(index)
			val contentType = attachmentContentTypes?.get(index)
			val content = attachmentContents?.get(index)

			if (size != null && contentType != null && content != null) {
				Attachment(name, size, contentType, content)
			}
			else {
				null
			}
		}
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
		return searchResults.hits.map { result ->

			DocumentMetadata(
					mapper.string(result, DocumentFields.IdFieldName),
					mapper.string(result, DocumentFields.UrlFieldName),
					mapper.long(result, DocumentFields.SizeFieldName),
					mapper.string(result, DocumentFields.ChecksumFieldName),
					mapper.date(result, DocumentFields.LastModifiedFieldName),
					mapper.nullableString(result, DocumentFields.ContentTypeFieldName),
					mapper.nullableString(result, DocumentFields.MetadataTitleFieldName),
					mapper.nullableString(result, DocumentFields.MetadataAuthorFieldName),
					series = mapper.nullableString(result, DocumentFields.MetadataSeriesFieldName)
			)
		}
	}

}