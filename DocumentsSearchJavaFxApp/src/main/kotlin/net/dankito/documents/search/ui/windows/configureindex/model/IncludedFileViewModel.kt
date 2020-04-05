package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.utils.extensions.absolutePath
import net.dankito.utils.extensions.size
import tornadofx.*
import java.nio.file.Path


open class IncludedFileViewModel(file: Path) : ItemViewModel<Path>(file) {

    val path = bind(Path::absolutePath) as SimpleStringProperty

    val size = bind(Path::size) as SimpleLongProperty

}