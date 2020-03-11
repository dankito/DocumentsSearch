package net.dankito.documents.search.filesystem

import net.dankito.utils.AsyncProducerConsumerQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths


internal class FilesystemWalkerTest {

	private val underTest = FilesystemWalker()


	@Test
	fun walk() {

		// given
		val discoveredFiles = mutableListOf<Path>()
		val discoveredFilesQueue = AsyncProducerConsumerQueue<Path>(1) { discoveredFile ->
			discoveredFiles.add(discoveredFile)
		}


		// when
		underTest.walk(Paths.get("/home/ganymed/data/docs/"), discoveredFilesQueue)


		// then
		assertThat(discoveredFiles).isNotEmpty()
	}

	@Test
	fun walkFlowable() {

		// given
		val discoveredFiles = mutableListOf<Path>()


		// when
		underTest.walk(Paths.get("/home/ganymed/data/docs/")).subscribe { discoveredFiles.add(it) }


		// then
		assertThat(discoveredFiles).isNotEmpty()
	}

}