package net.dankito.utils.lucene.search

import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import java.util.*


open class QueryBuilder {

	open fun allDocuments(): Query {
		return MatchAllDocsQuery()
	}


	open fun createQueriesForSingleTerms(searchTerm: String, singleTermQueryBuilder: (singleTerm: String) -> List<Query>): Query {
		if (searchTerm.isBlank()) {
			return allDocuments()
		}

		val singleTerms = searchTerm.split(" ").filter { it.isNotBlank() }

		val queryBuilder = BooleanQuery.Builder()

		singleTerms.forEach { singleTerm ->
			queryBuilder.add(createQueryForSingleTerm(singleTerm, singleTermQueryBuilder), BooleanClause.Occur.MUST)
		}

		return queryBuilder.build()
	}

	protected open fun createQueryForSingleTerm(singleTerm: String, singleTermQueryBuilder: (singleTerm: String) -> List<Query>): Query {
		val singleTermQueries = singleTermQueryBuilder(singleTerm)

		val queryBuilder = BooleanQuery.Builder()

		singleTermQueries.forEach {
			queryBuilder.add(it, BooleanClause.Occur.SHOULD)
		}

		return queryBuilder.build()
	}


	/*		String queries		*/

	open fun fulltextQuery(fieldName: String, searchTerm: String): Query {
		return PrefixQuery(Term(fieldName, adjustSearchTerm(searchTerm, false)))
	}

	@JvmOverloads
	open fun startsWith(fieldName: String, searchTerm: String, caseInsensitive: Boolean = true): Query {
		return wildcardQuery(fieldName, searchTerm, caseInsensitive, true, false)
	}

	@JvmOverloads
	open fun contains(fieldName: String, searchTerm: String, caseInsensitive: Boolean = true): Query {
		return wildcardQuery(fieldName, searchTerm, caseInsensitive)
	}

	@JvmOverloads
	open fun endsWith(fieldName: String, searchTerm: String, caseInsensitive: Boolean = true): Query {
		return wildcardQuery(fieldName, searchTerm, caseInsensitive, false, true)
	}

	@JvmOverloads
	open fun wildcardQuery(fieldName: String, searchTerm: String, caseInsensitive: Boolean = true): Query {
		return wildcardQuery(fieldName, searchTerm, caseInsensitive, true, true)
	}

	open fun wildcardQuery(fieldName: String, searchTerm: String, caseInsensitive: Boolean ,
						   prefixWildcard: Boolean, suffixWildcard: Boolean): Query {
		val adjustedSearchTerm = adjustSearchTermForWildcardQuery(searchTerm, caseInsensitive, prefixWildcard, suffixWildcard)

		return WildcardQuery(Term(fieldName, adjustedSearchTerm))
	}

	protected open fun adjustSearchTermForWildcardQuery(searchTerm: String, caseInsensitive: Boolean,
														prefixWildcard: Boolean, suffixWildcard: Boolean): String {
		val adjustedSearchTerm = adjustSearchTerm(searchTerm, caseInsensitive)

		if (prefixWildcard && suffixWildcard) {
			return "*$adjustedSearchTerm*"
		}
		else if (prefixWildcard) {
			return "$adjustedSearchTerm*"
		}
		else {
			return "*$adjustedSearchTerm"
		}
	}

	protected open fun adjustSearchTerm(searchTerm: String, caseInsensitive: Boolean): String {
		val adjustedSearchTerm = QueryParser.escape(searchTerm)

		return if (caseInsensitive) {
			adjustedSearchTerm.toLowerCase()
		}
		else {
			adjustedSearchTerm
		}
	}


	/*		Date queries		*/

	open fun exactDateQuery(fieldName: String, date: Date): Query {
		return LongPoint.newExactQuery(fieldName, date.time)
	}

	open fun afterDateQuery(fieldName: String, dateAfterThisInclusive: Date): Query {
		return LongPoint.newRangeQuery(fieldName, dateAfterThisInclusive.time, Long.MAX_VALUE)
	}

	open fun beforeDateQuery(fieldName: String, dateBeforeThisInclusive: Date): Query {
		return LongPoint.newRangeQuery(fieldName, Long.MIN_VALUE, dateBeforeThisInclusive.time)
	}

}