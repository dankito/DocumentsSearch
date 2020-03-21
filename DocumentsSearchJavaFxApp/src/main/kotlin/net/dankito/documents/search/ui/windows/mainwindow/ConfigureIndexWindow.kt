package net.dankito.documents.search.ui.windows.mainwindow

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import net.dankito.documents.search.model.IndexConfig
import net.dankito.utils.javafx.ui.dialogs.Window
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
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

    private val directory = SimpleStringProperty(index.directoriesToIndex.firstOrNull()?.absolutePath ?: "")

    private val isRequiredDataEntered = SimpleBooleanProperty(false)


    override val root = vbox {
        prefHeight = 125.0
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

        hbox {
            alignment = Pos.CENTER_LEFT

            label(messages["directory"]) {
                useMaxHeight = true
            }

            textfield(directory) {
                useMaxHeight = true

                textProperty().addListener { _, _, _ -> checkIfRequiredDataIsEntered() }

                hboxConstraints {
                    hGrow = Priority.ALWAYS

                    marginLeft = 6.0
                    marginRight = 6.0
                }
            }

            button(messages["..."]) {
                prefHeight = ButtonsHeight
                prefWidth = 60.0

                action { selectDirectory() }
            }

            vboxConstraints {
                marginTop = 6.0
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


    private fun selectDirectory() {
        val directoryChooser = DirectoryChooser()

        val currentDirectory = File(directory.value)

        if (currentDirectory.exists()) {
            directoryChooser.initialDirectory = currentDirectory
        }

        directoryChooser.showDialog(FX.primaryStage)?.let { selectedDirectory ->
            directory.value = selectedDirectory.absolutePath

            checkIfRequiredDataIsEntered()
        }
    }

    private fun checkIfRequiredDataIsEntered() {
        val selectedDirectory = File(directory.value)

        isRequiredDataEntered.value = name.value.isNotBlank() && selectedDirectory.exists()
    }


    private fun saveIndex() {
        index.name = name.value
        index.directoriesToIndex = listOf(File(directory.value))

        configuringIndexDone(index)

        close()
    }

}