package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*


abstract class IncludedIndexPartItemViewModel<T>(item: T) : ItemViewModel<T>(item) {

    abstract val displayName : SimpleStringProperty

    abstract val size: SimpleLongProperty

}