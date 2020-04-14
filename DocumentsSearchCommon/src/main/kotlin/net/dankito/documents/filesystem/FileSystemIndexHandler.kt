package net.dankito.documents.filesystem

import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import net.dankito.documents.IIndexHandler
import net.dankito.documents.contentextractor.FileContentExtractionResult
import net.dankito.documents.contentextractor.IFileChecksumCalculator
import net.dankito.documents.contentextractor.IFileContentExtractor
import net.dankito.documents.contentextractor.Sha512FileChecksumCalculator
import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexedDirectoryConfig
import net.dankito.utils.filesystem.watcher.FileChange
import net.dankito.utils.filesystem.watcher.FileChangeInfo
import net.dankito.utils.filesystem.watcher.IFileSystemWatcher
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


open class FileSystemIndexHandler(
        protected val contentExtractor: IFileContentExtractor,
        protected val fileSystemWatcher: IFileSystemWatcher,
        protected val indexUpdatedEventBus: PublishSubject<IndexConfig>
) : IIndexHandler {

    companion object {
        private val log = LoggerFactory.getLogger(FileSystemIndexHandler::class.java)
    }


    protected val filesToIndexFinder = FilesToIndexFinder()

    protected val fileChecksumCalculator: IFileChecksumCalculator = Sha512FileChecksumCalculator()

    protected var stopFindingFilesToIndex: AtomicBoolean? = null


    override suspend fun updateIndexDirectoriesDocuments(index: IndexConfig, directoryConfig: IndexedDirectoryConfig,
                                                         currentFilesInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer) {
        stopFindingFilesToIndex?.set(true)

        withContext(Dispatchers.IO) {
            val stopTraversal = AtomicBoolean(false)
            stopFindingFilesToIndex = stopTraversal

            filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(directoryConfig, stopTraversal)) { fileToIndex ->
                val file = fileToIndex.path.toFile()
                val url = file.absolutePath
                val attributes = fileToIndex.attributes ?: Files.readAttributes(fileToIndex.path, BasicFileAttributes::class.java)

                async(Dispatchers.IO) {
                    if (isNewOrUpdatedFile(file, url, attributes, currentFilesInIndex)) {
                        extractContentAndIndex(file, url, attributes, indexer)

                        indexUpdatedEventBus.onNext(index)
                    }

                    currentFilesInIndex.remove(url)
                }
            }
        }
    }

    protected open fun isNewOrUpdatedFile(file: File, url: String, attributes: BasicFileAttributes, currentFilesInIndex: Map<String, DocumentMetadata>): Boolean {
        val metadata = currentFilesInIndex[url]

        if (metadata == null) { // a new file
            log.debug("New file discovered: {}", file)

            return true
        }

        val isUpdated = file.length() != metadata.fileSize
                || attributes.lastModifiedTime().toMillis() != metadata.lastModified.time
                || metadata.checksum != calculateFileChecksum(file)

        if (isUpdated) {
            log.debug("Updated file discovered: {}\n" +
                    "File size changed: {}\n" +
                    "Last modified changed: {}", // do not calculate checksum twice, only lazily above
                    file, file.length() != metadata.fileSize,
                    attributes.lastModifiedTime().toMillis() != metadata.lastModified.time)
        }

        return isUpdated
    }


    override suspend fun extractContentAndIndex(file: File, url: String, attributes: BasicFileAttributes, indexer: IDocumentsIndexer) {
        try {
            val result = contentExtractor.extractContentSuspendable(file)

            val document = createDocument(file, url, attributes, result)

            indexer.index(document)
        } catch (e: Exception) {
            log.error("Could not extract file '$file'", e)
        }
    }

    protected open fun createDocument(file: File, url: String, attributes: BasicFileAttributes, result: FileContentExtractionResult): Document {

        return Document(
                url,
                url,
                result.content ?: "",
                file.length(),
                calculateFileChecksum(file),
                Date(attributes.creationTime().toMillis()),
                Date(attributes.lastModifiedTime().toMillis()),
                Date(attributes.lastAccessTime().toMillis()),
                result.contentType, result.title, result.author, result.length, result.category,
                result.language, result.series, result.keywords
        )
    }


    override fun listenForChangesToIndexedItems(index: IndexConfig, indexedDirectory: IndexedDirectoryConfig, indexer: IDocumentsIndexer) {
        fileSystemWatcher.startWatchFolderRecursively(indexedDirectory.directory) { changeInfo ->
            if (changeInfo.file.isDirectory == false) {
                handleIndexedFileChangedAsync(index, indexedDirectory, indexer, changeInfo)
            }
        }
    }

    protected open fun handleIndexedFileChangedAsync(index: IndexConfig, indexedDirectory: IndexedDirectoryConfig, indexer: IDocumentsIndexer,
                                                     changeInfo: FileChangeInfo) = GlobalScope.launch(Dispatchers.IO) {

        val file = changeInfo.file.toFile()
        val url = file.absolutePath
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)

        if (changeInfo.change == FileChange.Created || changeInfo.change == FileChange.Modified) {
            extractContentAndIndex(file, url, attributes, indexer)
        }
        else if (changeInfo.change == FileChange.Deleted) {
            indexer.remove(url)
        }
        else if (changeInfo.change == FileChange.Renamed) {
            changeInfo.previousName?.let {
                indexer.remove(it.absolutePath)
            }

            extractContentAndIndex(file, url, attributes, indexer)
        }

        indexUpdatedEventBus.onNext(index)
    }


    protected open fun calculateFileChecksum(file: File): String {
        return fileChecksumCalculator.calculateChecksum(file)
    }

}