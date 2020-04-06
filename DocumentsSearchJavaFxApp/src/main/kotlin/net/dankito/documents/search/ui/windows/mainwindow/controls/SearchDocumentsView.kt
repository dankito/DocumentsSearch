package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.documents.search.model.IndexConfig
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.mainwindow.model.DocumentMetadataListCellFragment
import net.dankito.utils.javafx.ui.controls.searchtextfield
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.runNonBlockingDispatchToUiThread
import org.slf4j.LoggerFactory
import tornadofx.*
import java.awt.Desktop
import java.io.File
import kotlin.concurrent.thread


class SearchDocumentsView(
		private val presenter: DocumentsSearchPresenter,
		private val countSearchResults: SimpleIntegerProperty
) : View() {

	companion object {
		val LabelsWidth = 60.0

		private val logger = LoggerFactory.getLogger(SearchDocumentsView::class.java)
	}


	private val enteredSearchTerm = SimpleStringProperty("")

	private val searchAllIndices = SimpleBooleanProperty(presenter.appSettings.searchAllIndices)

	private val searchResult = FXCollections.observableArrayList<DocumentMetadata>()

	private var searchResultsSplitPaneDividerPos = 0.5


	private val selectIndicesView = SelectIndexView(presenter) { selectedIndexChanged(it) }

	private var searchResultsSplitPane: SplitPane by singleAssign()

	private var selectedDocumentContentTextArea: TextArea by singleAssign()


	init {
		enteredSearchTerm.addListener { _, _, newValue -> searchDocuments(newValue) }

		presenter.subscribeToIndexUpdatedEvents().subscribe { indexUpdated(it) }
	}


	override fun onUndock() {
		presenter.close()

		super.onUndock()
	}


	override val root = vbox {
		paddingAll = 2.0

		add(selectIndicesView)

		hbox {
			fixedHeight = 40.0
			alignment = Pos.CENTER_LEFT

			label(messages["search"]) {
				prefWidth = LabelsWidth
			}

			searchtextfield(enteredSearchTerm) {
				prefHeight = 36.0

				hboxConstraints {
					hGrow = Priority.ALWAYS
					marginLeft = 6.0
					marginRight = 18.0
				}
			}

			checkbox(messages["main.window.search.all.indices"], searchAllIndices) {
				selectedProperty().addListener { _, _, _ -> searchAllIndicesToggled() }
			}
		}

		searchResultsSplitPane = splitpane {
			vboxConstraints {
				vGrow = Priority.ALWAYS
				marginTop = 6.0
			}

			listview(searchResult) {
				SplitPane.setResizableWithParent(this, true)

				cellFragment(DocumentMetadataListCellFragment::class)

				onDoubleClick { selectionModel.selectedItem?.let { handleDoubleClickOnDocument(it) } }

				selectionModel.selectedItemProperty().addListener { _, _, newValue -> selectedDocumentChanged(newValue) }
			}

			selectedDocumentContentTextArea = textarea {
				isWrapText = true

				useMaxHeight = true
				useMaxWidth = true
			}

			items.remove(selectedDocumentContentTextArea) // initially selectedDocumentContentTextArea is not visible (as no document is selected)
		}

		thread {
			searchDocuments("")
		}
	}


	private fun selectedIndexChanged(selectedIndex: IndexConfig?) {
		searchDocuments()

		saveAppSettings()
	}

	private fun searchAllIndicesToggled() {
		searchDocuments()

		saveAppSettings()
	}

	private fun saveAppSettings() {
		presenter.updateAndSaveAppSettings(selectIndicesView.currentSelectedIndex, searchAllIndices.value)
	}


	private fun indexUpdated(updatedIndex: IndexConfig) {
		if (selectIndicesView.currentSelectedIndex == updatedIndex || searchAllIndices.value) {
			searchDocuments()
		}
	}


	private fun searchDocuments() {
		searchDocuments(presenter.lastSearchTerm)
	}

	private fun searchDocuments(searchTerm: String) {
		runNonBlockingDispatchToUiThread( { presenter.searchDocuments(searchTerm, getSelectedIndices()) } ) { result ->
			showSearchResultOnUiThread(searchTerm, result)
		}
	}

	private fun showSearchResultOnUiThread(searchTerm: String, result: SearchResult) {
		result.error?.let { error ->
			showError("Could not search for '$searchTerm'", error) // TODO: translate
		}

		if (result.successful) {
			searchResult.setAll(result.hits)
			countSearchResults.value = result.hits.size
		}
	}


	private fun selectedDocumentChanged(selectedDocumentMetadata: DocumentMetadata?) {
		if (selectedDocumentMetadata == null) { // no document selected (e.g. after entering a search term)
			if (searchResultsSplitPane.items.contains(selectedDocumentContentTextArea)) { // there's no selected document -> we cannot show document content preview -> hide text area from user
				searchResultsSplitPaneDividerPos = searchResultsSplitPane.dividerPositions[0] // save divider position so that we can restore it when re-displaying text area
				searchResultsSplitPane.items.remove(selectedDocumentContentTextArea)
			}

			selectedDocumentContentTextArea.text = ""
		}
		else {
			getAndDisplayDocument(selectedDocumentMetadata)
		}
	}

	private fun getAndDisplayDocument(selectedDocumentMetadata: DocumentMetadata) {
		selectIndicesView.currentSelectedIndex?.let { index ->
			runNonBlockingDispatchToUiThread( { presenter.getDocument(index, selectedDocumentMetadata) } ) {document ->
				if (searchResultsSplitPane.items.contains(selectedDocumentContentTextArea) == false) {
					searchResultsSplitPane.items.add(selectedDocumentContentTextArea) // re-add text area
					searchResultsSplitPane.setDividerPositions(searchResultsSplitPaneDividerPos) // restore divider position
				}

				selectedDocumentContentTextArea.text = document.content
			}
		}
	}

	private fun handleDoubleClickOnDocument(selectedDocument: DocumentMetadata) {
		try {
			val file = File(selectedDocument.url)

			if (file.exists()) {
				openFileInOsDefaultApplication(file)
			}
		} catch (e: Exception) {
			logger.error("Could not open file '${selectedDocument.url}'", e)
			showError("Could not open file '${selectedDocument.url}'", e) // TODO: translate
		}
	}


	private fun getSelectedIndices(): List<IndexConfig> {
		return if (searchAllIndices.value) {
			presenter.indices
		}
		else {
			selectIndicesView.currentSelectedIndex?.let { currentSelectedIndex ->
				return listOf(currentSelectedIndex)
			}

			listOf()
		}
	}


	private fun openFileInOsDefaultApplication(file: File) {
		thread { // get off UI thread
			try {
				Desktop.getDesktop().open(file)
			} catch(ignored: Exception) { }
		}
	}


	private fun showError(error: String, exception: Exception?) {
		var errorMessage = error + (if (exception == null) "" else exception.localizedMessage)
		// TODO: show error message
	}

}