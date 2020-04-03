package net.dankito.documents.search

import net.dankito.documents.search.highlighting.SearchResultsHighlighter
import net.dankito.documents.search.index.*
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.highlight.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.io.File


open class LuceneDocumentsSearcher(
		protected val indexPath: File
) : IDocumentsSearcher, AutoCloseable {

	companion object {
		private val log = LoggerFactory.getLogger(LuceneDocumentsSearcher::class.java)
	}


	protected val analyzer: Analyzer

	protected val metadataDirectory: Directory

	protected val contentDirectory: Directory

	protected val highlighter: SearchResultsHighlighter


	protected val searcher = Searcher()

	protected val mapper = FieldMapper()

	protected val queries = QueryBuilder()

	protected var lastQuery: Query? = null



	init {
		analyzer = StandardAnalyzer()

		metadataDirectory = FSDirectory.open(File(indexPath, LuceneConfig.MetadataDirectoryName).toPath())

		contentDirectory = FSDirectory.open(File(indexPath, LuceneConfig.ContentDirectoryName).toPath())

		highlighter = SearchResultsHighlighter(contentDirectory, analyzer)
	}


	override fun close() {
		analyzer.close()

		metadataDirectory.close()

		contentDirectory.close()
	}


	override fun search(searchTerm: String): SearchResult {
		try {
			val query = createDocumentsQuery(searchTerm)

			val searchResults = searcher.search(metadataDirectory, query,
					sortFields = listOf(SortField(DocumentFields.UrlFieldName, SortField.Type.STRING)))

			this.lastQuery = query

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


	protected open fun mapSearchResults(searchResults: SearchResults): List<DocumentMetadata> {
		return searchResults.hits.map {
			val doc = it.document
			val url = mapper.string(doc, DocumentFields.UrlFieldName)

			DocumentMetadata(
				url,
				url,
				mapper.long(doc, DocumentFields.FileSizeFieldName),
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


	override fun getDocument(metadata: DocumentMetadata): Document? {
		try {
			val searchResults = searcher.search(contentDirectory, TermQuery(Term(DocumentFields.UrlFieldName, metadata.url)), 1)

			if (searchResults.hits.isNotEmpty()) {
				val searchResult = searchResults.hits[0]
				val doc = searchResult.document

				var content = mapper.string(doc, DocumentFields.ContentFieldName)

				val test = highlighter.highlightSearchResultsWithUnifiedHighlighter(searchResults, lastQuery!!, content)

				content = highlighter.highlightSearchResults(lastQuery, searchResult.documentId, content)

				return Document(content, metadata)
			}
		} catch (e: Exception) {
			log.error("Could not get Document for metadata $metadata", e)
		}

		return null
	}


	protected open fun highlightSearchResults(searchResults: SearchResults, query: Query): List<Document> {
		val reader = DirectoryReader.open(contentDirectory)

//		val unifiedHighlighter = UnifiedHighlighter(IndexSearcher(reader), analyzer)
////			unifiedHighlighter.setFormatter(DefaultPassageFormatter())
//		val highlights = unifiedHighlighter.highlight(DocumentFields.ContentFieldName, query, searchResults.topDocs, 4)
//		if (highlights != null) { }

		val highlightMarkupLength = 7
		val textStartPosField = TextFragment::class.java.getDeclaredField("textStartPos")
		textStartPosField.isAccessible = true
		val textEndPosField = TextFragment::class.java.getDeclaredField("textEndPos")
		textEndPosField.isAccessible = true

		return searchResults.hits.mapIndexed { index, searchResult ->
			val doc = searchResult.document
			val url = mapper.string(doc, DocumentFields.UrlFieldName)

			var content = mapper.string(doc, DocumentFields.ContentFieldName)

			val htmlFormatter = SimpleHTMLFormatter()
			val highlighter = Highlighter(htmlFormatter, QueryScorer(query))
			val tokenStream = TokenSources.getAnyTokenStream(reader, searchResult.documentId, DocumentFields.ContentFieldName, analyzer)
			val fragments = highlighter.getBestTextFragments(tokenStream, mapper.string(doc, DocumentFields.ContentFieldName), false, 10)
			val textFragments = fragments.filter { it.score > 0 }
			textFragments.forEach { fragment ->
				val textStartPos = textStartPosField.getInt(fragment)
				val textEndPos = textEndPosField.getInt(fragment)

				content = content.replaceRange(textStartPos, textEndPos - highlightMarkupLength, fragment.toString())
			}

			Document(
					url,
					url,
					content,
					mapper.long(doc, DocumentFields.FileSizeFieldName),
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