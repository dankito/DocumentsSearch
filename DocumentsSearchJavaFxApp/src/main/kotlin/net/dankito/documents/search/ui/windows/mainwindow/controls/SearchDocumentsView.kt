package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.mainwindow.model.DocumentListCellFragment
import net.dankito.utils.javafx.ui.controls.searchtextfield
import net.dankito.utils.javafx.ui.extensions.fixedHeight
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
		private val logger = LoggerFactory.getLogger(SearchDocumentsView::class.java)
	}


	private val enteredSearchTerm = SimpleStringProperty("")

	private val searchResult = FXCollections.observableArrayList<Document>()

	private var searchResultsSplitPaneDividerPos = 0.5


	private var searchResultsSplitPane: SplitPane by singleAssign()

	private var selectedDocumentContentTextArea: TextArea by singleAssign()


	init {
		enteredSearchTerm.addListener { _, _, newValue -> searchDocuments(newValue) }
	}


	override val root = vbox {
		paddingAll = 2.0

		hbox {
			fixedHeight = 32.0

			alignment = Pos.CENTER_LEFT

			label(messages["search"])

			searchtextfield(enteredSearchTerm) {
				hboxConstraints {
					hGrow = Priority.ALWAYS
					marginLeft = 6.0
				}
			}
		}

		searchResultsSplitPane = splitpane {
			vboxConstraints {
				vGrow = Priority.ALWAYS
				marginTop = 6.0
			}

			listview(searchResult) {
				SplitPane.setResizableWithParent(this, true)

				cellFragment(DocumentListCellFragment::class)

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


	private fun searchDocuments(searchTerm: String) {
		presenter.searchDocumentsAsync(searchTerm) { result ->
			runLater {
				showSearchResultOnUiThread(searchTerm, result)
			}
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


	private fun selectedDocumentChanged(selectedDocument: Document?) {
		if (selectedDocument == null) { // no document selected (e.g. after entering a search term)
			if (searchResultsSplitPane.items.contains(selectedDocumentContentTextArea)) { // there's no selected document -> we cannot show document content preview -> hide text area from user
				searchResultsSplitPaneDividerPos = searchResultsSplitPane.dividerPositions[0] // save divider position so that we can restore it when re-displaying text area
				searchResultsSplitPane.items.remove(selectedDocumentContentTextArea)
			}

			selectedDocumentContentTextArea.text = ""
		}
		else {
			if (searchResultsSplitPane.items.contains(selectedDocumentContentTextArea) == false) {
				searchResultsSplitPane.items.add(selectedDocumentContentTextArea) // re-add text area
				searchResultsSplitPane.setDividerPositions(searchResultsSplitPaneDividerPos) // restore divider position
			}

			selectedDocumentContentTextArea.text = selectedDocument?.content
		}
	}

	private fun handleDoubleClickOnDocument(selectedDocument: Document) {
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