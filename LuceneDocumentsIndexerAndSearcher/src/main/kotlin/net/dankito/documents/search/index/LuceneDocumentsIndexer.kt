package net.dankito.documents.search.index

import net.dankito.documents.search.index.DocumentFields.Companion.ContentFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.CreatedAtFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FileSizeFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.FilenameFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastAccessedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.LastModifiedFieldName
import net.dankito.documents.search.index.DocumentFields.Companion.UrlFieldName
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.io.Closeable
import java.nio.file.Paths


open class LuceneDocumentsIndexer : IDocumentsIndexer, Closeable {

	protected val directory: Directory

	protected val analyzer: Analyzer

	protected val writer: IndexWriter

	protected val fields = FieldBuilder()



	init {
		directory = FSDirectory.open(Paths.get("data", "index", "documents")) // TODO: make index path settable
		analyzer = StandardAnalyzer()

		val writerConfig = IndexWriterConfig(analyzer)
		writerConfig.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND

		// Optional: for better indexing performance, if you
		// are indexing many documents, increase the RAM
		// buffer.  But if you do this, increase the max heap
		// size to the JVM (eg add -Xmx512m or -Xmx1g):
		//
		// iwc.setRAMBufferSizeMB(256.0);

		writer = IndexWriter(directory, writerConfig)
	}


	override fun close() {
		writer.close()

		analyzer.close()

		directory.close()
	}


	override fun index(documentToIndex: net.dankito.documents.search.model.Document) {
		fields.updateDocument(writer, Term(UrlFieldName, documentToIndex.url),
			fields.keywordField(FilenameFieldName, documentToIndex.filename),
			fields.keywordField(UrlFieldName, documentToIndex.url),
			fields.fullTextSearchField(ContentFieldName, documentToIndex.content, true),
			fields.storedField(FileSizeFieldName, documentToIndex.fileSize),
			fields.storedField(CreatedAtFieldName, documentToIndex.createdAt),
			fields.storedField(LastAccessedFieldName, documentToIndex.lastAccessed),
			fields.storedField(LastModifiedFieldName, documentToIndex.lastModified)
		)

		// NOTE: if you want to maximize search performance,
		// you can optionally call forceMerge here.  This can be
		// a terribly costly operation, so generally it's only
		// worth it when your index is relatively static (ie
		// you're done adding documents to it):
		//
		// writer.forceMerge(1);
	}

}