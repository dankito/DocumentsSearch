package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
import net.dankito.documents.search.model.IndexedDirectoryConfig
import net.dankito.utils.javafx.ui.extensions.runNonBlockingDispatchToUiThread
import org.slf4j.LoggerFactory


class IndexedDirectoryViewModel(config: IndexedDirectoryConfig, private val filesToIndexFinder: FilesToIndexFinder)
    : IndexPartViewModel<IndexedDirectoryConfig>(config) {

    companion object {
        private val logger = LoggerFactory.getLogger(IndexedDirectoryViewModel::class.java)
    }


    override val displayName = SimpleStringProperty(item.directory.absolutePath)


    init {
        itemProperty.addListener { _, _, newValue ->
            displayName.value = newValue.directory.absolutePath
            updateCountItemsInIndexPart(newValue)
        }

        updateCountItemsInIndexPart(config)
    }


    private fun updateCountItemsInIndexPart(config: IndexedDirectoryConfig?) {
        countItemsInIndexPart.value = DeterminingCountItems

        if (config != null) {
            runNonBlockingDispatchToUiThread("Could not get count files in directory '$config'", logger,
                    { filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(config.directory))}) { includesAndExcludes ->
                countItemsInIndexPart.value = includesAndExcludes.first.size
            }
        }
    }

}