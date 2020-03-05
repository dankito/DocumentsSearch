package net.dankito.documents.search.filesystem

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import net.dankito.utils.AsyncProducerConsumerQueue
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


open class FilesystemWalker {

	open fun walk(startDir: Path, discoveredFilesQueue: AsyncProducerConsumerQueue<Path>) {

		Files.walkFileTree(startDir, object : SimpleFileVisitor<Path>() {
			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				file?.let {
					discoveredFilesQueue.add(file)
				}

				return FileVisitResult.CONTINUE
			}
		})
	}


	open fun walk(startDir: Path): Flowable<Path> {
		val flowableOnSubscribe = FlowableOnSubscribe<Path> { emitter -> visitFilesForEmitter(startDir, emitter) }

		return Flowable.create(flowableOnSubscribe, BackpressureStrategy.BUFFER)
	}

	protected open fun visitFilesForEmitter(startDir: Path, emitter: FlowableEmitter<Path>) {
		Files.walkFileTree(startDir, object : SimpleFileVisitor<Path>() {
			override fun visitFile(file: Path?, attributes: BasicFileAttributes?): FileVisitResult {
				file?.let {
					emitter.onNext(file)
				}

				return FileVisitResult.CONTINUE
			}

			override fun postVisitDirectory(path: Path?, exception: IOException?): FileVisitResult {
				if (path == startDir) {
					emitter.onComplete()
				}

				return super.postVisitDirectory(path, exception)
			}
		})
	}

}