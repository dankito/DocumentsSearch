package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.search.filesystem.FilesystemWalker
import tornadofx.*
import java.io.File


class IndexDirectoryViewItem(indexDirectory: File) : ItemViewModel<File>(indexDirectory) {

    val path = bind(File::getAbsolutePath) as SimpleStringProperty

    // TODO: determine count files asynchronously to not block UI thread
    val countFiles = SimpleIntegerProperty(FilesystemWalker().listFiles(item.toPath()).size) // TODO: implement binding to update value when item changes

}