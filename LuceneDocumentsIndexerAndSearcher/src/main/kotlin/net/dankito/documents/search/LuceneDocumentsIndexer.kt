package net.dankito.documents.search

import net.dankito.documents.language.DetectedLanguage
import net.dankito.documents.language.ILanguageDetector
import net.dankito.documents.search.config.DocumentFields
import net.dankito.documents.search.config.DocumentFields.Companion.ChecksumFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContainingDirectoryFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContentFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContentTypeFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.FilenameFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.IdFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.LastModifiedFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataAuthorFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataSeriesFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataTitleFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.RecipientFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.SizeFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.UrlFieldName
import net.dankito.documents.search.config.LuceneConfig.Companion.ContentDirectoryName
import net.dankito.documents.search.config.LuceneConfig.Companion.MetadataDirectoryName
import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.Document
import net.dankito.utils.lucene.index.DocumentsWriter
import net.dankito.utils.lucene.index.FieldBuilder
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import java.io.File


open class LuceneDocumentsIndexer(
		protected val indexPath: File,
		protected val languageDetector: ILanguageDetector
) : IDocumentsIndexer, AutoCloseable {

	protected val fieldLanguageBasedAnalyzer = FieldLanguageBasedAnalyzer()

	protected val analyzer: Analyzer = PerFieldAnalyzerWrapper(StandardAnalyzer(), mapOf(ContentFieldName to fieldLanguageBasedAnalyzer))

	protected val metadataWriter = DocumentsWriter(File(indexPath, MetadataDirectoryName), analyzer)

	protected val contentWriter = DocumentsWriter(File(indexPath, ContentDirectoryName), analyzer)

	protected val fields = FieldBuilder()



	override fun close() {
		metadataWriter.close()

		contentWriter.close()
	}


	override fun index(document: Document) {
		val detectedLanguage = detectedContentLanguage(document)

		fieldLanguageBasedAnalyzer.setLanguageOfNextField(detectedLanguage)

		if (document.language == null && detectedLanguage != DetectedLanguage.NotRecognized) {
			document.language = detectedLanguage.name
		}

		indexDocumentMetadata(document)

		indexDocumentContent(document)
	}

	private fun indexDocumentMetadata(document: Document) {

		metadataWriter.updateDocumentForNonNullFields(IdFieldName, document.id,
				listOf(
					document.recipients?.map { recipient ->
						listOf(
								fields.fullTextSearchField(RecipientFieldName, recipient, false)
						)
					},
					document.attachments?.map { attachment ->
						listOf(
							fields.fullTextSearchField(DocumentFields.AttachmentNameFieldName, attachment.name, false),
							fields.fullTextSearchField(DocumentFields.AttachmentContentTypeFieldName, attachment.contentType, false),
							fields.fullTextSearchField(DocumentFields.AttachmentContentFieldName, attachment.content, false)
						)
					}
				),

				// searchable fields
				fields.fullTextSearchField(ContentFieldName, document.content, false),
				fields.fullTextSearchField(FilenameFieldName, document.filename.toLowerCase(), false),
				fields.nullableFullTextSearchField(ContainingDirectoryFieldName, document.containingDirectory?.toLowerCase(), false),
				fields.nullableFullTextSearchField(MetadataTitleFieldName, document.title, true),
				fields.nullableFullTextSearchField(MetadataAuthorFieldName, document.author, true),
				fields.nullableFullTextSearchField(MetadataSeriesFieldName, document.series, true),

				// stored fields
				fields.storedField(UrlFieldName, document.url),
				fields.storedField(SizeFieldName, document.size),
				fields.storedField(ChecksumFieldName, document.checksum),
				fields.storedField(LastModifiedFieldName, document.lastModified),
				fields.nullableStoredField(ContentTypeFieldName, document.contentType?.toLowerCase()),

				// fields for sorting
				fields.sortField(UrlFieldName, document.url)
		)
	}

	private fun indexDocumentContent(document: Document) {
		contentWriter.updateDocumentForNonNullFields(IdFieldName, document.id,
			listOf(
				document.recipients?.map { recipient ->
					listOf(
						fields.storedField(RecipientFieldName, recipient)
					)
				},
				document.attachments?.map { attachment ->
					listOf(
						fields.storedField(DocumentFields.AttachmentNameFieldName, attachment.name),
						fields.storedField(DocumentFields.AttachmentSizeFieldName, attachment.size),
						fields.storedField(DocumentFields.AttachmentContentTypeFieldName, attachment.contentType),
						fields.storedField(DocumentFields.AttachmentContentFieldName, attachment.content)
					)
				}
			),
			fields.storedField(ContentFieldName, document.content)
		)
	}

	protected open fun detectedContentLanguage(documentToIndex: Document): DetectedLanguage {
		var detectedLanguage = languageDetector.detectLanguage(documentToIndex.content)

		if (detectedLanguage == DetectedLanguage.NotRecognized && documentToIndex.language != null) {
			try { detectedLanguage = DetectedLanguage.valueOf(documentToIndex.language!!) } catch (ignored: Exception) { }
		}

		return detectedLanguage
	}


	override fun remove(documentId: String) {
		metadataWriter.deleteDocument(IdFieldName, documentId)

		contentWriter.deleteDocument(IdFieldName, documentId)
	}


	override fun optimizeIndex() {
		// NOTE: if you want to maximize search performance,
		// you can optionally call forceMerge here.  This can be
		// a terribly costly operation, so generally it's only
		// worth it when your index is relatively static (ie
		// you're done adding documents to it):

		metadataWriter.optimizeIndex()

		contentWriter.optimizeIndex()
	}

}