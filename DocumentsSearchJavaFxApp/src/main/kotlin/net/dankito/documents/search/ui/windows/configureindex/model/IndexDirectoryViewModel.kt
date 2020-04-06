package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
import net.dankito.utils.javafx.ui.extensions.runNonBlockingDispatchToUiThread
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File


class IndexDirectoryViewModel(indexDirectory: File, private val filesToIndexFinder: FilesToIndexFinder)
    : ItemViewModel<File>(indexDirectory) {

    companion object {
        const val DeterminingCountFiles = -1

        private val logger = LoggerFactory.getLogger(IndexDirectoryViewModel::class.java)
    }


    val path = bind(File::getAbsolutePath) as SimpleStringProperty

    val countFiles = SimpleIntegerProperty(DeterminingCountFiles)


    init {
        itemProperty.addListener { _, _, newValue -> updateCountFiles(newValue) }

        updateCountFiles(indexDirectory)
    }


    private fun updateCountFiles(indexDirectory: File) {
        countFiles.value = DeterminingCountFiles

        runNonBlockingDispatchToUiThread("Could not get count files in directory '$indexDirectory'", logger,
                { filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(indexDirectory))}) { includesAndExcludes ->
            countFiles.value = includesAndExcludes.first.size
        }
    }

}