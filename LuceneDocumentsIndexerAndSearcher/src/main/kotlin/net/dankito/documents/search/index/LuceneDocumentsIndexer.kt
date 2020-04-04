package net.dankito.documents.search.index

import net.dankito.documents.language.DetectedLanguage
import net.dankito.documents.language.ILanguageDetector
import net.dankito.documents.search.LuceneConfig.Companion.ContentDirectoryName
import net.dankito.documents.search.LuceneConfig.Companion.MetadataDirectoryName
import net.dankito.documents.search.index.DocumentFields.Companion.ContainingDirectoryFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.ContentFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.ContentTypeFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.CreatedAtFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FileSizeFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FilenameFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastAccessedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastModifiedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.MetadataAuthorFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.MetadataSeriesFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.MetadataTitleFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.UrlFieldName
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.File


open class LuceneDocumentsIndexer(
		protected val indexPath: File,
		protected val languageDetector: ILanguageDetector
) : IDocumentsIndexer, AutoCloseable {

	protected val fieldLanguageBasedAnalyzer = FieldLanguageBasedAnalyzer()

	protected val analyzer: Analyzer

	protected val metadataDirectory: Directory
	protected val metadataWriter: IndexWriter

	protected val contentDirectory: Directory
	protected val contentWriter: IndexWriter

	protected val documents = DocumentsWriter()
	protected val fields = FieldBuilder()



	init {
		analyzer = PerFieldAnalyzerWrapper(StandardAnalyzer(), mapOf(ContentFieldName to fieldLanguageBasedAnalyzer))

		val metadataWriterConfig = IndexWriterConfig(analyzer)
		metadataWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND

		metadataDirectory = FSDirectory.open(File(indexPath, MetadataDirectoryName).toPath())
		metadataWriter = IndexWriter(metadataDirectory, metadataWriterConfig)

		val contentWriterConfig = IndexWriterConfig(analyzer)
		contentWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND

		contentDirectory = FSDirectory.open(File(indexPath, ContentDirectoryName).toPath())
		contentWriter = IndexWriter(contentDirectory, contentWriterConfig)
	}


	override fun close() {
		metadataWriter.close()
		metadataDirectory.close()

		contentWriter.close()
		contentDirectory.close()

		analyzer.close()
	}


	override fun index(documentToIndex: Document) {
		val detectedLanguage = detectedContentLanguage(documentToIndex)

		fieldLanguageBasedAnalyzer.setLanguageOfNextField(detectedLanguage)

		if (documentToIndex.language == null && detectedLanguage != DetectedLanguage.NotRecognized) {
			documentToIndex.language = detectedLanguage.name
		}

		documents.updateDocumentForNonNullFields(metadataWriter, UrlFieldName, documentToIndex.url,
			// searchable fields
			fields.fullTextSearchField(ContentFieldName, documentToIndex.content, false),
			fields.keywordField(FilenameFieldName, documentToIndex.filename.toLowerCase(), false),
			fields.nullableKeywordField(ContainingDirectoryFieldName, documentToIndex.containingDirectory?.toLowerCase(), false),
			fields.nullableKeywordField(MetadataTitleFieldName, documentToIndex.title, true),
			fields.nullableKeywordField(MetadataAuthorFieldName, documentToIndex.author, true),
			fields.nullableKeywordField(MetadataSeriesFieldName, documentToIndex.series, true),

			// stored fields
			fields.storedField(FileSizeFieldName, documentToIndex.fileSize),
			fields.storedField(CreatedAtFieldName, documentToIndex.createdAt),
			fields.storedField(LastAccessedFieldName, documentToIndex.lastAccessed),
			fields.storedField(LastModifiedFieldName, documentToIndex.lastModified),
			fields.nullableStoredField(ContentTypeFieldName, documentToIndex.contentType?.toLowerCase()),

			// fields for sorting
			fields.sortField(UrlFieldName, documentToIndex.url)
		)

		documents.updateDocument(contentWriter, UrlFieldName, documentToIndex.url,
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


	override fun remove(document: DocumentMetadata) {
		documents.deleteDocument(metadataWriter, UrlFieldName, document.url)

		documents.deleteDocument(contentWriter, UrlFieldName, document.url)
	}


	override fun optimizeIndex() {
		// NOTE: if you want to maximize search performance,
		// you can optionally call forceMerge here.  This can be
		// a terribly costly operation, so generally it's only
		// worth it when your index is relatively static (ie
		// you're done adding documents to it):

		 metadataWriter.forceMerge(1);

		contentWriter.forceMerge(1);
	}

}