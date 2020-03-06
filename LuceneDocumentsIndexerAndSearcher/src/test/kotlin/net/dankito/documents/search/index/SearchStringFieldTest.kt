package net.dankito.documents.search.index

import org.apache.lucene.index.Term
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


open class SearchStringFieldTest : LuceneTestBase() {

	@Test
	fun `TermQuery - only whole string matches`() {

		// given
		index(FieldName, "Mahatma Gandhi")

		// when
		val query = TermQuery(Term(FieldName, "Mahatma Gandhi"))
		val result = search(query)

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `TermQuery - single term does not match`() {

		// given
		index(FieldName, "Mahatma Gandhi")

		// when
		val query = TermQuery(Term(FieldName, "Gandhi"))
		val result = search(query)

		// then
		assertThat(result).isEmpty()
	}

	@Test
	fun `PrefixQuery ('startsWith()') matches`() {

		// given
		index(FieldName, "Mahatma Gandhi")

		// when
		val query = PrefixQuery(Term(FieldName, "Mahatma"))
		val result = search(query)

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `WildcardQuery ('contains()') matches`() {

		// given
		index(FieldName, "Mahatma Gandhi")

		// when
		val query = WildcardQuery(Term(FieldName, "*Gand*"))
		val result = search(query)

		// then
		assertThat(result).hasSize(1)
	}

	@Test
	fun `WildcardQuery ('endsWith()') matches`() {

		// given
		index(FieldName, "Mahatma Gandhi")

		// when
		val query = WildcardQuery(Term(FieldName, "*Gandhi"))
		val result = search(query)

		// then
		assertThat(result).hasSize(1)
	}

}