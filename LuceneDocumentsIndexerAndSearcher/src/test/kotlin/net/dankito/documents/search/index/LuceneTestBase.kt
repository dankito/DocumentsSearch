package net.dankito.documents.search.index

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.store.RAMDirectory
import org.junit.jupiter.api.AfterEach


abstract class LuceneTestBase {

	companion object {
		const val FieldName = "test_field"
	}


	protected val directory = RAMDirectory()

	protected val analyzer = StandardAnalyzer()

	protected val writerConfig = IndexWriterConfig(analyzer)

	protected val writer = IndexWriter(directory, writerConfig)


	@AfterEach
	open fun tearDown() {
		writer.close()

		analyzer.close()

		directory.close()
	}


	protected open fun index(name: String, value: String, store: Boolean = true) {
		index(StringField(name, value, if (store) Field.Store.YES else Field.Store.NO))
	}

	protected open fun index(field: IndexableField) {
		index(listOf(field))
	}

	protected open fun index(fields: List<IndexableField>) {
		writer.addDocument(fields)

		writer.commit()
	}


	protected open fun search(query: Query): List<Document> {
		val reader = DirectoryReader.open(directory)
		val searcher = IndexSearcher(reader)

		val scoreDocs = searcher.search(query, 10).scoreDocs

		val result = scoreDocs.map { searcher.doc(it.doc) }

		reader.close()

		return result
	}

}