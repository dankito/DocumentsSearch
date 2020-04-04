package net.dankito.documents.search.filesystem

import net.dankito.utils.AsyncProducerConsumerQueue
import net.dankito.utils.info.SystemProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths


internal class FilesystemWalkerTest {

	companion object {
		// set your path here
		val PathToWalk: Path = Paths.get(SystemProperties().userHomeDirectory)
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
		underTest.walk(PathToWalk) { discoveredFile ->
			discoveredFilesQueue.add(discoveredFile)
		}


		// then
		assertThat(discoveredFiles).isNotEmpty()
	}

}