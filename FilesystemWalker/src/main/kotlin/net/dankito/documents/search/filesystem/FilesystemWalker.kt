package net.dankito.documents.search.filesystem

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


open class FilesystemWalker {

	companion object {
		private val log = LoggerFactory.getLogger(FilesystemWalker::class.java)
	}


	open fun listFiles(startDir: Path): List<Path> {
		val discoveredFiles = mutableListOf<Path>()

		walk(startDir) { discoveredFile ->
			discoveredFiles.add(discoveredFile)
		}

		return discoveredFiles
	}


	open fun walk(startDir: Path, discoveredFileCallback: (Path) -> Unit) {

		Files.walkFileTree(startDir, object : SimpleFileVisitor<Path>() {
			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				file?.let {
					discoveredFileCallback(file)
				}

				return FileVisitResult.CONTINUE
			}

			override fun visitFileFailed(file: Path?, exception: IOException?): FileVisitResult {
				log.error("Could not visit file '$file'", exception)

				return FileVisitResult.CONTINUE
			}
		})
	}

}