package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import java.io.File


class IndexDirectoryViewItem(indexDirectory: File) : ItemViewModel<File>(indexDirectory) {

    val path = bind(File::getAbsolutePath) as SimpleStringProperty

}