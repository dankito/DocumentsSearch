package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
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


    private fun updateCountFiles(indexDirectory: File) = GlobalScope.launch {
        try {
            countFiles.value = DeterminingCountFiles

            val includesAndExcludes = filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(indexDirectory))
            val countFilesInDirectory = includesAndExcludes.first.size

            withContext(Dispatchers.JavaFx) {
                countFiles.value = countFilesInDirectory
            }
        } catch (e: Exception) {
            logger.error("Could not get count files in directory '$indexDirectory'", e)
        }
    }

}