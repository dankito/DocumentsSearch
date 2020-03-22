package net.dankito.documents.search.index

import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.Term
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.WildcardQuery
import java.util.*


open class QueryBuilder {

	/*		String queries		*/

	open fun fulltextQuery(fieldName: String, searchTerm: String): Query {
		return PhraseQuery(fieldName, searchTerm)
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
		return if (caseInsensitive) {
			searchTerm.toLowerCase()
		}
		else {
			searchTerm
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