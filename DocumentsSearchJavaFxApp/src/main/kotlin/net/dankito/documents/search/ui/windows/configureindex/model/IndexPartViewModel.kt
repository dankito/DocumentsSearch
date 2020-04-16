package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.search.model.IndexPartConfig
import tornadofx.*


abstract class IndexPartViewModel<T : IndexPartConfig>(item: T?) : ItemViewModel<T>(item) {

    companion object {
        const val DeterminingCountItems = -1
    }


    abstract val displayName: SimpleStringProperty

    open val countItemsInIndexPart = SimpleIntegerProperty(DeterminingCountItems)

}