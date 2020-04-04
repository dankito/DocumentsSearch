package net.dankito.documents.search.filesystem

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes


open class FilesystemWalker {

	companion object {
		private val log = LoggerFactory.getLogger(FilesystemWalker::class.java)
	}


	open fun listFiles(startDir: Path): List<Path> {
		val discoveredFiles = mutableListOf<Path>()

		walk(startDir) { discoveredFile: Path ->
			discoveredFiles.add(discoveredFile)
		}

		return discoveredFiles
	}


	open fun walk(startDir: Path, discoveredFileCallback: (Path) -> Unit) {

		detailedWalk(startDir) { visitedFile: VisitedFile ->
			visitedFile.path?.let { discoveredFile ->
				discoveredFileCallback(discoveredFile)
			}
		}
	}


	open fun detailedWalk(startDir: Path, continueOnError: Boolean = true,
						  preVisitDirectory: ((directory: Path?) -> FileVisitResult)? = null,
						  postVisitDirectory: ((directory: Path?) -> FileVisitResult)? = null,
						  visitedFileCallback: (VisitedFile) -> Unit) {

		Files.walkFileTree(startDir, object : FileVisitor<Path> {

			// files:

			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				visitedFileCallback(VisitedFile(file, attributes))

				return FileVisitResult.CONTINUE
			}

			override fun visitFileFailed(file: Path?, exception: IOException?): FileVisitResult {
				log.error("Could not visit file '$file'", exception)

				visitedFileCallback(VisitedFile(file, null, exception))

				return if (continueOnError) FileVisitResult.CONTINUE else FileVisitResult.TERMINATE
			}


			// directories:

			override fun preVisitDirectory(directory: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				return preVisitDirectory?.invoke(directory)
						?: FileVisitResult.CONTINUE
			}

			override fun postVisitDirectory(directory: Path?, exception: IOException?): FileVisitResult {
				return postVisitDirectory?.invoke(directory)
						?: FileVisitResult.CONTINUE
			}

		})
	}

}