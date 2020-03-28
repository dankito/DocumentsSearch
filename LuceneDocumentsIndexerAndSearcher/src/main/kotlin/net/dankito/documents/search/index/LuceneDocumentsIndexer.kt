package net.dankito.documents.search.index

import net.dankito.documents.language.ILanguageDetector
import net.dankito.documents.search.LuceneConfig.Companion.ContentDirectoryName
import net.dankito.documents.search.LuceneConfig.Companion.MetadataDirectoryName
import net.dankito.documents.search.index.DocumentFields.Companion.ContainingDirectoryFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.ContentFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.CreatedAtFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FileSizeFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FilenameFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastAccessedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastModifiedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.UrlFieldName
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
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


	override fun index(documentToIndex: net.dankito.documents.search.model.Document) {
		fieldLanguageBasedAnalyzer.setLanguageOfNextField(languageDetector.detectLanguage(documentToIndex.content))

		fields.updateDocumentForNonNullFields(metadataWriter, Term(UrlFieldName, documentToIndex.url),
			// searchable fields
			fields.fullTextSearchField(ContentFieldName, documentToIndex.content, false),
			fields.keywordField(FilenameFieldName, documentToIndex.filename.toLowerCase(), false),
			fields.nullableKeywordField(ContainingDirectoryFieldName, documentToIndex.containingDirectory?.toLowerCase(), false),

			// stored fields
			fields.storedField(UrlFieldName, documentToIndex.url),
			fields.storedField(FileSizeFieldName, documentToIndex.fileSize),
			fields.storedField(CreatedAtFieldName, documentToIndex.createdAt),
			fields.storedField(LastAccessedFieldName, documentToIndex.lastAccessed),
			fields.storedField(LastModifiedFieldName, documentToIndex.lastModified),

			// fields for sorting
			fields.sortField(UrlFieldName, documentToIndex.url)
		)

		fields.updateDocument(contentWriter, Term(UrlFieldName, documentToIndex.url),
			fields.keywordField(UrlFieldName, documentToIndex.url, false),
			fields.storedField(ContentFieldName, documentToIndex.content)
		)
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