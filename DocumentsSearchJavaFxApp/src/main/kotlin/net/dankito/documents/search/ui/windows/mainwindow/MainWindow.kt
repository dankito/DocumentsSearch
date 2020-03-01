package net.dankito.documents.search.ui.windows.mainwindow

import javafx.beans.property.SimpleIntegerProperty
import net.dankito.documents.search.ui.presenter.DocumentsSearchPresenter
import net.dankito.documents.search.ui.windows.mainwindow.controls.SearchDocumentsView
import net.dankito.documents.search.ui.windows.mainwindow.controls.StatusBar
import tornadofx.*
import tornadofx.FX.Companion.messages


class MainWindow : View(messages["application.title"]) {

    private val presenter = DocumentsSearchPresenter()


    private val countSearchResults = SimpleIntegerProperty(0)



    override val root = borderpane {
        prefHeight = 620.0
        prefWidth = 1150.0

        center {
            add(SearchDocumentsView(presenter, countSearchResults))
        }

        bottom = StatusBar(countSearchResults).root
    }

}