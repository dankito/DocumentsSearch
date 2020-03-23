package net.dankito.documents.search.ui.windows.mainwindow.model

import net.dankito.documents.search.model.DocumentMetadata
import tornadofx.*


class DocumentMetadataViewModel : ItemViewModel<DocumentMetadata>() {

	val filename = bind(DocumentMetadata::filename)

	val url = bind(DocumentMetadata::url)

}