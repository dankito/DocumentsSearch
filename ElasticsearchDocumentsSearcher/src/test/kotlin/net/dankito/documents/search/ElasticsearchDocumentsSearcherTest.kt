package net.dankito.documents.search

import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


internal class ElasticsearchDocumentsSearcherTest {

	private val underTest = ElasticsearchDocumentsSearcher("192.168.178.32", 6200)


	@Test
	fun search() {

		// given
		val searchResult = AtomicReference<SearchResult>()
		val countDownLatch = CountDownLatch(1)


		// when
		underTest.searchAsync("ticket") { result ->
			searchResult.set(result)
			countDownLatch.countDown()
		}


		// then
		countDownLatch.await(20, TimeUnit.SECONDS)
	}

}