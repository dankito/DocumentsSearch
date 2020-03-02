package net.dankito.documents.search.filesystem

import net.dankito.utils.AsyncProducerConsumerQueue
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


open class FilesystemWalker {

	fun walk(startDir: Path, discoveredFilesQueue: AsyncProducerConsumerQueue<Path>) {

		Files.walkFileTree(startDir, object : SimpleFileVisitor<Path>() {
			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				file?.let {
					discoveredFilesQueue.add(file)
				}

				return FileVisitResult.CONTINUE
			}
		})
	}

}