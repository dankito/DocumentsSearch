package net.dankito.documents.search.index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.store.Directory


open class Searcher {

	@JvmOverloads
	open fun search(directory: Directory, query: Query, countMaxResults: Int = 10_000,
					sortFields: List<SortField> = listOf()): SearchResults {
		val reader = DirectoryReader.open(directory)
		val searcher = IndexSearcher(reader)

		val topDocs = if (sortFields.isEmpty()) searcher.search(query, countMaxResults)
					  else searcher.search(query, countMaxResults, Sort(*sortFields.toTypedArray()))

		val hits = topDocs.scoreDocs.map { SearchResult(it.score, searcher.doc(it.doc)) }

		reader.close()

		return SearchResults(topDocs.totalHits.value, hits)
	}

}