package net.dankito.documents.search.ui.windows.mainwindow

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.SelectionMode
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.ui.windows.mainwindow.model.IndexDirectoryViewItem
import net.dankito.utils.javafx.ui.controls.addButton
import net.dankito.utils.javafx.ui.dialogs.Window
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import net.dankito.utils.javafx.ui.extensions.initiallyUseRemainingSpace
import tornadofx.*
import java.io.File


class ConfigureIndexWindow(
        private val index: IndexConfig,
        private val configuringIndexDone: (IndexConfig) -> Unit
) : Window() {


    companion object {
        private val ButtonsHeight = 35.0

        private val ButtonsWidth = 150.0
    }


    private val name = SimpleStringProperty(index.name)

    private val directories = FXCollections.observableArrayList<IndexDirectoryViewItem>(index.directoriesToIndex.map { mapToIndexDirectoryViewItem(it) })

    private var lastSelectedDirectory: File? = index.directoriesToIndex.firstOrNull()

    private val isRequiredDataEntered = SimpleBooleanProperty(false)


    override val root = vbox {
        prefHeight = 250.0
        prefWidth = 450.0

        hbox {
            fixedHeight = 34.0
            alignment = Pos.CENTER_LEFT

            label(messages["name"]) {
                useMaxHeight = true
            }

            textfield(name) {
                useMaxHeight = true

                textProperty().addListener { _, _, _ -> checkIfRequiredDataIsEntered() }

                hboxConstraints {
                    hGrow = Priority.ALWAYS

                    marginLeft = 6.0
                }
            }
        }

        anchorpane {
            fixedHeight = 34.0
            alignment = Pos.CENTER_LEFT

            label(messages["directories"]) {
                useMaxHeight = true

                anchorpaneConstraints {
                    topAnchor = 0.0
                    leftAnchor = 0.0
                    bottomAnchor = 0.0
                }
            }

            addButton {
                action { addDirectory() }

                anchorpaneConstraints {
                    topAnchor = 0.0
                    rightAnchor = 0.0
                    bottomAnchor = 0.0
                }
            }

            vboxConstraints {
                marginTop = 6.0
                marginBottom = 4.0
            }
        }

        tableview<IndexDirectoryViewItem>(directories) {
            column<IndexDirectoryViewItem, String>(messages["configure.index.window.path.column.name"], IndexDirectoryViewItem::path) {
                this.initiallyUseRemainingSpace(this@tableview)
            }


            prefHeight = 100.0

            selectionModel.selectionMode = SelectionMode.SINGLE

            selectionModel.selectedItemProperty().addListener { _, _, newValue -> selectedIndexDirectoryChanged(newValue) }

            setOnKeyReleased { event -> indexDirectoryTableKeyPressed(event, selectionModel.selectedItem) }

            vboxConstraints {
                vGrow = Priority.ALWAYS

                marginBottom = 12.0
            }
        }

        anchorpane {
            fixedHeight = ButtonsHeight

            button(messages["cancel"]) {
                fixedWidth = ButtonsWidth

                isCancelButton = true

                action { close() }

                anchorpaneConstraints {
                    topAnchor = 0.0
                    rightAnchor = ButtonsWidth + 12.0
                    bottomAnchor = 0.0
                }
            }

            button(messages["ok"]) {
                fixedWidth = ButtonsWidth

                isDefaultButton = true

                enableWhen(isRequiredDataEntered)

                action { saveIndex() }

                anchorpaneConstraints {
                    topAnchor = 0.0
                    rightAnchor = 0.0
                    bottomAnchor = 0.0
                }
            }
        }
    }


    private fun mapToIndexDirectoryViewItem(indexDirectory: File): IndexDirectoryViewItem {
        return IndexDirectoryViewItem(indexDirectory)
    }

    private fun selectedIndexDirectoryChanged(selectedIndexDirectory: IndexDirectoryViewItem?) {
        selectedIndexDirectory?.let {

        }
    }

    private fun indexDirectoryTableKeyPressed(event: KeyEvent, selectedIndexDirectory: IndexDirectoryViewItem?) {
        selectedIndexDirectory?.let {
            if (event.code == KeyCode.DELETE) {
                directories.remove(selectedIndexDirectory)

                checkIfRequiredDataIsEntered()
            }
        }
    }


    private fun addDirectory() {
        val directoryChooser = DirectoryChooser()

        lastSelectedDirectory?.let { lastSelectedDirectory ->
            if (lastSelectedDirectory.exists()) {
                directoryChooser.initialDirectory = lastSelectedDirectory
            }
        }

        directoryChooser.showDialog(FX.primaryStage)?.let { selectedDirectory ->
            directories.add(mapToIndexDirectoryViewItem(selectedDirectory))

            lastSelectedDirectory = selectedDirectory

            checkIfRequiredDataIsEntered()
        }
    }

    private fun checkIfRequiredDataIsEntered() {
        isRequiredDataEntered.value = name.value.isNotBlank() && directories.isNotEmpty()
    }


    private fun saveIndex() {
        index.name = name.value
        index.directoriesToIndex = directories.map { File(it.path.value) }

        configuringIndexDone(index)

        close()
    }

}