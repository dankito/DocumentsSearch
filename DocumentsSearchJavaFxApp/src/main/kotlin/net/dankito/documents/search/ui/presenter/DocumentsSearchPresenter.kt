package net.dankito.documents.search.ui.presenter

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.dankito.documents.IIndexHandler
import net.dankito.documents.contentextractor.FileContentExtractor
import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.documents.filesystem.FileSystemIndexHandler
import net.dankito.documents.language.OptimaizeLanguageDetector
import net.dankito.documents.mail.MailAccountIndexHandler
import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.LuceneDocumentsIndexer
import net.dankito.documents.search.LuceneDocumentsSearcher
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.*
import net.dankito.documents.search.ui.model.AppSettings
import net.dankito.utils.Stopwatch
import net.dankito.utils.filesystem.watcher.FileSystemWatcherJava
import net.dankito.utils.hashing.HashService
import net.dankito.utils.io.FileUtils
import net.dankito.utils.serialization.ISerializer
import net.dankito.utils.serialization.JacksonJsonSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger


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

	protected val documentIndexer = ConcurrentHashMap<IndexConfig, Pair<IDocumentsIndexer, AtomicInteger>>()

	protected val languageDetector = OptimaizeLanguageDetector()


	protected val indicesBeingUpdatedField = CopyOnWriteArraySet<IndexConfig>()

	protected val indexUpdatedEventBus = PublishSubject.create<IndexConfig>()

	protected val serializer: ISerializer = JacksonJsonSerializer()


	protected val fileContentExtractor = FileContentExtractor(FileContentExtractorSettings())

	protected val fileSystemIndexHandler: IIndexHandler<IndexedDirectoryConfig> = FileSystemIndexHandler(fileContentExtractor, FileSystemWatcherJava(), indexUpdatedEventBus)

	protected val mailAccountIndexHandler: IIndexHandler<IndexedMailAccountConfig> = MailAccountIndexHandler(fileContentExtractor, HashService(), indexUpdatedEventBus)


	open var appSettings: AppSettings = AppSettings()
		protected set

	open val indices: List<IndexConfig>
		get() = ArrayList(indicesField) // make a copy

	open val selectedIndex: IndexConfig?
		get() = indices.firstOrNull { it.id == appSettings.selectedIndexId }

	open var lastSearchTerm: String = ""
		protected set

	open val indicesBeingUpdated: List<IndexConfig>
		get() = ArrayList(indicesBeingUpdatedField) // don't pass mutable list to outside

	open fun doesIndexCurrentlyGetUpdated(index: IndexConfig): Boolean {
		return indicesBeingUpdatedField.contains(index)
	}


	init {
		restoreAppSettings()
		restoreIndices()

		ensureIndicesAreAndStayUpToDate()
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


	open fun saveOrUpdateIndex(index: IndexConfig) {
		if (index.isIdSet == false) { // a new index
			saveIndex(index)
		}
		else {
			updateIndex(index)
		}
	}

	open fun saveIndex(index: IndexConfig) {
		index.id = UUID.randomUUID().toString()

		indicesField.add(index)

		persistIndices()

		createDocumentsSearcherForIndex(index)

		listenForChangesToIndexedItems(index)

		updateIndexDocuments(index)
	}

	open fun updateIndex(index: IndexConfig) {
		persistIndices()

		updateIndexDocuments(index)
	}

	open fun removeIndex(index: IndexConfig) {
		indicesField.remove(index)

		persistIndices()

		(getSearcherForIndex(index) as? AutoCloseable)?.close()

		documentsSearchers.remove(index)

		FileUtils().deleteFolderRecursively(getIndexPath(index))
	}

	protected open fun createDocumentsSearcherForIndex(index: IndexConfig) {
		documentsSearchers[index] = LuceneDocumentsSearcher(getIndexPath(index))
	}


	protected open fun ensureIndicesAreAndStayUpToDate() {
		indices.forEach { index ->
			updateIndexDocuments(index)

			listenForChangesToIndexedItems(index)
		}
	}

	protected open fun listenForChangesToIndexedItems(index: IndexConfig) {
		index.indexParts.forEach { indexPart ->
			getIndexHandler(indexPart).listenForChangesToIndexedItems(index, indexPart, getIndexerForIndex(index))
		}
	}


	open fun updateIndexDocuments(index: IndexConfig, doneCallback: (() -> Unit)? = null) = GlobalScope.launch {
		indicesBeingUpdatedField.add(index) // TODO: check if index is already being updated

		val currentItemsInIndex = ConcurrentHashMap(getAllDocumentMetadataForIndex(index))

		val indexer = getIndexerForIndex(index)

		val stopwatch = Stopwatch()

		updateIndexDirectoriesDocuments(index, currentItemsInIndex, indexer)

		deleteRemovedFilesFromIndex(currentItemsInIndex, indexer) // all files that are now still in currentItemsInIndex have been deleted

		stopwatch.stopAndLog("Indexing ${index.name}", log)

		indicesBeingUpdatedField.remove(index)

		indexer.optimizeIndex()

		doneCallback?.invoke()

		releaseIndexer(index)
	}

	protected open fun getIndexerForIndex(index: IndexConfig): IDocumentsIndexer {
		documentIndexer[index]?.let { indexerCountPair ->
			indexerCountPair.second.incrementAndGet()

			return indexerCountPair.first
		}

		val indexer = LuceneDocumentsIndexer(getIndexPath(index), languageDetector)
		val count = AtomicInteger(1)
		documentIndexer.put(index, Pair(indexer, count))

		return indexer
	}

	protected open fun releaseIndexer(index: IndexConfig) {
		documentIndexer[index]?.let { indexerCountPair ->
			val count = indexerCountPair.second.decrementAndGet()

			if (count == 0) {
				documentIndexer.remove(index)

				(indexerCountPair.first as? AutoCloseable)?.close()
			}
		}
	}

	protected open suspend fun updateIndexDirectoriesDocuments(index: IndexConfig, currentItemsInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer) {
		coroutineScope {
			index.indexParts.forEach { indexPart ->
				getIndexHandler(indexPart).updateIndexPartItems(index, indexPart, currentItemsInIndex, indexer)
			}
		}
	}

	protected open fun <T : IndexPartConfig> getIndexHandler(indexPart: T): IIndexHandler<T> {
		if (indexPart is IndexedMailAccountConfig) {
			return mailAccountIndexHandler as IIndexHandler<T>
		}

		return fileSystemIndexHandler as IIndexHandler<T>
	}

	protected open fun deleteRemovedFilesFromIndex(deletedItems: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer) {
		deletedItems.values.forEach { metadata ->
			log.debug("Removing file from index: {}", metadata)

			indexer.remove(metadata)
		}
	}


	open suspend fun searchDocuments(searchTerm: String, indices: List<IndexConfig>): SearchResult {
		lastSearchCancellable?.cancel()

		lastSearchTerm = searchTerm

		val indexSearchers = indices.mapNotNull { getSearcherForIndex(it) }

		val jobs = indexSearchers.map { it.searchAsync(searchTerm) }

		lastSearchCancellable = JobsCancellable(jobs)

		val searchResults = jobs.awaitAll()

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


	open fun getDocument(index: IndexConfig, metadata: DocumentMetadata): Document? {
		return getSearcherForIndex(index)?.getDocument(metadata)
	}

	open fun getAllDocumentMetadataForIndex(index: IndexConfig): MutableMap<String, DocumentMetadata> {
		return getSearcherForIndex(index)?.getAllDocumentMetadataForIndex(index)?.associateBy { it.id }?.toMutableMap()
				?: mutableMapOf()
	}

	open fun getSearcherForIndex(index: IndexConfig): IDocumentsSearcher? {
		return documentsSearchers[index]
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