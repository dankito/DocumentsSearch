package net.dankito.documents.search.index

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


open class DateFieldTest : LuceneTestBase() {

	companion object {
		val Date = java.util.Date(88, 2, 27)
	}


	protected val fields = FieldBuilder()

	protected val queries = QueryBuilder()


	@Test
	fun `Exact date`() {

		// given
		index(fields.dateTimeField(FieldName, Date))

		// when
		val result = search(queries.exactDateQuery(FieldName, Date))

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `Date after`() {

		// given
		index(fields.dateTimeField(FieldName, Date))

		// when
		val result = search(queries.afterDateQuery(FieldName, Date))

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `Date before`() {

		// given
		index(fields.dateTimeField(FieldName, Date))

		// when
		val result = search(queries.beforeDateQuery(FieldName, Date))

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `Date out of range`() {

		// given
		index(fields.dateTimeField(FieldName, Date))

		// when
		val result = search(queries.afterDateQuery(FieldName, java.util.Date(Date.time + 1)))

		// then
		assertThat(result).isEmpty()
	}

}