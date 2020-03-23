package net.dankito.documents.search.ui.presenter

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
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
import net.dankito.documents.search.model.JobsCancellable
import net.dankito.documents.search.ui.model.AppSettings
import net.dankito.utils.Stopwatch
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

		val AppSettingsFile = File(DataPath, "app_settings.json")

		val IndicesFile = File(DataPath, "indices.json")

		private val log = LoggerFactory.getLogger(DocumentsSearchPresenter::class.java)
	}


	protected var indicesField: MutableList<IndexConfig> = mutableListOf()

	protected val documentsSearchers: MutableMap<IndexConfig, IDocumentsSearcher> = mutableMapOf()

	protected var lastSearchCancellable: Cancellable? = null


	protected val indexUpdatedEventBus = PublishSubject.create<IndexConfig>()

	protected val serializer: ISerializer = JacksonJsonSerializer()

	protected val threadPool = ThreadPool()


	open var appSettings: AppSettings = AppSettings()
		protected set

	open val indices: List<IndexConfig>
		get() = ArrayList(indicesField) // make a copy

	open val selectedIndex: IndexConfig?
		get() = indices.firstOrNull { it.id == appSettings.selectedIndexId }

	open var lastSearchTerm: String = ""
		protected set


	init {
		restoreAppSettings()
		restoreIndices()
	}


	override fun close() {
		documentsSearchers.forEach { searcher ->
			(searcher as? AutoCloseable)?.close()
		}
	}


	open fun subscribeToIndexUpdatedEvents(): Flowable<IndexConfig> {
		return indexUpdatedEventBus.toFlowable(BackpressureStrategy.LATEST)
	}


	protected open fun restoreIndices() {
		try {
			if (IndicesFile.exists()) {
				val indicesJson = FileInputStream(IndicesFile).bufferedReader().readText()

				serializer.deserializeList(indicesJson, IndexConfig::class.java)?.let {
					indicesField = ArrayList(it)

					indicesField.forEach { index ->
						createDocumentsSearcherForIndex(index)
					}
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
			log.error("Could not persist indices to file $IndicesFile", e)
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


	protected open fun updateIndexDocuments(index: IndexConfig) = GlobalScope.launch {
		val contentExtractor = FileContentExtractor(FileContentExtractorSettings()) // TODO: use a common FileContentExtractor?

		LuceneDocumentsIndexer(getIndexPath(index)).use { indexer ->
			val stopwatch = Stopwatch()

			coroutineScope {
				index.directoriesToIndex.forEach { directoryToIndex ->
					withContext(Dispatchers.IO) {
						FilesystemWalker().walk(directoryToIndex.toPath()) { discoveredFile ->
							async(Dispatchers.IO) {
								extractContentAndIndex(discoveredFile, index, contentExtractor, indexer)
							}
						}
					}
				}
			}

			stopwatch.stopAndLog("Indexing documents", log)
		}
	}

	private suspend fun extractContentAndIndex(discoveredFile: Path, index: IndexConfig, contentExtractor: FileContentExtractor,
											   indexer: LuceneDocumentsIndexer) {
		try {
			val content = contentExtractor.extractContentSuspendable(discoveredFile.toFile()) ?: ""

			val document = createDocument(discoveredFile, content)

			indexer.indexSuspendable(document)

			indexUpdatedEventBus.onNext(index)
		} catch (e: Exception) {
			log.error("Could not extract file $discoveredFile", e)
		}
	}

	protected open fun createDocument(path: Path, content: String): Document {
		val file = path.toFile()
		val url = file.absolutePath
		val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

		return Document(
				url,
				url,
				content,
				file.length(),
				Date(attributes.creationTime().toMillis()),
				Date(attributes.lastModifiedTime().toMillis()),
				Date(attributes.lastAccessTime().toMillis())
		)
	}


	open suspend fun searchDocuments(searchTerm: String, indices: List<IndexConfig>): SearchResult {
		lastSearchCancellable?.cancel()

		lastSearchTerm = searchTerm

		val indexSearchers = indices.mapNotNull { documentsSearchers[it] }

		val jobs = indexSearchers.map { it.searchAsync(searchTerm) }

		lastSearchCancellable = JobsCancellable(jobs)

		val searchResults = jobs.map { it.await() }

		return mergeSearchResults(searchResults, indices)
	}

	protected open fun mergeSearchResults(searchResults: List<SearchResult>, indices: List<IndexConfig>): SearchResult {

		return if (searchResults.size == 1) searchResults[0] // only one index search -> return result directory, no need to merge
		else {
			SearchResult(
					searchResults.firstOrNull { it.hasError } == null,
					searchResults.map { it.error }.firstOrNull(),
					searchResults.flatMap { it.hits }
			)
		}
	}

	protected open fun getIndexPath(index: IndexConfig): File {
		return File(File(DataPath, "index"), index.id)
	}


	open fun updateAndSaveAppSettings(selectedIndex: IndexConfig?, searchAllIndices: Boolean) {
		appSettings.selectedIndexId = selectedIndex?.id
		appSettings.searchAllIndices = searchAllIndices

		saveAppSettings()
	}

	open fun saveAppSettings() {
		try {
			serializer.serializeObject(appSettings, AppSettingsFile)
		} catch (e: Exception) {
			log.error("Could not persist app settings to file $AppSettingsFile", e)
		}
	}

	protected open fun restoreAppSettings() {
		try {
			serializer.deserializeObject(AppSettingsFile, AppSettings::class.java)?.let {
				appSettings = it
			}
		} catch (e: Exception) {
			log.error("Could not deserialize app settings from $AppSettingsFile", e)
		}
	}

}