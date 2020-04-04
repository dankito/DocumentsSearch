package net.dankito.documents.search.ui.presenter

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import net.dankito.documents.contentextractor.FileContentExtractionResult
import net.dankito.documents.contentextractor.FileContentExtractor
import net.dankito.documents.contentextractor.IFileChecksumCalculator
import net.dankito.documents.contentextractor.Sha512FileChecksumCalculator
import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.documents.language.OptimaizeLanguageDetector
import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.LuceneDocumentsSearcher
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.filesystem.FilesystemWalker
import net.dankito.documents.search.index.LuceneDocumentsIndexer
import net.dankito.documents.search.model.*
import net.dankito.documents.search.ui.model.AppSettings
import net.dankito.utils.Stopwatch
import net.dankito.utils.io.FileUtils
import net.dankito.utils.serialization.ISerializer
import net.dankito.utils.serialization.JacksonJsonSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet


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

	protected val filesystemWalker = FilesystemWalker()

	protected var contentExtractor: FileContentExtractor? = null

	protected val fileChecksumCalculator: IFileChecksumCalculator = Sha512FileChecksumCalculator()

	protected val languageDetector = OptimaizeLanguageDetector()


	protected val indicesBeingUpdatedField = CopyOnWriteArraySet<IndexConfig>()

	protected val indexUpdatedEventBus = PublishSubject.create<IndexConfig>()

	protected val serializer: ISerializer = JacksonJsonSerializer()


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

		(getSearcherForIndex(index) as? AutoCloseable)?.close()

		documentsSearchers.remove(index)

		FileUtils().deleteFolderRecursively(getIndexPath(index))
	}

	protected open fun createDocumentsSearcherForIndex(index: IndexConfig) {
		documentsSearchers[index] = LuceneDocumentsSearcher(getIndexPath(index))
	}


	open fun updateIndexDocuments(index: IndexConfig, doneCallback: (() -> Unit)? = null) = GlobalScope.launch {
		indicesBeingUpdatedField.add(index) // TODO: check if index is already been updated

		val contentExtractor = getFileContentExtractor()
		val currentFilesInIndex = getAllDocumentMetadataForIndex(index)

		LuceneDocumentsIndexer(getIndexPath(index), languageDetector).use { indexer ->
			val stopwatch = Stopwatch()

			updateIndexDirectoriesDocuments(index, currentFilesInIndex, contentExtractor, indexer)

			stopwatch.stopAndLog("Indexing ${index.name}", log)

			indicesBeingUpdatedField.remove(index)

			indexer.optimizeIndex()

			doneCallback?.invoke()
		}
	}

	protected open suspend fun updateIndexDirectoriesDocuments(index: IndexConfig, currentFilesInIndex: MutableMap<String, DocumentMetadata>, contentExtractor: FileContentExtractor, indexer: LuceneDocumentsIndexer) {
		coroutineScope {
			index.directoriesToIndex.forEach { directoryToIndex ->
				withContext(Dispatchers.IO) {
					filesystemWalker.walk(directoryToIndex.toPath()) { discoveredFile ->
						val file = discoveredFile.toFile()
						val url = file.absolutePath
						val attributes = Files.readAttributes(discoveredFile, BasicFileAttributes::class.java) // TODO: take file attributes from Filesystem Walk

						async(Dispatchers.IO) {
							if (isNewOrUpdatedFile(file, url, attributes, currentFilesInIndex)) {
								extractContentAndIndex(file, url, attributes, index, contentExtractor, indexer)
							}

							currentFilesInIndex.remove(url)
						}
					}
				}
			}
		}
	}

	protected open fun isNewOrUpdatedFile(file: File, url: String, attributes: BasicFileAttributes, currentFilesInIndex: Map<String, DocumentMetadata>): Boolean {
		val metadata = currentFilesInIndex[url]

		if (metadata == null) {
			return true // a new file
		}

		val isUpdated = file.length() != metadata.fileSize
				|| attributes.lastModifiedTime().toMillis() != metadata.lastModified.time
				|| metadata.checksum != calculateFileChecksum(file)

		return isUpdated
	}

	protected open fun getFileContentExtractor(): FileContentExtractor {
		contentExtractor?.let {
			return it
		}

		val newFileContentExtractor = FileContentExtractor(FileContentExtractorSettings())

		this.contentExtractor = newFileContentExtractor

		return newFileContentExtractor
	}

	protected open suspend fun extractContentAndIndex(file: File, url: String, attributes: BasicFileAttributes, index: IndexConfig,
													  contentExtractor: FileContentExtractor, indexer: LuceneDocumentsIndexer) {
		try {
			val result = contentExtractor.extractContentSuspendable(file)

			val document = createDocument(file, url, attributes, result)

			indexer.indexSuspendable(document)

			indexUpdatedEventBus.onNext(index)
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

	protected open fun calculateFileChecksum(file: File): String {
		return fileChecksumCalculator.calculateChecksum(file)
	}


	open suspend fun searchDocuments(searchTerm: String, indices: List<IndexConfig>): SearchResult {
		lastSearchCancellable?.cancel()

		lastSearchTerm = searchTerm

		val indexSearchers = indices.mapNotNull { getSearcherForIndex(it) }

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


	open suspend fun getDocumentSuspendable(index: IndexConfig, metadata: DocumentMetadata): Document? {
		return getSearcherForIndex(index)?.getDocumentSuspendable(metadata)
	}

	open suspend fun getAllDocumentMetadataForIndex(index: IndexConfig): MutableMap<String, DocumentMetadata> {
		return getSearcherForIndex(index)?.getAllDocumentMetadataForIndex(index)?.associateBy { it.url }?.toMutableMap()
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