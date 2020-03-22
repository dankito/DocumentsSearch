package net.dankito.documents.search.ui.windows.mainwindow.model

import net.dankito.documents.search.model.Document
import tornadofx.*


class DocumentViewModel : ItemViewModel<Document>() {

	val filename = bind(Document::filename)

	val url = bind(Document::url)

}