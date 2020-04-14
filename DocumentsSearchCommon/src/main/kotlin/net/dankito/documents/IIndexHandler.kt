package net.dankito.documents

import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexedDirectoryConfig
import java.io.File
import java.nio.file.attribute.BasicFileAttributes


interface IIndexHandler {

    suspend fun updateIndexDirectoriesDocuments(index: IndexConfig, directoryConfig: IndexedDirectoryConfig,
                                                     currentFilesInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer)

    suspend fun extractContentAndIndex(file: File, url: String, attributes: BasicFileAttributes, indexer: IDocumentsIndexer)

    fun listenForChangesToIndexedItems(index: IndexConfig, indexedDirectory: IndexedDirectoryConfig, indexer: IDocumentsIndexer)

}