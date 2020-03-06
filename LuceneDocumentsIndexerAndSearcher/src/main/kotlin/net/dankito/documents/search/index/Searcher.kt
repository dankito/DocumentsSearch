package net.dankito.documents.search.index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.store.Directory


open class Searcher {

	@JvmOverloads
	open fun search(directory: Directory, query: Query, countMaxResults: Int = 1000): SearchResults {
		val reader = DirectoryReader.open(directory)
		val searcher = IndexSearcher(reader)

		val topDocs = searcher.search(query, countMaxResults)

		val hits = topDocs.scoreDocs.map { SearchResult(it.score, searcher.doc(it.doc)) }

		reader.close()

		return SearchResults(topDocs.totalHits.value, hits)
	}

}