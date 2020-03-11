package net.dankito.documents.search.ui.presenter

import io.reactivex.schedulers.Schedulers
import net.dankito.documents.contentextractor.FileContentExtractor
import net.dankito.documents.contentextractor.model.FileContentExtractorSettings
import net.dankito.documents.search.IDocumentsSearcher
import net.dankito.documents.search.LuceneDocumentsSearcher
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.filesystem.FilesystemWalker
import net.dankito.documents.search.index.LuceneDocumentsIndexer
import net.dankito.documents.search.model.Cancellable
import net.dankito.documents.search.model.Document
import net.dankito.utils.ThreadPool
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Date


open class DocumentsSearchPresenter {

	protected val threadPool = ThreadPool()

	protected val documentsSearcher: IDocumentsSearcher = LuceneDocumentsSearcher(threadPool)

	protected var lastSearchCancellable: Cancellable? = null


	init {
		indexDocuments() // TODO: remove again
	}

	private fun indexDocuments() {
		val indexer = LuceneDocumentsIndexer()
		val filesystemWalker = FilesystemWalker()
		val contentExtractor = FileContentExtractor(FileContentExtractorSettings())

		filesystemWalker.walk(Paths.get("/home/ganymed/data/docs/"))
				.parallel(10)
				.runOn(Schedulers.io())
				.map { createDocument(it, contentExtractor) }
				.doOnNext { indexer.index(it) }
				.sequential().subscribe()

	}

	private fun createDocument(path: Path, contentExtractor: FileContentExtractor): Document {
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


	open fun searchDocumentsAsync(searchTerm: String, callback: (SearchResult) -> Unit) {
		lastSearchCancellable?.cancel()

		lastSearchCancellable = documentsSearcher.searchAsync(searchTerm, callback)
	}

}