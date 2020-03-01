package net.dankito.documents.search.ui.windows.mainwindow.controls

import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import tornadofx.*


class StatusBar(countSearchResults: SimpleIntegerProperty) : View() {

	override val root = anchorpane {
		fixedHeight = 32.0

		hbox {
			alignment = Pos.CENTER_LEFT

			label(countSearchResults)

			label(messages["hits"]) {
				hboxConstraints {
					marginLeft = 4.0
				}
			}

			anchorpaneConstraints {
				topAnchor = 0.0
				rightAnchor = 6.0
				bottomAnchor = 0.0
			}
		}
	}

}