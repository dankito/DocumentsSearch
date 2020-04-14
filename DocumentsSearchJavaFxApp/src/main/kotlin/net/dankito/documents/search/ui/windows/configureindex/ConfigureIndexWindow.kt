package net.dankito.documents.search.ui.windows.configureindex

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.OverrunStyle
import javafx.scene.control.SelectionMode
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import net.dankito.documents.filesystem.ExcludedFile
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexedDirectoryConfig
import net.dankito.documents.search.ui.windows.configureindex.controls.AdvancedConfigurationView
import net.dankito.documents.search.ui.windows.configureindex.controls.ConfiguredIndexPreview
import net.dankito.documents.search.ui.windows.configureindex.model.IndexDirectoryViewModel
import net.dankito.utils.javafx.ui.controls.addButton
import net.dankito.utils.javafx.ui.controls.okCancelButtonBar
import net.dankito.utils.javafx.ui.controls.removeButton
import net.dankito.utils.javafx.ui.dialogs.Window
import net.dankito.utils.javafx.ui.extensions.*
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean


class ConfigureIndexWindow(
        private val index: IndexConfig,
        private val configuringIndexDone: (IndexConfig) -> Unit
) : Window() {


    companion object {
        private val AddRemoveButtonsHeight = 34.0

        private val AddRemoveButtonsWidth = 40.0

        private val SpaceBetweenAddAndRemoveButtons = 6.0

        private val ButtonsHeight = 35.0

        private val ButtonsWidth = 150.0

        private val logger = LoggerFactory.getLogger(ConfigureIndexWindow::class.java)
    }


    private val filesToIndexFinder = FilesToIndexFinder()

    protected var stopFindingFilesToIndex: AtomicBoolean? = null


    private val name = SimpleStringProperty(index.name)

    private val indexDirectories = FXCollections.observableArrayList<IndexDirectoryViewModel>(index.directoriesToIndex.map { mapToIndexDirectoryViewItem(it) })

    private val selectedIndexDirectory = SimpleObjectProperty<IndexDirectoryViewModel>(null)

    private val isAIndexDirectorySelected = SimpleBooleanProperty(false)

    private var lastSelectedDirectory: IndexedDirectoryConfig? = index.directoriesToIndex.firstOrNull()

    private val isRequiredDataEntered = SimpleBooleanProperty(false)


    private val advancedConfigurationView = AdvancedConfigurationView() { checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview() }

    private val configuredIndexPreview = ConfiguredIndexPreview()


    override val root = vbox {
        prefHeight = 700.0
        prefWidth = 1250.0

        hbox {
            fixedHeight = 34.0
            alignment = Pos.CENTER_LEFT

            label(messages["configure.index.window.index.name"]) {
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

        splitpane {
            vboxConstraints {
                marginTop = 6.0
                marginBottom = 6.0
            }

            splitpane(Orientation.VERTICAL) {
                SplitPane.setResizableWithParent(this, false)

                prefWidth = 350.0

                // TODO: extract to own View
                vbox {
                    prefHeight = AddRemoveButtonsHeight + 120.0 // 120.0 = TableView pref height

                    anchorpane {
                        fixedHeight = AddRemoveButtonsHeight
                        alignment = Pos.CENTER_LEFT

                        label(messages["configure.index.window.directories.to.index"]) {
                            useMaxHeight = true

                            this.textOverrun = OverrunStyle.ELLIPSIS

                            anchorpaneConstraints {
                                topAnchor = 0.0
                                leftAnchor = 0.0
                                rightAnchor = 2 * (AddRemoveButtonsWidth + SpaceBetweenAddAndRemoveButtons)
                                bottomAnchor = 0.0
                            }
                        }

                        removeButton {
                            fixedWidth = AddRemoveButtonsWidth

                            enableWhen(isAIndexDirectorySelected)

                            action { removeSelectedIndexDirectory() }

                            anchorpaneConstraints {
                                topAnchor = 0.0
                                rightAnchor = AddRemoveButtonsWidth + SpaceBetweenAddAndRemoveButtons
                                bottomAnchor = 0.0
                            }
                        }

                        addButton {
                            fixedWidth = AddRemoveButtonsWidth

                            action { addIndexDirectory() }

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

                    tableview<IndexDirectoryViewModel>(indexDirectories) {
                        column<IndexDirectoryViewModel, String>(messages["configure.index.window.path.column.name"], IndexDirectoryViewModel::path) {
                            this.initiallyUseRemainingSpace(this@tableview)
                        }

                        column<IndexDirectoryViewModel, Number>(messages["configure.index.window.count.files.column.name"], IndexDirectoryViewModel::countElements) {
                            this.cellFormat { countFiles ->
                                this.text = if (countFiles.toInt() == IndexDirectoryViewModel.DeterminingCountElements) {
                                    messages["configure.index.window.determining.count.files.column.name"]
                                }
                                else {
                                    countFiles.toString()
                                }
                            }
                        }


                        minHeight = 60.0

                        selectionModel.selectionMode = SelectionMode.SINGLE

                        indexDirectories.firstOrNull()?.let { selectedIndexDirectory.value = it }

                        selectionModel.bindSelectedItemTo(selectedIndexDirectory) {
                            advancedConfigurationView.setCurrentIndexedDirectoryConfig(it?.item)
                            updateIndexConfigurationPreview()
                        }

                        selectionModel.bindIsAnItemSelectedTo(isAIndexDirectorySelected)

                        setOnKeyReleased { event -> indexDirectoryTableKeyPressed(event, selectionModel.selectedItem) }

                        vboxConstraints {
                            vGrow = Priority.ALWAYS
                        }
                    }
                }

                add(advancedConfigurationView)

                setDividerPosition(0, 0.4)
            }

            add(configuredIndexPreview)

            setDividerPosition(0, 0.2)
        }

        okCancelButtonBar(ButtonsHeight, ButtonsWidth, { close() }, { saveIndex() }, isRequiredDataEntered)
    }


    private fun mapToIndexDirectoryViewItem(indexDirectory: IndexedDirectoryConfig): IndexDirectoryViewModel {
        return IndexDirectoryViewModel(indexDirectory, filesToIndexFinder)
    }

    private fun indexDirectoryTableKeyPressed(event: KeyEvent, selectedIndexDirectory: IndexDirectoryViewModel?) {
        selectedIndexDirectory?.let {
            if (event.code == KeyCode.DELETE) {
                indexDirectories.remove(selectedIndexDirectory)

                checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
            }
        }
    }


    private fun addIndexDirectory() {
        val directoryChooser = DirectoryChooser()

        lastSelectedDirectory?.directory?.let { lastSelectedDirectory ->
            if (lastSelectedDirectory.exists()) {
                directoryChooser.initialDirectory = lastSelectedDirectory
            }
        }

        directoryChooser.showDialog(FX.primaryStage)?.let { selectedDirectory ->
            indexDirectoryAdded(selectedDirectory)
        }
    }

    private fun indexDirectoryAdded(addedDirectory: File) {
        val config = IndexedDirectoryConfig(addedDirectory)

        val indexDirectoryViewModel = mapToIndexDirectoryViewItem(config)
        indexDirectories.add(indexDirectoryViewModel)

        selectedIndexDirectory.value = indexDirectoryViewModel

        lastSelectedDirectory = config

        advancedConfigurationView.setCurrentIndexedDirectoryConfig(config)

        if (name.value.isEmpty()) {
            name.value = addedDirectory.name
        }

        checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
    }

    private fun removeSelectedIndexDirectory() {
        selectedIndexDirectory.value?.let {
            indexDirectories.remove(it)
        }

        checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
    }


    private fun checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview() {
        checkIfRequiredDataIsEntered()

        updateIndexConfigurationPreview()
    }

    private fun updateIndexConfigurationPreview() {
        stopFindingFilesToIndex?.set(true)

        selectedIndexDirectory.value?.path?.value?.let { selectedIndexDirectory ->
            runNonBlockingDispatchToUiThread("Could not search for files to index", logger,
                    { findIncludesAnExcludes(selectedIndexDirectory) }) { includesAndExcludes ->
                configuredIndexPreview.update(includesAndExcludes.first, includesAndExcludes.second)
            }
        }
    }

    private fun findIncludesAnExcludes(selectedIndexDirectory: String): Pair<List<Path>, List<ExcludedFile>>? {
        val stopTraversal = AtomicBoolean(false)
        stopFindingFilesToIndex = stopTraversal

        // TODO: display irregular include and exclude rules

        return filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(File(selectedIndexDirectory),
                advancedConfigurationView.includeRules, advancedConfigurationView.excludeRules, false,
                advancedConfigurationView.ignoreFilesLargerThanCountBytes,
                advancedConfigurationView.ignoreFilesSmallerThanCountBytes, stopTraversal))
    }


    private fun checkIfRequiredDataIsEntered() {
        isRequiredDataEntered.value = name.value.isNotBlank() && indexDirectories.isNotEmpty()
    }


    private fun saveIndex() {
        index.name = name.value
        index.directoriesToIndex = indexDirectories.map {
            IndexedDirectoryConfig(
                File(it.path.value),
                advancedConfigurationView.includeRules,
                advancedConfigurationView.excludeRules,
                advancedConfigurationView.ignoreFilesLargerThanCountBytes,
                advancedConfigurationView.ignoreFilesSmallerThanCountBytes
            )
        }

        configuringIndexDone(index)

        close()
    }

}