package net.dankito.documents.contentextractor

import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.documents.search.filesystem.FilesystemWalker
import net.dankito.utils.AsyncProducerConsumerQueue
import net.dankito.utils.Stopwatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


internal class FileContentExtractorTest {

	companion object {
		private val log = LoggerFactory.getLogger(FileContentExtractorTest::class.java)
	}


	private val underTest = FileContentExtractor(FileContentExtractorSettings())


	@BeforeEach
	fun setUp() {
		File("extractedContent").mkdirs() // TODO: remove again
	}


	@Test
	fun extractContent() {

		// given
		val extractedContents = mutableMapOf<Path, String?>()
		val discoveredFiles = mutableListOf<Path>()
		val stopwatch = Stopwatch()


		// when
		val discoveredFilesQueue = AsyncProducerConsumerQueue<Path>(10) { discoveredFile ->
			discoveredFiles.add(discoveredFile)

			extractContent(discoveredFile, extractedContents)
		}

		FilesystemWalker().walk(Paths.get("/home/ganymed/data/docs/"), discoveredFilesQueue)


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
	fun extractContentWithFlowable() {

		// given
		val extractedContents = mutableMapOf<Path, String?>()
		val discoveredFiles = mutableListOf<Path>()
		val extractedContentOutputDir = File("extractedContent")
		extractedContentOutputDir.mkdirs()
		val stopwatch = Stopwatch()


		// when
		val flowable = FilesystemWalker().walk(Paths.get("/home/ganymed/data/docs/"))

		val disposable = flowable
				.subscribe { discoveredFile ->
//				.parallel(10)
//				.runOn(Schedulers.io())
//				.map { discoveredFile ->
					discoveredFiles.add(discoveredFile)

					extractContent(discoveredFile, extractedContents)
		}


		// then
		while (disposable.isDisposed == false) {
			TimeUnit.MILLISECONDS.sleep(100)
		}

		log.info("Extracting content of ${extractedContents.size} (of ${discoveredFiles.size} discovered) files took ${stopwatch.stopAndPrint()}")

		assertThat(extractedContents).isNotEmpty()
	}


	private fun extractContent(discoveredFile: Path, extractedContents: MutableMap<Path, String?>) {
		try {
			net.dankito.utils.Stopwatch.logDuration("Extracting content of $discoveredFile") {
				val extractedContent = underTest.extractContent(discoveredFile.toFile())
				extractedContents[discoveredFile] = extractedContent

//				log.info((if (extractedContent.isNullOrBlank()) "Could not extract" else "Successfully extracted") +
//						" content of file $discoveredFile")
//
//				extractedContent?.let {
//					val writer = FileOutputStream(File(File("extractedContent"), discoveredFile.toFile().name + ".txt")).bufferedWriter()
//					writer.write(extractedContent)
//					writer.close()
//				}
			}
		} catch (e: Exception) {
			extractedContents[discoveredFile] = null
			log.error("Could not extract content of file $discoveredFile", e)
		}
	}

}