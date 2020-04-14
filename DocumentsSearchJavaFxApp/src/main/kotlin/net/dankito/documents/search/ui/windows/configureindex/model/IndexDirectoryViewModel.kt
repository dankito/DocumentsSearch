package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
import net.dankito.documents.search.model.IndexedDirectoryConfig
import net.dankito.utils.javafx.ui.extensions.runNonBlockingDispatchToUiThread
import org.slf4j.LoggerFactory
import tornadofx.*


class IndexDirectoryViewModel(config: IndexedDirectoryConfig, private val filesToIndexFinder: FilesToIndexFinder)
    : ItemViewModel<IndexedDirectoryConfig>(config) {

    companion object {
        const val DeterminingCountElements = -1

        private val logger = LoggerFactory.getLogger(IndexDirectoryViewModel::class.java)
    }


    val path = SimpleStringProperty(item.directory.absolutePath)

    val countElements = SimpleIntegerProperty(DeterminingCountElements)


    init {
        itemProperty.addListener { _, _, newValue ->
            path.value = newValue.directory.absolutePath
            updateCountElementsInIndexPart(newValue)
        }

        updateCountElementsInIndexPart(config)
    }


    private fun updateCountElementsInIndexPart(config: IndexedDirectoryConfig) {
        countElements.value = DeterminingCountElements

        runNonBlockingDispatchToUiThread("Could not get count files in directory '$config'", logger,
                { filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(config.directory))}) { includesAndExcludes ->
            countElements.value = includesAndExcludes.first.size
        }
    }

}