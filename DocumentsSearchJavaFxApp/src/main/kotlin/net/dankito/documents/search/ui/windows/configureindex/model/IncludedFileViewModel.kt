package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.utils.extensions.absolutePath
import net.dankito.utils.extensions.size
import java.nio.file.Path


open class IncludedFileViewModel(file: Path) : IncludedIndexPartItemViewModel<Path>(file) {

    override val displayName = bind(Path::absolutePath) as SimpleStringProperty

    override val size = bind(Path::size) as SimpleLongProperty

}