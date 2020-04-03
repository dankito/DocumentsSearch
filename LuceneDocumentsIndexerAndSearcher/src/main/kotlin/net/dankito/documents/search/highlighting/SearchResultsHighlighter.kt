package net.dankito.documents.search.highlighting

import net.dankito.documents.search.index.DocumentFields
import net.dankito.documents.search.index.SearchResults
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.highlight.*
import org.apache.lucene.search.uhighlight.UnifiedHighlighter
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory
import java.lang.reflect.Field


open class SearchResultsHighlighter(
        protected val directory: Directory,
        protected val analyzer: Analyzer
) {

    companion object {
        private val log = LoggerFactory.getLogger(SearchResultsHighlighter::class.java)
    }


    val highlightMarkupLength = 7
    protected val textStartPosField: Field?
    protected val textEndPosField: Field?


    init {
        val textFragmentClass = TextFragment::class.java

        val fieldNames = textFragmentClass.declaredFields.map { it.name }

        if (fieldNames.contains("textStartPos") && fieldNames.contains("textEndPos")) {
            textStartPosField = textFragmentClass.getDeclaredField("textStartPos")
            textStartPosField.isAccessible = true

            textEndPosField = TextFragment::class.java.getDeclaredField("textEndPos")
            textEndPosField.isAccessible = true
        }
        else {
            textStartPosField = null
            textEndPosField = null
        }
    }


    open fun highlightSearchResults(query: Query?, docId: Int, content: String): String {
        try {
            textStartPosField?.let { textStartPosField ->
                textEndPosField?.let { textEndPosField ->
                    query?.let {
                        return highlightSearchResults(query, docId, content, textStartPosField, textEndPosField)
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Could not highlight text for $query -> $content", e)
        }

        return content
    }

    protected open fun highlightSearchResults(query: Query, docId: Int, content: String,
                                       textStartPosField: Field, textEndPosField: Field): String {

        val textFragments = getHighlightedTextFragments(query, docId, content)
        var resultContent = content

        textFragments.forEach { fragment ->
            val textStartPos = textStartPosField.getInt(fragment)
            val textEndPos = textEndPosField.getInt(fragment)

            resultContent = resultContent.replaceRange(textStartPos, textEndPos - highlightMarkupLength, fragment.toString())
        }

        return resultContent
    }

    protected open fun getHighlightedTextFragments(query: Query, docId: Int, content: String): List<TextFragment> {
        val reader = DirectoryReader.open(directory)

        val htmlFormatter = SimpleHTMLFormatter()
        val highlighter = Highlighter(htmlFormatter, QueryScorer(query))
        val tokenStream = TokenSources.getAnyTokenStream(reader, docId, DocumentFields.ContentFieldName, analyzer)
        val fragments = highlighter.getBestTextFragments(tokenStream, content, false, 10)

        return fragments.filter { it.score > 0 } // return only fragments with really contain a search result
    }


    open fun highlightSearchResultsWithUnifiedHighlighter(searchResults: SearchResults, query: Query, content: String): List<String> {
        try {
            val reader = DirectoryReader.open(directory)

            val unifiedHighlighter = UnifiedHighlighter(IndexSearcher(reader), analyzer)
            unifiedHighlighter.maxLength = Int.MAX_VALUE - 1
//			unifiedHighlighter.setFormatter(DefaultPassageFormatter())
            val highlights = unifiedHighlighter.highlight(DocumentFields.ContentFieldName, query, searchResults.topDocs)
//        val highlights = unifiedHighlighter.highlight(DocumentFields.ContentFieldName, query, TopDocs(TotalHits(1,
//                TotalHits.Relation.EQUAL_TO), arrayOf(ScoreDoc(docId, 1f))))

            val testUnifiedHighlighter = UnifiedHighlighter(null, analyzer)
            val testHighlights = testUnifiedHighlighter.highlightWithoutSearcher(DocumentFields.ContentFieldName, query, content, 10)

            return highlights.toList()
        } catch (e: Exception) {
            log.error("Could not highlight search results with UnifiedHighlighter", e)
        }

        return listOf()
    }

}