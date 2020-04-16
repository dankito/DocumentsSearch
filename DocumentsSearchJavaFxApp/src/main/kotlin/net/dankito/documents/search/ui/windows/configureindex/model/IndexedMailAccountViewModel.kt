package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.search.model.IndexedMailAccountConfig


class IndexedMailAccountViewModel(config: IndexedMailAccountConfig?)
    : IndexPartViewModel<IndexedMailAccountConfig>(config) {


    override val displayName = SimpleStringProperty(item.mailAddress)


    init {
        itemProperty.addListener { _, _, newValue ->
            displayName.value = newValue.mailAddress
            updateCountItemsInIndexPart()
        }

        countItemsInIndexPart.value = 0
    }


    private fun updateCountItemsInIndexPart() {
        countItemsInIndexPart.value = DeterminingCountItems
    }

}