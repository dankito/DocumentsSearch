package net.dankito.documents.search.index

import net.dankito.documents.search.model.Document


interface IDocumentsIndexer {

	fun index(documentToIndex: Document)

}