package net.dankito.documents.search.index

import org.apache.lucene.document.LongPoint
import org.apache.lucene.search.Query
import java.util.Date


open class QueryBuilder {

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