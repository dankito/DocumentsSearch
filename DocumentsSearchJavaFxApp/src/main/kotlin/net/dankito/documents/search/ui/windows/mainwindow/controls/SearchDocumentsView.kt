package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.documents.search.SearchResult
import net.dankito.documents.search.model.Document
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.mainwindow.model.DocumentListCellFragment
import net.dankito.utils.javafx.ui.controls.searchtextfield
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import org.slf4j.LoggerFactory
import tornadofx.*
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

		listview(searchResult) {
			cellFragment(DocumentListCellFragment::class)

			vboxConstraints {
				vGrow = Priority.ALWAYS
				marginTop = 6.0
			}
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
			logger.warn("Could not search for '$searchTerm':\n$error")
			showError("Could not search for '$searchTerm':\n$error") // TODO: translate
		}

		if (result.successful) {
			searchResult.setAll(result.hits)
			countSearchResults.value = result.hits.size
		}
	}

	private fun showError(error: String) {
		// TODO: show error message
	}

}