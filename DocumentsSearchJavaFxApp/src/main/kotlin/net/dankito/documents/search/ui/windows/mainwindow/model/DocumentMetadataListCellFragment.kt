package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.geometry.Pos
import net.dankito.documents.search.model.DocumentMetadata
import net.dankito.utils.javafx.ui.extensions.ensureOnlyUsesSpaceIfVisible
import tornadofx.*


class DocumentMetadataListCellFragment : ListCellFragment<DocumentMetadata>() {

	private val document = DocumentMetadataViewModel().bindTo(this)


	override val root = hbox {
		prefHeight = 60.0

		vbox {
			alignment = Pos.CENTER_LEFT

			hbox {
				alignment = Pos.CENTER_LEFT

				label(document.metadata) {
					visibleWhen(document.showMetadata)

					ensureOnlyUsesSpaceIfVisible()
				}

				label(":") {
					visibleWhen(document.showMetadata)

					ensureOnlyUsesSpaceIfVisible()

					hboxConstraints {
						marginLeft = 0.0
						marginRight = 8.0
					}
				}

				label(document.filename)

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