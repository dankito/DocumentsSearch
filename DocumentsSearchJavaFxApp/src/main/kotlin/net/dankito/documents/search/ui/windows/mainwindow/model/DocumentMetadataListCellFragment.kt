package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.geometry.Pos
import net.dankito.documents.search.model.DocumentMetadata
import tornadofx.*


class DocumentMetadataListCellFragment : ListCellFragment<DocumentMetadata>() {

	private val document = DocumentMetadataViewModel().bindTo(this)


	override val root = hbox {
		prefHeight = 60.0

		vbox {
			alignment = Pos.CENTER_LEFT

			label(document.filename) {
				vboxConstraints {
					marginBottom = 8.0
				}
			}

			label(document.url) {
				textProperty().addListener { _, _, newValue ->
					tooltip(newValue)
				}
			}
		}
	}

}