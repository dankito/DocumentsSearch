package net.dankito.documents.contentextractor

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.utils.AsyncProducerConsumerQueue
import net.dankito.utils.Stopwatch
import net.dankito.utils.filesystem.FilesystemWalker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


internal class FileContentExtractorTest {

	companion object {
		// set your path here
		val PathToWalk: Path = Paths.get(SystemProperties().userHomeDirectory)

		private val log = LoggerFactory.getLogger(FileContentExtractorTest::class.java)
	}


	private val underTest = FileContentExtractor(FileContentExtractorSettings())


	@Test
	fun extractContent() {

		// given
		val extractedContents = mutableMapOf<Path, String?>()
		val discoveredFiles = mutableListOf<Path>()
		val stopwatch = Stopwatch()


		// when
		val discoveredFilesQueue = AsyncProducerConsumerQueue<Path>(10) { discoveredFile ->
			discoveredFiles.add(discoveredFile)

			try {
				extractContent(discoveredFile, extractedContents)
			} catch (e: Exception) {
				extractedContents[discoveredFile] = null

				log.error("Could not extract file $discoveredFile", e)
			}
		}

		FilesystemWalker().walk(PathToWalk) { discoveredFile ->
			discoveredFilesQueue.add(discoveredFile)
		}


		// then
		while (discoveredFilesQueue.isEmpty == false || extractedContents.size < discoveredFiles.size) {
			val notExtractedFiles = ArrayList(discoveredFiles)
			notExtractedFiles.removeAll(extractedContents.keys)
			TimeUnit.MILLISECONDS.sleep(100)
		}

		log.info("Extracting content of ${extractedContents.size} (of ${discoveredFiles.size} discovered) files took ${stopwatch.stopAndPrint()}")

		assertThat(extractedContents).isNotEmpty()
	}

	@Test
	fun extractContentAsync() {

		// given
		val extractedContents = mutableMapOf<Path, String?>()
		val discoveredFiles = mutableListOf<Path>()
		val stopwatch = Stopwatch()


		// when

		runBlocking {
			FilesystemWalker().walk(PathToWalk) { discoveredFile ->
				launch {
					discoveredFiles.add(discoveredFile)

					try {
						extractContentAsync(discoveredFile, extractedContents)
					} catch (e: Exception) {
						log.error("Could not extract file $discoveredFile", e)
					}
				}
			}
		}


		// then
		log.info("Extracting content of ${extractedContents.size} (of ${discoveredFiles.size} discovered) files took ${stopwatch.stopAndPrint()}")

		assertThat(extractedContents).isNotEmpty()
	}


	private fun extractContent(discoveredFile: Path, extractedContents: MutableMap<Path, String?>) {
		try {
			Stopwatch.logDuration("[${extractedContents.size + 1}] Extracting content of $discoveredFile") {
				val result = underTest.extractContent(discoveredFile.toFile())
				extractedContents[discoveredFile] = result.content
			}
		} catch (e: Exception) {
			extractedContents[discoveredFile] = null
			log.error("Could not extract content of file $discoveredFile", e)
		}
	}

	private suspend fun extractContentAsync(discoveredFile: Path, extractedContents: MutableMap<Path, String?>) {
		try {
			val stopwatch = Stopwatch()

			val result = underTest.extractContentSuspendable(discoveredFile.toFile())
			extractedContents[discoveredFile] = result.content

			stopwatch.stopAndLog("[${extractedContents.size}] Extracting content of $discoveredFile", log)

			if (result.content.isNullOrBlank()) {
				filesTheirContentCouldNotGetExtracted.add(discoveredFile)
			}
		} catch (e: Exception) {
			extractedContents[discoveredFile] = null
			log.error("Could not extract content of file $discoveredFile", e)
		}
	}

}