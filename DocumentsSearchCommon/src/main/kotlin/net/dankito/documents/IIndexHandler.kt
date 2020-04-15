package net.dankito.documents

import net.dankito.documents.search.index.IDocumentsIndexer
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexPartConfig


interface IIndexHandler<T : IndexPartConfig> {

    suspend fun updateIndexPartElements(index: IndexConfig, indexPart: T,
                                        currentItemsInIndex: MutableMap<String, DocumentMetadata>, indexer: IDocumentsIndexer)

    fun listenForChangesToIndexedItems(index: IndexConfig, indexPart: T, indexer: IDocumentsIndexer)

}