package net.dankito.documents.search.filesystem

import net.dankito.utils.AsyncProducerConsumerQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths


internal class FilesystemWalkerTest {

	companion object {
		val PathToWalk = Paths.get("/media/data/docs/")
	}

	private val underTest = FilesystemWalker()


	@Test
	fun walk() {

		// given
		val discoveredFiles = mutableListOf<Path>()
		val discoveredFilesQueue = AsyncProducerConsumerQueue<Path>(1) { discoveredFile ->
			discoveredFiles.add(discoveredFile)
		}


		// when
		underTest.walk(PathToWalk, discoveredFilesQueue)


		// then
		assertThat(discoveredFiles).isNotEmpty()
	}

}