package net.dankito.documents.search.ui.presenter

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dankito.documents.contentextractor.FileContentExtractor
import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.LuceneDocumentsSearcher
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.filesystem.FilesystemWalker
import net.dankito.documents.search.index.LuceneDocumentsIndexer
import net.dankito.documents.search.model.Cancellable
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.model.IndexConfig
import net.dankito.utils.ThreadPool
import net.dankito.utils.io.FileUtils
import net.dankito.utils.serialization.ISerializer
import net.dankito.utils.serialization.JacksonJsonSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*


open class DocumentsSearchPresenter : AutoCloseable {

	companion object {
		val DataPath = File("data")

		val IndicesFile = File(DataPath, "indices.json")

		private val log = LoggerFactory.getLogger(DocumentsSearchPresenter::class.java)
	}


	protected var indicesField: MutableList<IndexConfig> = mutableListOf()

	open val indices: List<IndexConfig>
		get() = ArrayList(indicesField) // make a copy


	protected val serializer: ISerializer = JacksonJsonSerializer()

	protected val threadPool = ThreadPool()

	protected val documentsSearchers: MutableMap<IndexConfig, IDocumentsSearcher> = mutableMapOf()

	protected var lastSearchCancellable: Cancellable? = null


	init {
		restoreIndices()
	}


	override fun close() {
		documentsSearchers.forEach { searcher ->
			(searcher as? AutoCloseable)?.close()
		}
	}


	protected open fun restoreIndices() {
		try {
			val indicesJson = FileInputStream(IndicesFile).bufferedReader().readText()

			serializer.deserializeList(indicesJson, IndexConfig::class.java)?.let {
				indicesField = ArrayList(it)

				indicesField.forEach { index ->
					createDocumentsSearcherForIndex(index)
				}
			}
		} catch (e: Exception) {
			log.error("Could not deserialize indices file $IndicesFile", e)
		}
	}

	protected open fun persistIndices() {
		try {
			serializer.serializeObject(indicesField, IndicesFile)
		} catch (e: Exception) {
			log.error("Could not deserialize indices file $IndicesFile", e)
		}
	}


	open fun saveOrUpdateIndex(index: IndexConfig, didIndexDocumentsChange: Boolean) {
		if (index.isIdSet == false) { // a new index
			saveIndex(index)
		}
		else if (didIndexDocumentsChange) { // index's documents have most likely changed -> recreate index (TODO: is it necessary to remove whole index?)
			removeIndex(index)
			saveIndex(index)
		}
		else { // simply save configuration changes like index's name
			persistIndices()
		}
	}

	open fun saveIndex(index: IndexConfig) {
		index.id = UUID.randomUUID().toString()

		indicesField.add(index)

		persistIndices()

		createDocumentsSearcherForIndex(index)

		updateIndexDocuments(index)
	}

	open fun removeIndex(index: IndexConfig) {
		indicesField.remove(index)

		persistIndices()

		(documentsSearchers[index] as? AutoCloseable)?.close()

		documentsSearchers.remove(index)

		FileUtils().deleteFolderRecursively(getIndexPath(index))
	}

	protected open fun createDocumentsSearcherForIndex(index: IndexConfig) {
		documentsSearchers[index] = LuceneDocumentsSearcher(getIndexPath(index), threadPool)
	}


	protected open fun updateIndexDocuments(index: IndexConfig) {
		LuceneDocumentsIndexer(getIndexPath(index)).use { indexer ->
			val contentExtractor = FileContentExtractor(FileContentExtractorSettings()) // TODO: use a common FileContentExtractor?

			index.directoriesToIndex.forEach { directoryToIndex ->
				GlobalScope.launch {
					FilesystemWalker().walk(directoryToIndex.toPath()) { discoveredFile ->
						try {
							val document = createDocument(discoveredFile, contentExtractor)

							indexer.index(document)
						} catch (e: Exception) {
							log.error("Could not extract file $discoveredFile", e)
						}
					}
				}
			}
		}
	}

	protected open fun createDocument(path: Path, contentExtractor: FileContentExtractor): Document {
		val file = path.toFile()
		val url = file.absolutePath
		val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

		return Document(
				url,
				file.name,
				url,
				contentExtractor.extractContent(file) ?: "",
				file.length(),
				Date(attributes.creationTime().toMillis()),
				Date(attributes.lastModifiedTime().toMillis()),
				Date(attributes.lastAccessTime().toMillis())
		)
	}


	open fun searchDocumentsAsync(searchTerm: String, index: IndexConfig, callback: (SearchResult) -> Unit) {
		lastSearchCancellable?.cancel()

		lastSearchCancellable = documentsSearchers[index]?.searchAsync(searchTerm, callback)
	}

	protected open fun getIndexPath(index: IndexConfig): File {
		return File(File(DataPath, "index"), index.name)
	}

}