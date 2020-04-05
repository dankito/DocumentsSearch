package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.documents.filesystem.ExcludeReason
import net.dankito.documents.filesystem.ExcludedFile
import net.dankito.documents.search.ui.absolutePath
import net.dankito.documents.search.ui.size
import tornadofx.*


class ExcludedFileViewModel(file: ExcludedFile) : ItemViewModel<ExcludedFile>(file)  {

    val path = bind(ExcludedFile::absolutePath) as SimpleStringProperty

    val size = bind(ExcludedFile::size) as SimpleLongProperty

    val reason = bind(ExcludedFile::reason) as SimpleObjectProperty<ExcludeReason>

}