package net.dankito.documents.search.ui.windows.mainwindow.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.search.model.DocumentMetadata
import tornadofx.*


class DocumentMetadataViewModel : ItemViewModel<DocumentMetadata>() {

	init {
	    itemProperty.addListener { _, _, _ -> updateMetadata() }
	}


	val filename = bind(DocumentMetadata::filename)

	val showMetadata = SimpleBooleanProperty(false)

	val metadata = SimpleStringProperty("")

	val url = bind(DocumentMetadata::url)


	private fun updateMetadata() {
		metadata.value = getMetadataString()

		showMetadata.value = metadata.value.isNullOrBlank() == false
	}

	private fun getMetadataString(): String {
		item?.let { item ->
			var metadata = if (item.author != null && item.title != null) {
				"${item.author} - ${item.title} "
			}
			else if (item.title != null) {
				"${item.title} "
			}
			else ""

			if (item.series != null) {
				metadata += "(${item.series})"
			}

			return metadata
		}

		return ""
	}

}