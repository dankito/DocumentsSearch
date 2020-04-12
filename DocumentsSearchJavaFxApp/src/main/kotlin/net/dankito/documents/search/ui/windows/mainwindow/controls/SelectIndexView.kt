package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.configureindex.ConfigureIndexWindow
import net.dankito.utils.javafx.ui.controls.EditEntityButton
import net.dankito.utils.javafx.ui.controls.editEntityButton
import net.dankito.utils.javafx.ui.dialogs.JavaFXDialogService
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.ui.dialogs.ConfirmationDialogButton
import tornadofx.*


class SelectIndexView(
        private val presenter: DocumentsSearchPresenter,
        private val selectedIndexChanged: ((IndexConfig?) -> Unit)? = null
) : View() {

    private val availableIndices = FXCollections.observableArrayList(presenter.indices)

    private val selectedIndex = SimpleObjectProperty<IndexConfig>()

    private val canUpdateSelectedIndex = SimpleBooleanProperty(false)


    private var editIndexButton: EditEntityButton by singleAssign()


    private val dialogService = JavaFXDialogService()


    val currentSelectedIndex: IndexConfig?
        get() = selectedIndex.value


    init {
        if (presenter.indices.isNotEmpty()) {
            selectedIndex.value = presenter.selectedIndex
        }

        selectedIndex.addListener { _, _, newValue ->
            updateCanUpdateSelectedIndex()
            selectedIndexChanged?.invoke(newValue)
        }

        updateCanUpdateSelectedIndex()
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

        editIndexButton = editEntityButton( { configureIndex(selectedIndex.value) }, { configureIndex(null) }, { askUserToDeleteIndex(selectedIndex.value) } ) {
            useMaxHeight = true
            prefWidth = 150.0

            this.showOnlyCreateOperation = availableIndices.isEmpty()
        }

        button(messages["main.window.update.index"]) {
            useMaxHeight = true
            prefWidth = 150.0

            enableWhen(canUpdateSelectedIndex)

            action { updateSelectedIndex() }

            hboxConstraints {
                marginLeft = 6.0
            }
        }
    }


    private fun updateSelectedIndex() {
        currentSelectedIndex?.let { index ->
            if (presenter.doesIndexCurrentlyGetUpdated(index) == false) {
                presenter.updateIndexDocuments(index) {
                    updateCanUpdateSelectedIndex()
                }

                updateCanUpdateSelectedIndex()
            }
        }
    }

    private fun updateCanUpdateSelectedIndex() {
        val selectedIndex = currentSelectedIndex

        canUpdateSelectedIndex.value = selectedIndex != null && presenter.doesIndexCurrentlyGetUpdated(selectedIndex)== false
    }


    private fun askUserToDeleteIndex(indexToDelete: IndexConfig?) {
        indexToDelete?.let {
            dialogService.showConfirmationDialog(String.format(messages["configure.index.window.ask.user.delete.index"], indexToDelete.name)) { selectedOption ->
                if (selectedOption == ConfirmationDialogButton.Confirm) {
                    deleteIndex(indexToDelete)
                }
            }
        }
    }

    private fun deleteIndex(indexToDelete: IndexConfig) {
        val isSelectedIndexDeleted = selectedIndex.value == indexToDelete
        val deletedIndexListIndex = availableIndices.indexOf(indexToDelete)

        presenter.removeIndex(indexToDelete)

        updateAvailableIndicesAndEditIndexButtonItems()

        if (isSelectedIndexDeleted) {
            val nextIndexInList = deletedIndexListIndex - 1
            selectedIndex.value = if (nextIndexInList < availableIndices.size && nextIndexInList >= 0) availableIndices[nextIndexInList]
            else availableIndices.firstOrNull()
        }
    }

    private fun configureIndex(index: IndexConfig?) {
        val indexToConfigure = index ?: IndexConfig("", listOf())

        ConfigureIndexWindow(indexToConfigure) { configuredIndex ->
            indexHasBeenConfigured(configuredIndex)
        }.show(messages["configure.index.window.title"])
    }

    private fun indexHasBeenConfigured(configuredIndex: IndexConfig) {
        presenter.saveOrUpdateIndex(configuredIndex)

        selectedIndex.value = configuredIndex

        updateAvailableIndicesAndEditIndexButtonItems()
    }

    private fun updateAvailableIndicesAndEditIndexButtonItems() {
        availableIndices.setAll(presenter.indices)

        editIndexButton.showOnlyCreateOperation = availableIndices.isEmpty()
    }

}