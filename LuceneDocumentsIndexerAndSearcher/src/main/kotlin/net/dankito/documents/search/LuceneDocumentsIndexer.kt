package net.dankito.documents.search

import net.dankito.documents.language.DetectedLanguage
import net.dankito.documents.language.ILanguageDetector
import net.dankito.documents.search.config.DocumentFields.Companion.ChecksumFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContainingDirectoryFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContentFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.ContentTypeFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.CreatedAtFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.FilenameFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.IdFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.LastAccessedFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.LastModifiedFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataAuthorFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataSeriesFieldName
import net.dankito.documents.search.config.DocumentFields.Companion.MetadataTitleFieldName
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


	override fun index(documentToIndex: Document) {
		val detectedLanguage = detectedContentLanguage(documentToIndex)

		fieldLanguageBasedAnalyzer.setLanguageOfNextField(detectedLanguage)

		if (documentToIndex.language == null && detectedLanguage != DetectedLanguage.NotRecognized) {
			documentToIndex.language = detectedLanguage.name
		}

		metadataWriter.updateDocumentForNonNullFields(IdFieldName, documentToIndex.id,
			// searchable fields
			fields.fullTextSearchField(ContentFieldName, documentToIndex.content, false),
			fields.fullTextSearchField(FilenameFieldName, documentToIndex.filename.toLowerCase(), false),
			fields.nullableFullTextSearchField(ContainingDirectoryFieldName, documentToIndex.containingDirectory?.toLowerCase(), false),
			fields.nullableFullTextSearchField(MetadataTitleFieldName, documentToIndex.title, true),
			fields.nullableFullTextSearchField(MetadataAuthorFieldName, documentToIndex.author, true),
			fields.nullableFullTextSearchField(MetadataSeriesFieldName, documentToIndex.series, true),

			// stored fields
			fields.storedField(UrlFieldName, documentToIndex.url),
			fields.storedField(SizeFieldName, documentToIndex.fileSize),
			fields.storedField(ChecksumFieldName, documentToIndex.checksum),
			fields.storedField(CreatedAtFieldName, documentToIndex.createdAt),
			fields.storedField(LastAccessedFieldName, documentToIndex.lastAccessed),
			fields.storedField(LastModifiedFieldName, documentToIndex.lastModified),
			fields.nullableStoredField(ContentTypeFieldName, documentToIndex.contentType?.toLowerCase()),

			// fields for sorting
			fields.sortField(UrlFieldName, documentToIndex.url)
		)

		contentWriter.updateDocument(IdFieldName, documentToIndex.id,
			fields.storedField(ContentFieldName, documentToIndex.content)
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