package net.dankito.documents.search.filesystem

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


open class FilesystemWalker {

	open fun listFiles(startDir: Path): List<Path> {
		val discoveredFiles = mutableListOf<Path>()

		Files.walkFileTree(startDir, object : SimpleFileVisitor<Path>() {
			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				file?.let {
					discoveredFiles.add(file)
				}

				return FileVisitResult.CONTINUE
			}
		})

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
		})
	}

}