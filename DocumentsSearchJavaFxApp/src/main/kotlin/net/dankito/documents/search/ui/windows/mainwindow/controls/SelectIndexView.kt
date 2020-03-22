package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.configureindex.ConfigureIndexWindow
import net.dankito.utils.javafx.ui.controls.EditEntityButton
import net.dankito.utils.javafx.ui.controls.editEntityButton
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import tornadofx.*
import java.io.File


class SelectIndexView(
        private val presenter: DocumentsSearchPresenter,
        private val selectedIndexChanged: ((IndexConfig?) -> Unit)? = null
) : View() {

    private val availableIndices = FXCollections.observableArrayList(presenter.indices)

    private val selectedIndex = SimpleObjectProperty<IndexConfig>()

    private var editIndexButton: EditEntityButton by singleAssign()


    val currentSelectedIndex: IndexConfig?
        get() = selectedIndex.value


    init {
        if (presenter.indices.isNotEmpty()) {
            selectedIndex.value = presenter.selectedIndex
        }

        selectedIndex.addListener { _, _, newValue -> selectedIndexChanged?.invoke(newValue) }
    }


    override val root = hbox {
        fixedHeight = 40.0
        useMaxWidth = true
        alignment = Pos.CENTER_LEFT

        paddingTop = 4.0
        paddingBottom = 4.0

        label(messages["index"]) {
            useMaxHeight = true
            prefWidth = SearchDocumentsView.LabelsWidth
        }

        combobox(selectedIndex, availableIndices) {
            useMaxHeight = true
            useMaxWidth = true

            cellFormat { text = it.name }

            hboxConstraints {
                hGrow = Priority.ALWAYS

                marginLeft = 6.0
                marginRight = 6.0
            }
        }

        editIndexButton = editEntityButton( { configureIndex(selectedIndex.value) }, { configureIndex(null) }, { deleteIndex(selectedIndex.value) } ) {
            useMaxHeight = true
            prefWidth = 135.0

            this.showOnlyCreateOperation = availableIndices.isEmpty()
        }
    }


    private fun deleteIndex(indexToDelete: IndexConfig?) {
        indexToDelete?.let {
            // TODO: ask user if she really likes to delete index?
            presenter.removeIndex(indexToDelete)

            if (selectedIndex.value == indexToDelete) {
                selectedIndex.value = availableIndices.firstOrNull()
            }

            updateAvailableIndicesAndEditIndexButtonItems()
        }
    }

    private fun configureIndex(index: IndexConfig?) {
        val indexToConfigure = index ?: IndexConfig("", listOf())

        val previousDirectoriesToIndex = ArrayList(indexToConfigure.directoriesToIndex)

        ConfigureIndexWindow(indexToConfigure) { configuredIndex ->
            indexHasBeenConfigured(configuredIndex, previousDirectoriesToIndex)
        }.show(messages["configure.index.window.title"])
    }

    private fun indexHasBeenConfigured(configuredIndex: IndexConfig, previousDirectoriesToIndex: ArrayList<File>) {
        presenter.saveOrUpdateIndex(configuredIndex, didIndexDocumentsChange(configuredIndex, previousDirectoriesToIndex))

        selectedIndex.value = configuredIndex

        updateAvailableIndicesAndEditIndexButtonItems()
    }

    private fun didIndexDocumentsChange(configuredIndex: IndexConfig, previousDirectoriesToIndex: List<File>): Boolean {
        if (configuredIndex.directoriesToIndex.size == previousDirectoriesToIndex.size) {
            val containsAllPreviousFiles = ArrayList(configuredIndex.directoriesToIndex)
            containsAllPreviousFiles.removeAll(previousDirectoriesToIndex)

            return containsAllPreviousFiles.isNotEmpty()
        }

        return true
    }

    private fun updateAvailableIndicesAndEditIndexButtonItems() {
        availableIndices.setAll(presenter.indices)

        editIndexButton.showOnlyCreateOperation = availableIndices.isEmpty()
    }

}