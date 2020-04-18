package net.dankito.documents.search.ui.windows.configureindex

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.control.OverrunStyle
import javafx.scene.control.SelectionMode
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.dankito.documents.filesystem.ExcludedFile
import net.dankito.documents.filesystem.FilesToIndexConfig
import net.dankito.documents.filesystem.FilesToIndexFinder
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.model.IndexPartConfig
import net.dankito.documents.search.model.IndexedDirectoryConfig
import net.dankito.documents.search.model.IndexedMailAccountConfig
import net.dankito.documents.search.ui.windows.configureindex.controls.ConfiguredIndexPreview
import net.dankito.documents.search.ui.windows.configureindex.controls.IndexedDirectoryConfigurationView
import net.dankito.documents.search.ui.windows.configureindex.controls.IndexedMailAccountConfigurationView
import net.dankito.documents.search.ui.windows.configureindex.model.*
import net.dankito.mail.EmailFetcher
import net.dankito.mail.model.FetchEmailOptions
import net.dankito.utils.info.Ports
import net.dankito.utils.javafx.ui.controls.AddButtonWithDropDownMenu
import net.dankito.utils.javafx.ui.controls.addButtonWithDropDownMenu
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

        private val AddButtonsWidthDropDownWidth = AddRemoveButtonsWidth + AddButtonWithDropDownMenu.DefaultDropDownButtonWidth

        private val SpaceBetweenAddAndRemoveButtons = 6.0

        private val ButtonsHeight = 35.0

        private val ButtonsWidth = 150.0

        private val logger = LoggerFactory.getLogger(ConfigureIndexWindow::class.java)
    }


    private val filesToIndexFinder = FilesToIndexFinder()

    protected var stopFindingFilesToIndex: AtomicBoolean? = null

    private val mailFetcher = EmailFetcher()


    private val name = SimpleStringProperty(index.name)

    private val indexParts = FXCollections.observableArrayList<IndexPartViewModel<*>>(index.indexParts.map { mapToIndexPartViewModel(it) })

    private val selectedIndexPart = SimpleObjectProperty<IndexPartViewModel<*>>(null)

    private val isAIndexPartSelected = SimpleBooleanProperty(false)

    private var lastSelectedDirectory: IndexedDirectoryConfig? = index.indexParts.mapNotNull { it as? IndexedDirectoryConfig }.firstOrNull()

    private val isRequiredDataEntered = SimpleBooleanProperty(false)


    private val directoryConfigurationView = IndexedDirectoryConfigurationView { checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview() }

    private val showDirectoryConfigurationView = SimpleBooleanProperty(false)

    private val mailAccountConfigurationView = IndexedMailAccountConfigurationView(mailFetcher, { checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview() },
            { newValue, oldValue ->  mailAccountNameChanged(newValue, oldValue) })

    private val showMailAccountConfigurationView = SimpleBooleanProperty(false)

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

                minWidth = 350.0

                // TODO: extract to own View
                vbox {
                    prefHeight = AddRemoveButtonsHeight + 120.0 // 120.0 = TableView pref height

                    anchorpane {
                        fixedHeight = AddRemoveButtonsHeight
                        alignment = Pos.CENTER_LEFT

                        label(messages["configure.index.window.index.parts.label"]) {
                            useMaxHeight = true

                            this.textOverrun = OverrunStyle.ELLIPSIS

                            anchorpaneConstraints {
                                topAnchor = 0.0
                                leftAnchor = 0.0
                                rightAnchor = SpaceBetweenAddAndRemoveButtons + AddRemoveButtonsWidth + SpaceBetweenAddAndRemoveButtons + AddButtonsWidthDropDownWidth
                                bottomAnchor = 0.0
                            }
                        }

                        removeButton {
                            fixedWidth = AddRemoveButtonsWidth

                            enableWhen(isAIndexPartSelected)

                            action { removeSelectedIndexPart() }

                            anchorpaneConstraints {
                                topAnchor = 0.0
                                rightAnchor = AddButtonsWidthDropDownWidth + SpaceBetweenAddAndRemoveButtons
                                bottomAnchor = 0.0
                            }
                        }

                        addButtonWithDropDownMenu {
                            fixedWidth = AddButtonsWidthDropDownWidth

                            action { addIndexDirectory() }

                            items.addAll(createAddIndexHandlerOptions())

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

                    tableview<IndexPartViewModel<*>>(indexParts) {
                        column<IndexPartViewModel<*>, String>(messages["configure.index.window.index.part.display.name.column.name"], IndexPartViewModel<*>::displayName) {
                            this.initiallyUseRemainingSpace(this@tableview)
                        }

                        column<IndexPartViewModel<*>, Number>(messages["configure.index.window.count.items.in.index.part.column.name"], IndexPartViewModel<*>::countItemsInIndexPart) {
                            this.cellFormat { countFiles ->
                                this.text = if (countFiles.toInt() == IndexPartViewModel.DeterminingCountItems) {
                                    messages["configure.index.window.determining.count.items.in.index.part.column.name"]
                                }
                                else {
                                    countFiles.toString()
                                }
                            }
                        }


                        minHeight = 60.0

                        selectionModel.selectionMode = SelectionMode.SINGLE

                        indexParts.firstOrNull()?.let { selectedIndexPart.value = it }

                        selectionModel.bindSelectedItemTo(selectedIndexPart) { selectedIndexPartChanged(it) }

                        selectionModel.bindIsAnItemSelectedTo(isAIndexPartSelected)

                        setOnKeyReleased { event -> indexPartTableKeyPressed(event, selectionModel.selectedItem) }

                        vboxConstraints {
                            vGrow = Priority.ALWAYS
                        }
                    }
                }

                vbox {
                    add(directoryConfigurationView.apply {
                        this.root.visibleWhen(showDirectoryConfigurationView)

                        this.root.ensureOnlyUsesSpaceIfVisible()
                    })

                    add(mailAccountConfigurationView.apply {
                        this.root.visibleWhen(showMailAccountConfigurationView)

                        this.root.ensureOnlyUsesSpaceIfVisible()
                    })
                }

                setDividerPosition(0, 0.4)

                selectedIndexPartChanged(selectedIndexPart.value) // to set initial state
            }

            add(configuredIndexPreview)

            setDividerPosition(0, 0.2)
        }

        okCancelButtonBar(ButtonsHeight, ButtonsWidth, { close() }, { saveIndex() }, isRequiredDataEntered)
    }

    private fun createAddIndexHandlerOptions(): List<MenuItem> {
        return listOf(
                MenuItem(messages["configure.index.window.add.mail.account"]).apply {
                    action { addIndexedMailAccount() }
                }
        )
    }


    private fun selectedIndexPartChanged(selectedIndexPart: IndexPartViewModel<*>?) {
        showDirectoryConfigurationView.value = false
        showMailAccountConfigurationView.value = false

        if (selectedIndexPart is IndexedDirectoryViewModel) {
            directoryConfigurationView.setCurrentIndexedDirectoryConfig(selectedIndexPart.item)
            showDirectoryConfigurationView.value = true
        }
        else if (selectedIndexPart is IndexedMailAccountViewModel) {
            mailAccountConfigurationView.setCurrentMailAccount(selectedIndexPart.item)
            showMailAccountConfigurationView.value = true
        }

        updateIndexConfigurationPreview()
    }


    private fun mapToIndexPartViewModel(indexPartConfig: IndexPartConfig): IndexPartViewModel<*> {
        if (indexPartConfig is IndexedDirectoryConfig) {
            return IndexedDirectoryViewModel(indexPartConfig, filesToIndexFinder)
        }
        else if (indexPartConfig is IndexedMailAccountConfig) {
            return IndexedMailAccountViewModel(indexPartConfig)
        }

        return IndexedDirectoryViewModel(indexPartConfig as IndexedDirectoryConfig, filesToIndexFinder) // to make compiler happy
    }

    private fun indexPartTableKeyPressed(event: KeyEvent, selectedIndexDPart: IndexPartViewModel<*>?) {
        selectedIndexDPart?.let {
            if (event.code == KeyCode.DELETE) {
                indexParts.remove(selectedIndexDPart)

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

        val indexDirectoryViewModel = mapToIndexPartViewModel(config)
        indexParts.add(indexDirectoryViewModel)

        selectedIndexPart.value = indexDirectoryViewModel

        lastSelectedDirectory = config

        directoryConfigurationView.setCurrentIndexedDirectoryConfig(config)

        if (name.value.isEmpty()) {
            name.value = addedDirectory.name
        }

        checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
    }

    private fun addIndexedMailAccount() {
        val indexedMailAccountViewModel = IndexedMailAccountViewModel(IndexedMailAccountConfig("", "", "", "", 993))
        indexParts.add(indexedMailAccountViewModel)

        selectedIndexPart.value = indexedMailAccountViewModel

        checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
    }

    private fun removeSelectedIndexPart() {
        selectedIndexPart.value?.let {
            indexParts.remove(it)
        }

        checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview()
    }


    private fun mailAccountNameChanged(accountName: String, oldValue: String) {
        if (name.value.isEmpty() || name.value == oldValue) {
            name.value = accountName
        }

        (selectedIndexPart.value as? IndexedMailAccountViewModel)?.let {
            it.displayName.value = accountName
        }
    }


    private fun checkIfRequiredDataIsEnteredAndUpdateIndexConfigurationPreview() {
        checkIfRequiredDataIsEntered()

        updateIndexConfigurationPreview()
    }

    private fun updateIndexConfigurationPreview() {
        stopFindingFilesToIndex?.set(true)

        selectedIndexPart.value?.let { selectedIndexPartViewModel ->
            if (selectedIndexPartViewModel is IndexedDirectoryViewModel) {
                updateDirectoryConfigurationPreview(selectedIndexPartViewModel)
            }
            else if (selectedIndexPartViewModel is IndexedMailAccountViewModel) {
                updatedMailAccountConfigurationPreview(selectedIndexPartViewModel)
            }
        }
    }

    private fun updateDirectoryConfigurationPreview(selectedIndexPartViewModel: IndexedDirectoryViewModel) {
        runNonBlockingDispatchToUiThread("Could not search for files to index", logger,
                { findIncludesAnExcludes(selectedIndexPartViewModel.displayName.value) }) { includesAndExcludes ->
            if (selectedIndexPart.value == selectedIndexPartViewModel) { // check if it's still the selected index part to not overwrite selected index part's preview
                configuredIndexPreview.update(includesAndExcludes.first.map { IncludedFileViewModel(it) }, includesAndExcludes.second.map { ExcludedFileViewModel(it) })
            }
        }
    }

    private fun findIncludesAnExcludes(selectedIndexDirectory: String): Pair<List<Path>, List<ExcludedFile>>? {
        val stopTraversal = AtomicBoolean(false)
        stopFindingFilesToIndex = stopTraversal

        // TODO: display irregular include and exclude rules

        return filesToIndexFinder.findFilesToIndex(FilesToIndexConfig(File(selectedIndexDirectory),
                directoryConfigurationView.includeRules, directoryConfigurationView.excludeRules, false,
                directoryConfigurationView.ignoreFilesLargerThanCountBytes,
                directoryConfigurationView.ignoreFilesSmallerThanCountBytes, stopTraversal))
    }

    private fun updatedMailAccountConfigurationPreview(selectedIndexPartViewModel: IndexedMailAccountViewModel) {
        if (isRequiredDataForMailAccountEntered(selectedIndexPartViewModel)) {
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            mailFetcher.fetchMails(FetchEmailOptions(mailAccountConfigurationView.mapToMailAccount(), chunkSize = 25)) { fetchEmailsResult ->
                GlobalScope.launch(Dispatchers.JavaFx) {
                    if (selectedIndexPart.value == selectedIndexPartViewModel) { // check if it's still the selected index part to not overwrite selected index part's preview
                        configuredIndexPreview.update(ArrayList(fetchEmailsResult.allRetrievedMails).map { IncludedMailAccountViewModel(it) }, listOf())
                    }

                    if (fetchEmailsResult.completed) {
                        selectedIndexPartViewModel.countItemsInIndexPart.value = fetchEmailsResult.allRetrievedMails.size
                    }
                }
            }
        }
    }


    private fun checkIfRequiredDataIsEntered() {
        isRequiredDataEntered.value = name.value.isNotBlank() && indexParts.isNotEmpty() && isForAllIndexPartsRequiredDataEntered()
    }

    private fun isForAllIndexPartsRequiredDataEntered(): Boolean {
        indexParts.forEach { indexPart ->
            if (indexPart is IndexedMailAccountViewModel) {
                if (isRequiredDataForMailAccountEntered(indexPart)) {
                    return false
                }
            }
        }

        return true
    }

    private fun isRequiredDataForMailAccountEntered(indexPart: IndexedMailAccountViewModel): Boolean {
        if (isRequiredDataForMailAccountEntered(indexPart.item)) {
            return false
        }

        return true
    }

    private fun isRequiredDataForMailAccountEntered(config: IndexedMailAccountConfig): Boolean {
        if (config.username.isBlank() || config.password.isBlank() ||
                config.imapServerAddress.isBlank() || Ports.isValidPortNumber(config.imapServerPort) == false) {

            return false
        }

        return true
    }


    private fun saveIndex() {
        index.name = name.value
        index.indexParts = indexParts.map { indexPart ->
            if (indexPart is IndexedMailAccountViewModel) {
                indexPart.item
            }
            else {
                IndexedDirectoryConfig(
                        File(indexPart.displayName.value),
                        directoryConfigurationView.includeRules,
                        directoryConfigurationView.excludeRules,
                        directoryConfigurationView.ignoreFilesLargerThanCountBytes,
                        directoryConfigurationView.ignoreFilesSmallerThanCountBytes
                )
            }
        }

        configuringIndexDone(index)

        close()
    }

}