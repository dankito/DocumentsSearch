package net.dankito.documents.search.ui.windows.configureindex.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.mail.model.Email


open class IncludedMailAccountViewModel(mail: Email) : IncludedIndexPartItemViewModel<Email>(mail) {

    override val displayName = bind(Email::displayName) as SimpleStringProperty

    override val size = bind(Email::size) as SimpleLongProperty

}