package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.geometry.Pos
import net.dankito.documents.search.model.Document
import tornadofx.*


class DocumentListCellFragment : ListCellFragment<Document>() {

	private val document = DocumentModel().bindTo(this)


	override val root = hbox {
		prefHeight = 60.0

		vbox {
			alignment = Pos.CENTER_LEFT

			label(document.filename) {
				vboxConstraints {
					marginBottom = 8.0
				}
			}

			label(document.url)
		}
	}

}